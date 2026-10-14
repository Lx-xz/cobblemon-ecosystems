package com.luiz.cobblemonecosystems.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.luiz.cobblemonecosystems.PokemonAi;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Ícone de casinha antes do nome dos Pokémon da vila, na etiqueta acima deles.
 *
 * O Cobblemon monta a etiqueta em resolveBaseLabel (o nome, ou "???" antes de escanear na Pokédex) e depois
 * acrescenta o nível no mesmo texto. Por isso o ícone vai num texto à parte: o texto principal fica vazio e na
 * fonte normal, e o nível acrescentado depois não herda a fonte do ícone (que não tem letras e virava quadrados).
 */
@Mixin(PokemonRenderer.class)
public abstract class PokemonRendererMixin {
    @Inject(method = "resolveBaseLabel", at = @At("RETURN"), cancellable = true, remap = false)
    private void cobblemon_ecosystems$villageIcon(PokemonEntity entity, CallbackInfoReturnable<MutableComponent> cir) {
        if (entity.getAspects().contains(PokemonAi.VILLAGE_ASPECT)) {
            cir.setReturnValue(Component.empty().append(PokemonAi.villageIcon()).append(" ").append(cir.getReturnValue()));
        }
    }
}
