package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Pokémon abelha moram nas colmeias e ninhos no lugar das abelhas, e funcionam como elas:
 * - cada abelha que sairia da colmeia vira um Pokémon morador dela;
 * - de dia visitam flores, polinizam plantações e levam mel para a colmeia;
 * - entram na colmeia para descansar, à noite e na chuva (a entidade fica guardada nos dados da colmeia);
 * - quebrar a colmeia (ou explodir) solta quem estiver dentro;
 * - Pokémon abelha selvagens sem casa adotam colmeias com vaga por perto, e perdem a casa se ela sumir.
 *
 * Cada colmeia é de uma colônia só, como os cercados das vilas: Combee (com no máximo uma rainha
 * Vespiquen) ou Cutiefly (com Ribombee).
 *
 * "Ser morador" é a marca HIVE_KEY no Pokémon mais a lista de moradores salva na colmeia.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class HivePokemon {
    /** Espécie → colônia a que pertence. */
    private static final Map<String, String> COLONY_OF = Map.of(
            "combee", "combee", "vespiquen", "combee",
            "cutiefly", "cutiefly", "ribombee", "cutiefly");
    /** Chance de uma colmeia nova virar colônia de Combee (o resto vira de Cutiefly). */
    private static final float COMBEE_COLONY_CHANCE = 0.65f;
    /** Chance de cada Pokémon nascido na colônia de Combee ser a rainha (se ainda não houver uma). */
    private static final float QUEEN_CHANCE = 0.15f;
    /** Chance de cada Pokémon nascido na colônia de Cutiefly já vir evoluído. */
    private static final float RIBOMBEE_CHANCE = 0.25f;

    private static final int MAX_PER_HIVE = 3;
    private static final int FLOWER_RADIUS = 10;
    private static final int CLAIM_RADIUS = 16;
    private static final int POLLINATE_TICKS = 100;
    /** Desiste de uma flor que não alcança nesse tempo. */
    private static final int GIVE_UP_TICKS = 1200;
    /** Tentando voltar para casa por mais que isso (30 s), entra mesmo assim. */
    private static final int RETURN_TIMEOUT_TICKS = 600;
    /** Morador que não aparece por esse tempo (com a colmeia carregada) deixa de contar: foi capturado ou derrotado. */
    private static final long MEMBER_TIMEOUT = 6000;

    private static final String HIVE_KEY = CobblemonEcosystems.MOD_ID + ":hive";
    private static final String NECTAR_KEY = CobblemonEcosystems.MOD_ID + ":nectar";
    /** Na colmeia: colônia, moradores (UUID → {seen, species}) e quem está dentro (entidade salva + hora de sair). */
    private static final String COLONY_KEY = CobblemonEcosystems.MOD_ID + ":colony";
    private static final String MEMBERS_KEY = CobblemonEcosystems.MOD_ID + ":members";
    private static final String INSIDE_KEY = CobblemonEcosystems.MOD_ID + ":inside";

    private record PendingSpawn(ServerLevel level, Vec3 pos, BlockPos hive) {}
    private record PendingRelease(ServerLevel level, BlockPos hive, ListTag inside) {}
    private static final List<PendingSpawn> pendingSpawns = new ArrayList<>();
    private static final List<PendingRelease> pendingReleases = new ArrayList<>();

    /** Estado de cada morador fora da colmeia (não salvo: recomeça do zero ao recarregar, sem problema). */
    private static final class Worker {
        BlockPos flower;
        long targetSince;
        long arrivedAt = -1;
        /** Desde quando está tentando voltar para casa (-1 = não está voltando). */
        long returningSince = -1;
    }
    private static final Map<PokemonEntity, Worker> workers = new HashMap<>();
    /** Pokémon abelha selvagens sem colmeia, procurando uma. */
    private static final Set<PokemonEntity> homeless = new HashSet<>();
    /** Colmeias com alguém dentro, para saberem a hora de soltar. */
    private static final Set<GlobalPos> occupiedHives = ConcurrentHashMap.newKeySet();
    private static int tick;

    // ----- Entradas -----

    /** Chamado por CobblemonEcosystems quando uma abelha bloqueada estava saindo de uma colmeia. */
    static void replaceIfFromHive(Entity entity, ServerLevel level) {
        if (entity instanceof Bee bee && bee.hasHive()) pendingSpawns.add(new PendingSpawn(level, bee.position(), bee.getHivePos()));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof PokemonEntity pokemon)) return;
        if (pokemon.getPersistentData().contains(HIVE_KEY)) {
            workers.putIfAbsent(pokemon, new Worker());
        } else if (COLONY_OF.containsKey(PokemonAi.species(pokemon)) && PokemonAi.isWild(pokemon)) {
            homeless.add(pokemon);
        }
    }

    /** Ao carregar um chunk, lembra das colmeias que têm Pokémon dentro. */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof BeehiveBlockEntity && !inside(blockEntity).isEmpty()) {
                occupiedHives.add(GlobalPos.of(level.dimension(), blockEntity.getBlockPos()));
            }
        }
    }

    /** Quebrar a colmeia solta quem está dentro, como as abelhas. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) releaseAllOnBreak(level, event.getPos());
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            for (BlockPos pos : event.getAffectedBlocks()) releaseAllOnBreak(level, pos);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (PendingSpawn p : List.copyOf(pendingSpawns)) spawnFromBees(p);
        pendingSpawns.clear();
        // Depois que o bloco já saiu do mundo.
        for (PendingRelease p : List.copyOf(pendingReleases)) {
            for (Tag entry : p.inside()) release(p.level(), p.hive(), ((CompoundTag) entry).getCompound("entity"));
        }
        pendingReleases.clear();

        if (++tick % 20 != 0) return;
        workers.entrySet().removeIf(entry -> entry.getKey().isRemoved());
        for (Map.Entry<PokemonEntity, Worker> entry : List.copyOf(workers.entrySet())) work(entry.getKey(), entry.getValue());
        releaseRested(event.getServer());
        if (tick % 600 == 0 && Config.HIVE_POKEMON.get()) findHomes();
    }

    // ----- Nascer das abelhas -----

    private static void spawnFromBees(PendingSpawn p) {
        // Abelha cancelada continua guardada na colmeia, e o jogo tenta soltá-la (com som) a cada tick.
        // Então, na primeira tentativa, cada abelha guardada vira um Pokémon e a colmeia é esvaziada.
        if (!(p.level().getBlockEntity(p.hive()) instanceof BeehiveBlockEntity hive)) return;
        int bees = hive.getOccupantCount();
        if (bees == 0) return;
        emptyVanillaBees(p.level(), hive);
        // Com o módulo desligado, a colmeia só é esvaziada (senão ficaria tentando soltar a abelha para sempre).
        if (!Config.HIVE_POKEMON.get()) return;

        long now = p.level().getGameTime();
        RandomSource random = p.level().random;
        String colony = hive.getPersistentData().getString(COLONY_KEY);
        if (colony.isEmpty()) colony = random.nextFloat() < COMBEE_COLONY_CHANCE ? "combee" : "cutiefly";

        int toSpawn = Math.min(bees, MAX_PER_HIVE - memberCount(hive, now));
        for (int i = 0; i < toSpawn; i++) {
            String species = colony.equals("combee")
                    ? (!hasMember(hive, "vespiquen") && random.nextFloat() < QUEEN_CHANCE ? "vespiquen" : "combee")
                    : (random.nextFloat() < RIBOMBEE_CHANCE ? "ribombee" : "cutiefly");
            PokemonEntity pokemon = createWorker(p.level(), p.pos(), species);
            if (p.level().addFreshEntity(pokemon)) join(pokemon, hive, now);
        }
    }

    /** Recarrega a colmeia com os próprios dados, só que sem a lista de abelhas do vanilla. */
    private static void emptyVanillaBees(ServerLevel level, BeehiveBlockEntity hive) {
        CompoundTag data = hive.saveCustomOnly(level.registryAccess());
        data.remove("bees");
        hive.loadCustomOnly(data, level.registryAccess());
        hive.setChanged();
    }

    private static PokemonEntity createWorker(ServerLevel level, Vec3 pos, String species) {
        RandomSource random = level.random;
        boolean evolved = species.equals("ribombee") || species.equals("vespiquen");
        int pokemonLevel = evolved ? 20 + random.nextInt(11) : 5 + random.nextInt(11);
        // Vespiquen só existe fêmea.
        String gender = species.equals("vespiquen") ? " gender=female" : "";
        PokemonEntity pokemon = PokemonProperties.Companion.parse(species + " level=" + pokemonLevel + gender).createEntity(level);
        pokemon.moveTo(pos.x + random.nextDouble() - 0.5, pos.y, pos.z + random.nextDouble() - 0.5, random.nextFloat() * 360f, 0f);
        return pokemon;
    }

    // ----- Ganhar e perder a colmeia -----

    /** Torna o Pokémon morador da colmeia (e define a colônia, se ela ainda não tiver uma). */
    private static void join(PokemonEntity pokemon, BlockEntity hive, long now) {
        String species = PokemonAi.species(pokemon);
        hive.getPersistentData().putString(COLONY_KEY, COLONY_OF.get(species));
        pokemon.getPersistentData().putLong(HIVE_KEY, hive.getBlockPos().asLong());
        KeptPokemon.keep(pokemon);
        touch(hive, pokemon, now);
        homeless.remove(pokemon);
        workers.putIfAbsent(pokemon, new Worker());
    }

    /** Tira a marca de morador: volta a ser um Pokémon abelha selvagem que pode adotar outra colmeia. */
    private static void leave(PokemonEntity pokemon) {
        pokemon.getPersistentData().remove(HIVE_KEY);
        pokemon.getPersistentData().remove(NECTAR_KEY);
        workers.remove(pokemon);
        if (PokemonAi.isWild(pokemon)) homeless.add(pokemon);
    }

    /** Pode morar aqui? Mesma colônia (ou colmeia vazia), com vaga, e só uma rainha. */
    private static boolean accepts(BlockEntity hive, String species, long now) {
        if (memberCount(hive, now) >= MAX_PER_HIVE) return false;
        String colony = hive.getPersistentData().getString(COLONY_KEY);
        if (!colony.isEmpty() && !colony.equals(COLONY_OF.get(species))) return false;
        return !species.equals("vespiquen") || !hasMember(hive, "vespiquen");
    }

    /** Pokémon abelha sem casa procuram uma colmeia com vaga por perto. */
    private static void findHomes() {
        homeless.removeIf(pokemon -> pokemon.isRemoved() || !PokemonAi.isWild(pokemon) || pokemon.getPersistentData().contains(HIVE_KEY));
        for (PokemonEntity pokemon : List.copyOf(homeless)) {
            if (pokemon.isBusy()) continue;
            ServerLevel level = (ServerLevel) pokemon.level();
            long now = level.getGameTime();
            String species = PokemonAi.species(pokemon);
            level.getPoiManager()
                    .findAllClosestFirstWithType(type -> type.is(PoiTypeTags.BEE_HOME), pos -> true,
                            pokemon.blockPosition(), CLAIM_RADIUS, PoiManager.Occupancy.ANY)
                    .map(pair -> level.getBlockEntity(pair.getSecond()))
                    .filter(hive -> hive instanceof BeehiveBlockEntity && accepts(hive, species, now))
                    .findFirst()
                    .ifPresent(hive -> join(pokemon, hive, now));
        }
    }

    private static CompoundTag members(BlockEntity hive) {
        CompoundTag data = hive.getPersistentData();
        if (!data.contains(MEMBERS_KEY)) data.put(MEMBERS_KEY, new CompoundTag());
        return data.getCompound(MEMBERS_KEY);
    }

    /** Marca o morador como visto agora (a espécie também, que muda se ele evoluir). */
    private static void touch(BlockEntity hive, PokemonEntity pokemon, long now) {
        CompoundTag member = new CompoundTag();
        member.putLong("seen", now);
        member.putString("species", PokemonAi.species(pokemon));
        members(hive).put(pokemon.getStringUUID(), member);
        hive.setChanged();
    }

    private static boolean hasMember(BlockEntity hive, String species) {
        CompoundTag members = members(hive);
        for (String id : members.getAllKeys()) {
            if (members.getCompound(id).getString("species").equals(species)) return true;
        }
        return false;
    }

    /**
     * Conta moradores, esquecendo os que sumiram (capturados, derrotados) há muito tempo. Quem está dentro
     * sempre conta. Colmeia sem ninguém volta a aceitar qualquer colônia.
     */
    private static int memberCount(BlockEntity hive, long now) {
        CompoundTag members = members(hive);
        Set<String> insideIds = new HashSet<>();
        for (Tag entry : inside(hive)) insideIds.add(((CompoundTag) entry).getCompound("entity").getUUID("UUID").toString());
        for (String id : List.copyOf(members.getAllKeys())) {
            if (!insideIds.contains(id) && now - members.getCompound(id).getLong("seen") > MEMBER_TIMEOUT) members.remove(id);
        }
        if (members.isEmpty()) hive.getPersistentData().remove(COLONY_KEY);
        return members.size();
    }

    // ----- Rotina do morador -----

    private static void work(PokemonEntity pokemon, Worker worker) {
        if (pokemon.isBusy()) return;
        if (!PokemonAi.isWild(pokemon)) {
            leave(pokemon);
            return;
        }
        ServerLevel level = (ServerLevel) pokemon.level();
        BlockPos hivePos = BlockPos.of(pokemon.getPersistentData().getLong(HIVE_KEY));
        if (!level.isLoaded(hivePos)) return;
        BlockState hiveState = level.getBlockState(hivePos);
        BlockEntity hive = level.getBlockEntity(hivePos);
        if (!hiveState.is(BlockTags.BEEHIVES) || !(hive instanceof BeehiveBlockEntity)) {
            leave(pokemon); // colmeia quebrada ou trocada
            return;
        }
        long now = level.getGameTime();
        touch(hive, pokemon, now);
        // O destino é a frente da colmeia (por onde as abelhas entram). Andar até o próprio bloco fazia o
        // caminho terminar em cima das folhas, longe demais para entrar.
        BlockPos entrance = entrance(level, hivePos, hiveState);
        boolean atHive = pokemon.blockPosition().closerThan(hivePos, 2.5) || pokemon.blockPosition().closerThan(entrance, 1.8);
        boolean nightOrRain = !level.isDay() || level.isRaining();
        boolean hasNectar = pokemon.getPersistentData().getBoolean(NECTAR_KEY);

        if (nightOrRain || hasNectar) {
            if (worker.returningSince < 0) worker.returningSince = now;
            // Se não achar caminho em um tempo, entra mesmo assim (dá um jeito de chegar).
            boolean gaveUp = now - worker.returningSince > RETURN_TIMEOUT_TICKS;
            if (!atHive && !gaveUp) {
                PokemonAi.walkTo(pokemon, entrance, 0.6f, 0);
                return;
            }
            worker.returningSince = -1;
            if (hasNectar) {
                // Com néctar: deixa o mel e descansa lá dentro um pouco, como as abelhas.
                deposit(level, hivePos, hiveState, hasMember(hive, "vespiquen"));
                pokemon.getPersistentData().putBoolean(NECTAR_KEY, false);
                enter(level, hive, pokemon, nightOrRain ? now : now + 200 + level.random.nextInt(400));
            } else {
                enter(level, hive, pokemon, now); // à noite e na chuva, vão para dentro
            }
            return;
        }
        worker.returningSince = -1;

        if (worker.flower == null || !level.getBlockState(worker.flower).is(BlockTags.FLOWERS) || now - worker.targetSince > GIVE_UP_TICKS) {
            worker.flower = findFlower(level, hivePos);
            worker.targetSince = now;
            worker.arrivedAt = -1;
            if (worker.flower == null) return;
        }
        if (!pokemon.blockPosition().closerThan(worker.flower, 2.5)) {
            PokemonAi.walkTo(pokemon, worker.flower, 0.6f, 1);
            return;
        }
        if (worker.arrivedAt < 0) worker.arrivedAt = now;
        level.sendParticles(ParticleTypes.FALLING_NECTAR, pokemon.getX(), pokemon.getY() + 0.5, pokemon.getZ(), 1, 0.2, 0.1, 0.2, 0);
        if (now - worker.arrivedAt >= POLLINATE_TICKS) {
            pollinate(level, worker.flower);
            pokemon.getPersistentData().putBoolean(NECTAR_KEY, true);
            worker.flower = null;
        }
    }

    // ----- Entrar e sair -----

    /** O bloco na frente da colmeia; se estiver bloqueado, o de cima. */
    private static BlockPos entrance(ServerLevel level, BlockPos hivePos, BlockState hiveState) {
        Direction front = hiveState.hasProperty(BeehiveBlock.FACING) ? hiveState.getValue(BeehiveBlock.FACING) : Direction.UP;
        BlockPos pos = hivePos.relative(front);
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() ? pos : hivePos.above();
    }

    /** Guarda a entidade nos dados da colmeia e tira ela do mundo. */
    private static void enter(ServerLevel level, BlockEntity hive, PokemonEntity pokemon, long exitAt) {
        CompoundTag entityData = new CompoundTag();
        if (pokemon.isPassenger() || pokemon.isVehicle() || !pokemon.save(entityData)) return;
        CompoundTag entry = new CompoundTag();
        entry.put("entity", entityData);
        entry.putLong("exit_at", exitAt);
        insideOrCreate(hive).add(entry);
        hive.setChanged();
        occupiedHives.add(GlobalPos.of(level.dimension(), hive.getBlockPos()));
        level.playSound(null, hive.getBlockPos(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1f, 1f);
        workers.remove(pokemon);
        pokemon.discard();
    }

    /** De dia e sem chuva, quem já descansou sai. */
    private static void releaseRested(MinecraftServer server) {
        occupiedHives.removeIf(global -> {
            ServerLevel level = server.getLevel(global.dimension());
            if (level == null) return true;
            if (!level.isLoaded(global.pos())) return false; // continua guardado até o chunk carregar
            if (!(level.getBlockEntity(global.pos()) instanceof BeehiveBlockEntity hive)) return true;
            ListTag inside = inside(hive);
            if (!level.isDay() || level.isRaining()) return inside.isEmpty();
            long now = level.getGameTime();
            for (int i = inside.size() - 1; i >= 0; i--) {
                CompoundTag entry = inside.getCompound(i);
                if (now >= entry.getLong("exit_at")) {
                    inside.remove(i);
                    release(level, global.pos(), entry.getCompound("entity"));
                }
            }
            hive.setChanged();
            return inside.isEmpty();
        });
    }

    private static void releaseAllOnBreak(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof BeehiveBlockEntity hive)) return;
        ListTag inside = inside(hive);
        if (inside.isEmpty()) return;
        pendingReleases.add(new PendingRelease(level, pos.immutable(), inside.copy()));
        inside.clear();
        occupiedHives.remove(GlobalPos.of(level.dimension(), pos));
    }

    /** Recria a entidade guardada na frente da colmeia (ou em cima, se a frente estiver bloqueada). */
    private static void release(ServerLevel level, BlockPos hivePos, CompoundTag entityData) {
        EntityType.create(entityData, level).ifPresent(entity -> {
            BlockState state = level.getBlockState(hivePos);
            BlockPos out = entrance(level, hivePos, state);
            float yaw = state.hasProperty(BeehiveBlock.FACING) ? state.getValue(BeehiveBlock.FACING).toYRot() : 0f;
            entity.moveTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5, yaw, 0f);
            if (level.addFreshEntity(entity)) {
                level.playSound(null, hivePos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1f, 1f);
            }
        });
    }

    private static ListTag inside(BlockEntity hive) {
        return hive.getPersistentData().getList(INSIDE_KEY, Tag.TAG_COMPOUND);
    }

    private static ListTag insideOrCreate(BlockEntity hive) {
        CompoundTag data = hive.getPersistentData();
        if (!data.contains(INSIDE_KEY)) data.put(INSIDE_KEY, new ListTag());
        return data.getList(INSIDE_KEY, Tag.TAG_COMPOUND);
    }

    // ----- Mel e polinização -----

    /** Mais um nível de mel na colmeia. Com a rainha Vespiquen morando nela, às vezes enche dois. */
    private static void deposit(ServerLevel level, BlockPos hive, BlockState state, boolean hasQueen) {
        int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
        int amount = hasQueen && level.random.nextBoolean() ? 2 : 1;
        int max = BeehiveBlock.MAX_HONEY_LEVELS;
        if (honey < max) level.setBlockAndUpdate(hive, state.setValue(BeehiveBlock.HONEY_LEVEL, Math.min(max, honey + amount)));
        level.sendParticles(ParticleTypes.WAX_ON, hive.getX() + 0.5, hive.getY() + 0.5, hive.getZ() + 0.5, 5, 0.4, 0.4, 0.4, 0);
    }

    /** Como as abelhas: uma plantação perto da flor cresce um estágio. */
    private static void pollinate(ServerLevel level, BlockPos flower) {
        for (int i = 0; i < 10; i++) {
            BlockPos pos = flower.offset(level.random.nextInt(9) - 4, level.random.nextInt(3) - 1, level.random.nextInt(9) - 4);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                level.setBlockAndUpdate(pos, crop.getStateForAge(crop.getAge(state) + 1));
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
                return;
            }
        }
    }

    private static BlockPos findFlower(ServerLevel level, BlockPos hive) {
        List<BlockPos> flowers = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(hive.offset(-FLOWER_RADIUS, -5, -FLOWER_RADIUS), hive.offset(FLOWER_RADIUS, 3, FLOWER_RADIUS))) {
            if (level.getBlockState(pos).is(BlockTags.FLOWERS)) flowers.add(pos.immutable());
        }
        return flowers.isEmpty() ? null : flowers.get(level.random.nextInt(flowers.size()));
    }
}
