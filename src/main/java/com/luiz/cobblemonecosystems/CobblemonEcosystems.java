package com.luiz.cobblemonecosystems;

import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity entity = event.getEntity();
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
}
