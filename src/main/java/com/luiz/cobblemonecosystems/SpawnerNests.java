package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Spawners de mobs que viraram Pokémon não fazem mais sentido (e virariam farm infinita de Pokémon). Quando o chunk
 * carrega, o spawner some e deixa um ninho: alguns Pokémon de uma espécie só, que ficam por ali e não somem sozinhos.
 * - Aranha e aranha de caverna (minas abandonadas, masmorras): Spinarak, Joltik ou Tarountula (e evoluções).
 *   Só enquanto as aranhas estiverem removidas na configuração.
 * - Traça (sala do portal da fortaleza): Dwebble, se as traças virarem Dwebble.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class SpawnerNests {
    private record Family(String base, String evolved, int weight) {}
    private static final List<Family> SPIDERS = List.of(
            new Family("spinarak", "ariados", 40), new Family("joltik", "galvantula", 30), new Family("tarountula", "spidops", 30));
    private static final Family DWEBBLE = new Family("dwebble", "crustle", 1);
    private static final float EVOLVED_CHANCE = 0.25f;
    private static final int MIN_NEST = 3, MAX_NEST = 5;
    /** Os do ninho não se afastam mais que isso dele. */
    private static final double HOME_RADIUS = 12;

    private static final String HOME_KEY = CobblemonEcosystems.MOD_ID + ":nest_home";

    private record Pending(ServerLevel level, BlockPos pos) {}
    private static final List<Pending> pending = new ArrayList<>();
    private static final Set<PokemonEntity> nestlings = new HashSet<>();
    private static int tick;

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof SpawnerBlockEntity && nestFamily(spawnedType(blockEntity)) != null) {
                // Mexer em blocos e criar entidades durante o carregamento do chunk não é seguro: fica para o fim do tick.
                pending.add(new Pending(level, blockEntity.getBlockPos()));
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof PokemonEntity pokemon
                && pokemon.getPersistentData().contains(HOME_KEY)) {
            nestlings.add(pokemon);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Pending p : List.copyOf(pending)) replace(p.level(), p.pos());
        pending.clear();

        if (++tick % 40 != 0) return;
        nestlings.removeIf(pokemon -> pokemon.isRemoved() || !PokemonAi.isWild(pokemon));
        for (PokemonEntity pokemon : nestlings) {
            if (pokemon.isBusy() || Hunts.isHunting(pokemon)) continue;
            BlockPos home = BlockPos.of(pokemon.getPersistentData().getLong(HOME_KEY));
            if (!pokemon.blockPosition().closerThan(home, HOME_RADIUS)) PokemonAi.walkTo(pokemon, home, 0.6f, 3);
        }
    }

    /** O tipo de mob do spawner (pelo SpawnData salvo), ou null. */
    private static EntityType<?> spawnedType(BlockEntity spawner) {
        CompoundTag data = ((SpawnerBlockEntity) spawner).getSpawner().save(new CompoundTag());
        String id = data.getCompound("SpawnData").getCompound("entity").getString("id");
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key) ? null : BuiltInRegistries.ENTITY_TYPE.get(key);
    }

    /** As famílias do ninho para esse spawner, ou null se ele continua como está. */
    private static List<Family> nestFamily(EntityType<?> type) {
        if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER) {
            return Config.SPIDER_SPAWNER_NESTS.get() && Removal.shouldRemove(type) ? SPIDERS : null;
        }
        if (type == EntityType.SILVERFISH) return Config.SILVERFISH_DWEBBLE.get() ? List.of(DWEBBLE) : null;
        return null;
    }

    private static void replace(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)) return;
        List<Family> families = nestFamily(spawnedType(spawner));
        if (families == null) return;
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

        RandomSource random = level.random;
        Family family = pick(families, random);
        int count = MIN_NEST + random.nextInt(MAX_NEST - MIN_NEST + 1);
        for (int i = 0; i < count; i++) {
            boolean evolved = random.nextFloat() < EVOLVED_CHANCE;
            int pokemonLevel = evolved ? 25 + random.nextInt(8) : 10 + random.nextInt(11);
            PokemonEntity pokemon = PokemonProperties.Companion.parse((evolved ? family.evolved() : family.base()) + " level=" + pokemonLevel)
                    .createEntity(level);
            pokemon.moveTo(pos.getX() + 0.5 + random.nextDouble() - 0.5, pos.getY(), pos.getZ() + 0.5 + random.nextDouble() - 0.5,
                    random.nextFloat() * 360f, 0f);
            pokemon.getPersistentData().putLong(HOME_KEY, pos.asLong());
            KeptPokemon.keep(pokemon);
            if (level.addFreshEntity(pokemon)) nestlings.add(pokemon);
        }
    }

    private static Family pick(List<Family> families, RandomSource random) {
        int roll = random.nextInt(families.stream().mapToInt(Family::weight).sum());
        for (Family family : families) {
            roll -= family.weight();
            if (roll < 0) return family;
        }
        return families.get(0);
    }
}
