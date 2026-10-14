package com.luiz.cobblemonecosystems.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.luiz.cobblemonecosystems.CobblemonEcosystems;
import com.luiz.cobblemonecosystems.TraderPokemon;

import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;

/** O mercador "shiny" usa a roupa com as cores invertidas, combinando com as mantas dos Mudbray dele. */
@Mixin(WanderingTraderRenderer.class)
public class WanderingTraderRendererMixin {
    private static final ResourceLocation SHINY_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CobblemonEcosystems.MOD_ID, "textures/entity/wandering_trader_shiny.png");

    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/npc/WanderingTrader;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void cobblemonEcosystems$shinyTexture(WanderingTrader trader, CallbackInfoReturnable<ResourceLocation> cir) {
        if (TraderPokemon.isShinyTrader(trader.getUUID())) cir.setReturnValue(SHINY_TEXTURE);
    }
}
