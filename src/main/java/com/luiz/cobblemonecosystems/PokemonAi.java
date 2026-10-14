package com.luiz.cobblemonecosystems;

import java.util.HashSet;
import java.util.Set;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

/**
 * Atalhos para os Pokémon do Cobblemon. Eles não usam os "goals" antigos do Minecraft: andam lendo a
 * memória WALK_TARGET do cérebro (Brain). Não trocamos os comportamentos do cérebro (isso deixava o
 * Pokémon parado); brigas são feitas pelo próprio mod em {@link Hunts}.
 */
public final class PokemonAi {
    /** Fonte com o ícone da vila (assets/cobblemon_ecosystems/font/icons.json), no caractere U+E000. */
    private static final ResourceLocation ICON_FONT = ResourceLocation.fromNamespaceAndPath(CobblemonEcosystems.MOD_ID, "icons");
    private static final String VILLAGE_ICON = "\uE000";
    /** Marca dos Pokémon da vila, lida no cliente para desenhar o ícone. */
    public static final String VILLAGE_ASPECT = "ecosystems_village";

    private PokemonAi() {}

    /** Id da espécie, ex.: "golurk". */
    public static String species(PokemonEntity pokemon) {
        return pokemon.getPokemon().getSpecies().getResourceIdentifier().getPath();
    }

    public static boolean isWild(PokemonEntity pokemon) {
        return pokemon.getOwnerUUID() == null;
    }

    public static void walkTo(PokemonEntity pokemon, BlockPos pos, float speed, int closeEnough) {
        pokemon.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, speed, closeEnough));
    }

    public static void walkTo(PokemonEntity pokemon, Entity target, float speed, int closeEnough) {
        pokemon.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, speed, closeEnough));
    }

    /**
     * Marca os Pokémon da vila (incapturáveis) com o aspect {@link #VILLAGE_ASPECT}. As marcas vão para o
     * cliente, e o {@code PokemonRendererMixin} desenha o ícone de casinha antes do nome, mesmo antes de
     * escanear na Pokédex (quando o nome aparece como "???").
     * Também limpa o apelido com ícone das versões antigas (ele só aparecia depois de escanear).
     */
    public static void markAsVillagePokemon(PokemonEntity pokemon) {
        Set<String> aspects = new HashSet<>(pokemon.getPokemon().getForcedAspects());
        if (aspects.add(VILLAGE_ASPECT)) {
            pokemon.getPokemon().setForcedAspects(aspects);
            pokemon.getPokemon().updateAspects();
        }
        if (pokemon.getPokemon().getNickname() != null
                && pokemon.getPokemon().getNickname().getString().startsWith(VILLAGE_ICON)) {
            pokemon.getPokemon().setNickname(null);
        }
    }

    /** O ícone de casinha, na fonte dele. */
    public static MutableComponent villageIcon() {
        return Component.literal(VILLAGE_ICON).withStyle(style -> style.withFont(ICON_FONT));
    }
}
