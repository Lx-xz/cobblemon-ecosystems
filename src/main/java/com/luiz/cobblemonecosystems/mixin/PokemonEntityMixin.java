package com.luiz.cobblemonecosystems.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.luiz.cobblemonecosystems.PastureReturn;

/**
 * Pokémon do pasto fora da área: em vez de ir para o PC quando o teleporte falha, volta andando
 * ({@link PastureReturn}).
 */
@Mixin(value = PokemonEntity.class, remap = false)
public abstract class PokemonEntityMixin {
    @Inject(method = "checkPastureTether", at = @At("HEAD"), cancellable = true)
    private void cobblemon_ecosystems$walkBackToPasture(CallbackInfo ci) {
        if (PastureReturn.handle((PokemonEntity) (Object) this)) ci.cancel();
    }
}
