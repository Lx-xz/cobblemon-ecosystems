package com.luiz.cobblemonecosystems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Remove os animais vanilla. Os spawns naturais são tirados dos biomas pelo biome modifier em
 * data/cobblemon_ecosystems/neoforge/biome_modifier; aqui bloqueamos o resto (ovos, reprodução, gatos de vila,
 * animais já salvos em mundos antigos...).
 */
@Mod(CobblemonEcosystems.MOD_ID)
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class CobblemonEcosystems {
    public static final String MOD_ID = "cobblemon_ecosystems";

    /** Lista editável em data/cobblemon_ecosystems/tags/entity_type/removed_animals.json (ou por datapack). */
    public static final TagKey<EntityType<?>> REMOVED_ANIMALS =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "removed_animals"));

    public CobblemonEcosystems() {
        TraderBattle.registerCobblemonEvents();
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity().getType().is(REMOVED_ANIMALS)) {
            event.setCanceled(true);
            StructurePokemon.replaceIfMarked(event.getEntity(), level);
            VillageGuardians.replaceIfMarked(event.getEntity(), level);
            HivePokemon.replaceIfFromHive(event.getEntity(), level);
        }
    }
}
