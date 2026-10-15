package com.luiz.cobblemonecosystems;

import java.util.UUID;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Pedido do cliente: "mostre o habitat deste Pokémon". Enviado pelo botão de casinha do menu radial do Cobblemon
 * ({@code CobblemonEcosystemsClient}); o servidor responde no chat com o relatório de {@link HabitatScan}.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public record HabitatRequest(UUID pokemon) implements CustomPacketPayload {
    public static final Type<HabitatRequest> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonEcosystems.MOD_ID, "habitat_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HabitatRequest> STREAM_CODEC =
            StreamCodec.composite(UUIDUtil.STREAM_CODEC, HabitatRequest::pokemon, HabitatRequest::new);
    /** Mais longe que isso do Pokémon, o pedido é ignorado. */
    private static final double MAX_DISTANCE = 16;

    @Override
    public Type<HabitatRequest> type() {
        return TYPE;
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(TYPE, STREAM_CODEC, HabitatRequest::handle);
    }

    private static void handle(HabitatRequest request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !Habitat.enabled()) return;
            if (!(player.serverLevel().getEntity(request.pokemon()) instanceof PokemonEntity pokemon)
                    || pokemon.distanceTo(player) > MAX_DISTANCE) return;
            if (pokemon.getTethering() != null) {
                HabitatScan.show(player, pokemon);
            } else {
                player.displayClientMessage(Component.translatable(CobblemonEcosystems.MOD_ID + ".habitat.not_pastured"), true);
            }
        });
    }
}
