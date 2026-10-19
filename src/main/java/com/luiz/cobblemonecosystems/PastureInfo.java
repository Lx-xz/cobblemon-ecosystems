package com.luiz.cobblemonecosystems;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Do servidor para o dono: "este Pokémon está no pasto, tem farm e a coleta está ligada?". O cliente não sabe disso
 * sozinho (o Cobblemon não sincroniza o pasto), e o menu radial precisa saber para mostrar os botões de habitat e de
 * coleta só para quem está no pasto. Vai quando o dono clica no Pokémon, antes do pacote do Cobblemon que abre o
 * menu, e o cliente guarda em {@link #KNOWN} até o menu ser montado ({@code CobblemonEcosystemsClient}).
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public record PastureInfo(UUID pokemon, boolean pastured, boolean hasFarm, boolean farmOn) implements CustomPacketPayload {
    public static final Type<PastureInfo> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonEcosystems.MOD_ID, "pasture_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PastureInfo> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PastureInfo::pokemon, ByteBufCodecs.BOOL, PastureInfo::pastured,
            ByteBufCodecs.BOOL, PastureInfo::hasFarm, ByteBufCodecs.BOOL, PastureInfo::farmOn, PastureInfo::new);

    /** No cliente: o que o servidor disse de cada Pokémon (pelo UUID da entidade), até o menu radial usar. */
    static final Map<UUID, PastureInfo> KNOWN = new ConcurrentHashMap<>();

    @Override
    public Type<PastureInfo> type() {
        return TYPE;
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, PastureInfo::handle);
    }

    private static void handle(PastureInfo info, IPayloadContext context) {
        KNOWN.put(info.pokemon(), info);
    }

    /** Como está este Pokémon do jogador agora. */
    static PastureInfo of(PokemonEntity pokemon) {
        boolean pastured = pokemon.getTethering() != null;
        boolean hasFarm = PastureFarms.collects(pokemon);
        return new PastureInfo(pokemon.getUUID(), pastured, hasFarm, !PastureFarms.isOff(pokemon));
    }

    /** O dono clicou no Pokémon: manda o estado antes de o Cobblemon abrir o menu radial. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof PokemonEntity pokemon)
                || !player.getUUID().equals(pokemon.getOwnerUUID()) || !player.connection.hasChannel(TYPE)) return;
        PacketDistributor.sendToPlayer(player, of(pokemon));
    }
}
