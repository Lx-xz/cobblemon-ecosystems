package com.luiz.cobblemonecosystems;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Quais mobs saem do jogo, de acordo com a configuração. Três grupos, cada um com sua lista na configuração
 * (que por padrão aponta para uma tag, editável por datapack): passivos, hostis e slimes. O golem de ferro sai
 * junto com o módulo dos guardiões da vila.
 */
public final class Removal {
    public static final TagKey<EntityType<?>> PASSIVE = tag("removed_animals");
    public static final TagKey<EntityType<?>> HOSTILE = tag("removed_hostile_animals");
    public static final TagKey<EntityType<?>> SLIMES = tag("removed_slimes");

    private Removal() {}

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(CobblemonEcosystems.MOD_ID, name));
    }

    public static boolean shouldRemove(EntityType<?> type) {
        if (type == EntityType.IRON_GOLEM) return Config.VILLAGE_GUARDIANS.get();
        return (Config.REMOVE_PASSIVE_ANIMALS.get() && ConfigLists.mobs(Config.PASSIVE_ANIMALS_LIST).matches(type))
                || (Config.REMOVE_HOSTILE_ANIMALS.get() && ConfigLists.mobs(Config.HOSTILE_ANIMALS_LIST).matches(type))
                || (Config.REMOVE_SLIMES.get() && ConfigLists.mobs(Config.SLIMES_LIST).matches(type));
    }

    /**
     * Biome modifier que tira dos biomas os spawns naturais dos grupos ligados na configuração
     * (data/cobblemon_ecosystems/neoforge/biome_modifier/remove_animals.json). Roda quando o mundo abre.
     */
    public record ConfiguredSpawnRemoval() implements BiomeModifier {
        public static final MapCodec<ConfiguredSpawnRemoval> CODEC = MapCodec.unit(ConfiguredSpawnRemoval::new);

        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            if (phase != Phase.REMOVE) return;
            var spawns = builder.getMobSpawnSettings();
            for (MobCategory category : MobCategory.values()) {
                spawns.getSpawner(category).removeIf(data -> shouldRemove(data.type));
            }
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            return CODEC;
        }
    }
}
