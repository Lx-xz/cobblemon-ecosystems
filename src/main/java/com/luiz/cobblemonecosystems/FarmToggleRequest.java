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
 * Pedido do cliente: "ligue/desligue a coleta deste Pokémon". Enviado pelo botão de cesta do menu radial
 * ({@code CobblemonEcosystemsClient}); só vale para o dono, com o Pokémon no pasto produzindo algo (farm ou ovos,
 * {@link PastureFarms#collects}).
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public record FarmToggleRequest(UUID pokemon) implements CustomPacketPayload {
    public static final Type<FarmToggleRequest> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonEcosystems.MOD_ID, "farm_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FarmToggleRequest> STREAM_CODEC =
            StreamCodec.composite(UUIDUtil.STREAM_CODEC, FarmToggleRequest::pokemon, FarmToggleRequest::new);
    /** Mais longe que isso do Pokémon, o pedido é ignorado. */
    private static final double MAX_DISTANCE = 16;

    @Override
    public Type<FarmToggleRequest> type() {
        return TYPE;
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(TYPE, STREAM_CODEC, FarmToggleRequest::handle);
    }

    private static void handle(FarmToggleRequest request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.serverLevel().getEntity(request.pokemon()) instanceof PokemonEntity pokemon)
                    || pokemon.distanceTo(player) > MAX_DISTANCE || !player.getUUID().equals(pokemon.getOwnerUUID())
                    || !PastureFarms.collects(pokemon)) return;
            boolean off = !PastureFarms.isOff(pokemon);
            PastureFarms.setOff(pokemon, off);
            player.displayClientMessage(Component.translatable(CobblemonEcosystems.MOD_ID + ".habitat.farm." + (off ? "turned_off" : "turned_on"),
                    pokemon.getPokemon().getDisplayName(false)), true);
        });
    }
}
