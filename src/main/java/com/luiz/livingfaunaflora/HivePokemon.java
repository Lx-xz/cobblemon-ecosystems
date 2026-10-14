package com.luiz.livingfaunaflora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Pokémon abelha moram nas colmeias e ninhos no lugar das abelhas. Cada abelha que sairia da colmeia
 * vira um Pokémon ligado a ela, que visita flores, poliniza plantações e enche a colmeia de mel.
 */
@EventBusSubscriber(modid = LivingFaunaFlora.MOD_ID)
public class HivePokemon {
    /** Espécie e peso do sorteio. Vespiquen é a rainha: no máximo uma por colmeia. */
    private static final List<Map.Entry<String, Integer>> SPECIES = List.of(
            Map.entry("combee", 55), Map.entry("cutiefly", 30), Map.entry("ribombee", 8), Map.entry("vespiquen", 7));
    private static final int MAX_PER_HIVE = 3;
    private static final int FLOWER_RADIUS = 10;
    private static final int POLLINATE_TICKS = 100;
    /** Desiste de um alvo (flor ou colmeia) que não alcança nesse tempo. */
    private static final int GIVE_UP_TICKS = 1200;

    private static final String HIVE_KEY = LivingFaunaFlora.MOD_ID + ":hive";
    private static final String NECTAR_KEY = LivingFaunaFlora.MOD_ID + ":nectar";
    /** Na colmeia: quantos Pokémon ela já soltou. */
    private static final String SPAWNED_KEY = LivingFaunaFlora.MOD_ID + ":hive_spawned";

    private record Pending(ServerLevel level, Vec3 pos, BlockPos hive) {}
    private static final List<Pending> pending = new ArrayList<>();

    /** Estado de cada trabalhador (não salvo: recomeça do zero ao recarregar, sem problema). */
    private static final class Worker {
        BlockPos flower;
        long targetSince;
        long arrivedAt = -1;
        long restUntil;
    }
    private static final Map<PokemonEntity, Worker> workers = new HashMap<>();
    private static int tick;

    /** Chamado por LivingFaunaFlora quando uma abelha bloqueada estava saindo de uma colmeia. */
    static void replaceIfFromHive(Entity entity, ServerLevel level) {
        if (entity instanceof Bee bee && bee.hasHive()) pending.add(new Pending(level, bee.position(), bee.getHivePos()));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof PokemonEntity pokemon
                && pokemon.getPersistentData().contains(HIVE_KEY)) {
            workers.putIfAbsent(pokemon, new Worker());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Pending p : List.copyOf(pending)) spawn(p);
        pending.clear();

        if (++tick % 20 != 0) return;
        workers.entrySet().removeIf(entry -> entry.getKey().isRemoved());
        for (Map.Entry<PokemonEntity, Worker> entry : List.copyOf(workers.entrySet())) work(entry.getKey(), entry.getValue());
    }

    private static void spawn(Pending p) {
        // A abelha cancelada continua "dentro" da colmeia e o jogo tenta soltá-la de novo a cada tick.
        // A contagem fica salva na própria colmeia, porque ao carregar o chunk ela pode funcionar antes
        // de os Pokémon dela carregarem.
        BlockEntity hiveEntity = p.level().getBlockEntity(p.hive());
        if (hiveEntity == null) return;
        int spawned = hiveEntity.getPersistentData().getInt(SPAWNED_KEY);
        if (spawned >= MAX_PER_HIVE) return;
        RandomSource random = p.level().random;
        String species = pickSpecies(random, hasQueen(p.level(), p.hive()));
        boolean evolved = species.equals("ribombee") || species.equals("vespiquen");
        int level = evolved ? 20 + random.nextInt(11) : 5 + random.nextInt(11);
        // Vespiquen só existe fêmea; Combee que vira Vespiquen também precisa ser fêmea.
        String gender = species.equals("vespiquen") ? " gender=female" : "";

        PokemonEntity pokemon = PokemonProperties.Companion.parse(species + " level=" + level + gender).createEntity(p.level());
        pokemon.moveTo(p.pos().x, p.pos().y, p.pos().z, random.nextFloat() * 360f, 0f);
        pokemon.getPersistentData().putLong(HIVE_KEY, p.hive().asLong());
        KeptPokemon.keep(pokemon);
        if (p.level().addFreshEntity(pokemon)) {
            workers.putIfAbsent(pokemon, new Worker());
            hiveEntity.getPersistentData().putInt(SPAWNED_KEY, spawned + 1);
            hiveEntity.setChanged();
        }
    }

    private static void work(PokemonEntity pokemon, Worker worker) {
        if (pokemon.isBusy() || !PokemonAi.isWild(pokemon)) return;
        ServerLevel level = (ServerLevel) pokemon.level();
        BlockPos hive = BlockPos.of(pokemon.getPersistentData().getLong(HIVE_KEY));
        if (!level.isLoaded(hive)) return;
        BlockState hiveState = level.getBlockState(hive);
        if (!hiveState.is(BlockTags.BEEHIVES)) {
            // Colmeia quebrada: vira um Pokémon selvagem comum.
            pokemon.getPersistentData().remove(HIVE_KEY);
            workers.remove(pokemon);
            return;
        }
        long now = level.getGameTime();

        // À noite e na chuva, ficam perto de casa.
        if (!level.isDay() || level.isRaining()) {
            PokemonAi.walkTo(pokemon, hive, 0.6f, 3);
            return;
        }

        if (pokemon.getPersistentData().getBoolean(NECTAR_KEY)) {
            if (pokemon.blockPosition().closerThan(hive, 3)) {
                deposit(level, hive, hiveState);
                pokemon.getPersistentData().putBoolean(NECTAR_KEY, false);
                worker.restUntil = now + 200 + level.random.nextInt(400);
            } else {
                PokemonAi.walkTo(pokemon, hive, 0.6f, 1);
            }
            return;
        }
        if (now < worker.restUntil) return;

        if (worker.flower == null || !level.getBlockState(worker.flower).is(BlockTags.FLOWERS) || now - worker.targetSince > GIVE_UP_TICKS) {
            worker.flower = findFlower(level, hive);
            worker.targetSince = now;
            worker.arrivedAt = -1;
            if (worker.flower == null) {
                worker.restUntil = now + 600;
                return;
            }
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

    /** Mais um nível de mel na colmeia. Com a rainha Vespiquen por perto, às vezes enche dois. */
    private static void deposit(ServerLevel level, BlockPos hive, BlockState state) {
        int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
        int amount = hasQueen(level, hive) && level.random.nextBoolean() ? 2 : 1;
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

    private static boolean hasQueen(ServerLevel level, BlockPos hive) {
        for (PokemonEntity pokemon : workers.keySet()) {
            if (!pokemon.isRemoved() && pokemon.level() == level && PokemonAi.species(pokemon).equals("vespiquen")
                    && pokemon.getPersistentData().getLong(HIVE_KEY) == hive.asLong()) return true;
        }
        return false;
    }

    private static String pickSpecies(RandomSource random, boolean hasQueen) {
        int total = SPECIES.stream().mapToInt(Map.Entry::getValue).sum();
        int roll = random.nextInt(total);
        for (Map.Entry<String, Integer> entry : SPECIES) {
            roll -= entry.getValue();
            if (roll < 0) return entry.getKey().equals("vespiquen") && hasQueen ? "combee" : entry.getKey();
        }
        return "combee";
    }
}
