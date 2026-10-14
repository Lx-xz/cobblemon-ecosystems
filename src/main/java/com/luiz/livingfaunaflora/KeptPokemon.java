package com.luiz.livingfaunaflora;

import com.cobblemon.mod.common.api.entity.Despawner;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Pokémon criados pelo mod (do mercador ou das vilas) não somem sozinhos como os selvagens. */
@EventBusSubscriber(modid = LivingFaunaFlora.MOD_ID)
public class KeptPokemon {
    private static final String KEEP_KEY = LivingFaunaFlora.MOD_ID + ":keep";

    private static final Despawner<PokemonEntity> NEVER = new Despawner<>() {
        @Override public void beginTracking(PokemonEntity entity) {}
        @Override public boolean shouldDespawn(PokemonEntity entity) { return false; }
    };

    public static void keep(PokemonEntity pokemon) {
        pokemon.getPersistentData().putBoolean(KEEP_KEY, true);
        pokemon.setDespawner(NEVER);
    }

    public static boolean isKept(PokemonEntity pokemon) {
        return pokemon.getPersistentData().getBoolean(KEEP_KEY);
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // O despawner não é salvo no disco, então reaplicamos ao carregar o mundo.
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof PokemonEntity pokemon
                && pokemon.getPersistentData().getBoolean(KEEP_KEY)) {
            pokemon.setDespawner(NEVER);
        }
    }
}
