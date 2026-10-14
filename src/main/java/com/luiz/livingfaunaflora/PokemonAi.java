package com.luiz.livingfaunaflora;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

/**
 * Atalhos para mandar no cérebro (Brain) dos Pokémon do Cobblemon. Eles não usam os "goals" antigos do
 * Minecraft: andam e brigam lendo memórias como WALK_TARGET e ATTACK_TARGET.
 */
public final class PokemonAi {
    public static final ResourceLocation FIGHTS_MELEE = ResourceLocation.fromNamespaceAndPath("cobblemon", "fights_melee");
    public static final ResourceLocation ATTACK_HOSTILE_MOBS = ResourceLocation.fromNamespaceAndPath("cobblemon", "attack_hostile_mobs");
    /** Nosso: entra no modo briga quando ATTACK_TARGET é preenchido (data/living_fauna_flora/behaviours/hunter.json). */
    public static final ResourceLocation HUNTER = ResourceLocation.fromNamespaceAndPath(LivingFaunaFlora.MOD_ID, "hunter");

    private PokemonAi() {}

    /** Id da espécie, ex.: "golurk". */
    public static String species(PokemonEntity pokemon) {
        return pokemon.getPokemon().getSpecies().getResourceIdentifier().getPath();
    }

    public static boolean isWild(PokemonEntity pokemon) {
        return pokemon.getOwnerUUID() == null;
    }

    /** Liga comportamentos do Cobblemon só nesta entidade (os que ela já tem são mantidos). */
    public static void addBehaviours(PokemonEntity pokemon, ResourceLocation... behaviours) {
        Collection<ResourceLocation> missing = new ArrayList<>();
        for (ResourceLocation behaviour : behaviours) {
            if (!pokemon.getBehaviours().contains(behaviour)) missing.add(behaviour);
        }
        if (!missing.isEmpty()) pokemon.updateBehaviours(List.copyOf(missing));
    }

    public static void walkTo(PokemonEntity pokemon, BlockPos pos, float speed, int closeEnough) {
        pokemon.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, speed, closeEnough));
    }

    public static void walkTo(PokemonEntity pokemon, Entity target, float speed, int closeEnough) {
        pokemon.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, speed, closeEnough));
    }

    public static boolean hasAttackTarget(PokemonEntity pokemon) {
        return pokemon.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isPresent();
    }

    public static void attack(PokemonEntity pokemon, LivingEntity target) {
        pokemon.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
    }
}
