package com.luiz.cobblemonecosystems;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Pedido do cliente: apertou a tecla de soltar Pokémon do Cobblemon (R) mirando num aldeão, mercador ou bruxa.
 * O Cobblemon não solta a Pokébola nesses alvos, então o cliente ({@code ClientHints}) avisa o servidor e o desafio
 * começa como no agachar + clique direito.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public record ChallengeRequest(int entityId) implements CustomPacketPayload {
    public static final Type<ChallengeRequest> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonEcosystems.MOD_ID, "challenge_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChallengeRequest> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, ChallengeRequest::entityId, ChallengeRequest::new);
    /** Mais longe que isso do alvo, o pedido é ignorado. */
    private static final double MAX_DISTANCE = 12;

    @Override
    public Type<ChallengeRequest> type() {
        return TYPE;
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToServer(TYPE, STREAM_CODEC, ChallengeRequest::handle);
    }

    private static void handle(ChallengeRequest request, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            Entity target = player.serverLevel().getEntity(request.entityId());
            if (target == null || target.distanceTo(player) > MAX_DISTANCE) return;
            if (!VillagerBattles.tryChallenge(player, target) && !TraderBattle.tryChallenge(player, target)) {
                HutWitch.tryChallenge(player, target);
            }
        });
    }
}
