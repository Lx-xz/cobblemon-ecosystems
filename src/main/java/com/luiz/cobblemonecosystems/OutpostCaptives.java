package com.luiz.cobblemonecosystems;

import java.util.List;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;

/**
 * Postos de saqueadores com um Pokémon preso na jaula, no lugar do golem de ferro (que virou Golett e Golurk nas
 * vilas, {@link VillageGuardians}). O Pokémon é selvagem e não some sozinho: quem invadir o posto e abrir a jaula
 * pode batalhar e capturar. As jaulas de allay continuam como estão.
 */
public final class OutpostCaptives {
    /** Pokémon que os saqueadores levariam: fortes, raros ou úteis. Pesos: o Golett, parente do golem, é o mais comum. */
    private record Captive(String species, int weight) {}
    private static final List<Captive> CAPTIVES = List.of(
            new Captive("golett", 25), new Captive("growlithe", 15), new Captive("ponyta", 12), new Captive("mudbray", 12),
            new Captive("riolu", 10), new Captive("eevee", 10), new Captive("larvitar", 8), new Captive("absol", 8));
    private static final int MIN_LEVEL = 15, MAX_LEVEL = 30;

    private OutpostCaptives() {}

    static boolean isInOutpost(ServerLevelAccessor level, BlockPos pos) {
        Structure outpost = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BuiltinStructures.PILLAGER_OUTPOST);
        return outpost != null && level.getLevel().structureManager().getStructureWithPieceAt(pos, outpost).isValid();
    }

    static void spawn(ServerLevel level, Vec3 pos, float yaw) {
        RandomSource random = level.random;
        String species = pick(random);
        int pokemonLevel = MIN_LEVEL + random.nextInt(MAX_LEVEL - MIN_LEVEL + 1);
        PokemonEntity pokemon = PokemonProperties.Companion.parse(species + " level=" + pokemonLevel).createEntity(level);
        pokemon.moveTo(pos.x, pos.y, pos.z, yaw, 0f);
        KeptPokemon.keep(pokemon);
        level.addFreshEntity(pokemon);
    }

    private static String pick(RandomSource random) {
        int roll = random.nextInt(CAPTIVES.stream().mapToInt(Captive::weight).sum());
        for (Captive captive : CAPTIVES) {
            roll -= captive.weight();
            if (roll < 0) return captive.species();
        }
        return CAPTIVES.get(0).species();
    }
}
