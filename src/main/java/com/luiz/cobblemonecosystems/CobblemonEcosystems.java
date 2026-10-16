package com.luiz.cobblemonecosystems;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Remove os mobs vanilla escolhidos na configuração ({@link Removal}). Os spawns naturais saem dos biomas pelo
 * biome modifier {@link Removal.ConfiguredSpawnRemoval}; aqui bloqueamos o resto (ovos, reprodução, gatos de vila,
 * mobs já salvos em mundos antigos...).
 */
@Mod(CobblemonEcosystems.MOD_ID)
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class CobblemonEcosystems {
    public static final String MOD_ID = "cobblemon_ecosystems";

    /** Baldes com bicho: se o bicho foi removido, esvaziar o balde não solta nada. */
    private static final Map<Item, EntityType<?>> MOB_BUCKETS = Map.of(
            Items.COD_BUCKET, EntityType.COD, Items.SALMON_BUCKET, EntityType.SALMON,
            Items.PUFFERFISH_BUCKET, EntityType.PUFFERFISH, Items.TROPICAL_FISH_BUCKET, EntityType.TROPICAL_FISH,
            Items.AXOLOTL_BUCKET, EntityType.AXOLOTL, Items.TADPOLE_BUCKET, EntityType.TADPOLE);

    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MOD_ID);
    static {
        BIOME_MODIFIERS.register("remove_configured_spawns", () -> Removal.ConfiguredSpawnRemoval.CODEC);
    }

    public CobblemonEcosystems(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        BIOME_MODIFIERS.register(modBus);
        TraderBattle.registerCobblemonEvents();
        VillagerBattles.registerCobblemonEvents();
        HutWitch.registerCobblemonEvents();
        HabitatScan.registerCobblemonEvents();
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
        if (entity instanceof WanderingTrader trader) removeMobBucketTrades(trader);
        if (!Removal.shouldRemove(entity.getType())) return;
        // Mobs já salvos no mundo podem ficar, se a configuração mandar. Os de estrutura marcados para virar
        // Pokémon também vêm "do disco" (o chunk novo é salvo antes de carregar), e esses sempre são trocados.
        boolean marked = StructurePokemon.isMarked(entity) || VillageGuardians.isMarked(entity);
        if (event.loadedFromDisk() && !marked && !Config.REMOVE_FROM_EXISTING_WORLDS.get()) return;

        event.setCanceled(true);
        StructurePokemon.replaceIfMarked(entity, level);
        VillageGuardians.replaceIfMarked(entity, level);
        HivePokemon.replaceIfFromHive(entity, level);
    }

    /** O mercador não vende balde de bicho removido (baiacu e peixe tropical, no vanilla). */
    private static void removeMobBucketTrades(WanderingTrader trader) {
        trader.getOffers().removeIf(offer -> {
            EntityType<?> type = MOB_BUCKETS.get(offer.getResult().getItem());
            return type != null && Removal.shouldRemove(type);
        });
    }
}
