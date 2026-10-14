package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * O mercador ambulante chega com Pokémon de carga presos na guia, no lugar das lhamas.
 * Eles usam a manta do mercador, são incapturáveis e vão embora junto com ele.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class TraderPokemon {
    /** Mudbray é um burrinho de carga. A manta só existe para o Mudbray (veja o resolver em assets/cobblemon). */
    private static final String PROPERTIES = "mudbray level=15 uncatchable";
    private static final int COUNT = 2;
    private static final String NAME = "Mudbray do Mercador";
    /** Aspect que liga a camada da manta no resolver 1_cobblemon_ecosystems_trader.json. */
    private static final String BLANKET_ASPECT = "cobblemon_ecosystems_trader";
    private static final String OWNER_KEY = CobblemonEcosystems.MOD_ID + ":trader";

    private static final List<WanderingTrader> newTraders = new ArrayList<>();
    private static final Set<PokemonEntity> owned = new HashSet<>();
    private static int tick;

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        Entity entity = event.getEntity();

        if (entity instanceof WanderingTrader trader && !event.loadedFromDisk()) {
            // Não dá para criar entidades no meio deste evento; fica para o fim do tick.
            newTraders.add(trader);
        } else if (entity instanceof PokemonEntity pokemon && pokemon.getPersistentData().hasUUID(OWNER_KEY)) {
            owned.add(pokemon);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (WanderingTrader trader : newTraders) {
            if (trader.isAlive()) spawnPackPokemon(trader);
        }
        newTraders.clear();

        if (++tick % 100 == 0) checkOwners();
    }

    private static void spawnPackPokemon(WanderingTrader trader) {
        ServerLevel level = (ServerLevel) trader.level();
        for (int i = 0; i < COUNT; i++) {
            PokemonEntity pokemon = PokemonProperties.Companion.parse(PROPERTIES).createEntity(level);
            pokemon.getPokemon().setForcedAspects(new HashSet<>(Set.of(BLANKET_ASPECT)));
            pokemon.getPokemon().updateAspects();
            pokemon.getPokemon().setNickname(Component.literal(NAME));

            double x = trader.getX() + level.random.nextInt(5) - 2;
            double z = trader.getZ() + level.random.nextInt(5) - 2;
            pokemon.moveTo(x, trader.getY(), z, level.random.nextFloat() * 360f, 0f);
            KeptPokemon.keep(pokemon);
            pokemon.getPersistentData().putUUID(OWNER_KEY, trader.getUUID());

            if (level.addFreshEntity(pokemon)) {
                pokemon.setLeashedTo(trader, true);
                owned.add(pokemon);
            }
        }
    }

    /** Os Pokémon de carga deste mercador que estão carregados no mundo. */
    static List<PokemonEntity> packOf(WanderingTrader trader) {
        List<PokemonEntity> pack = new ArrayList<>();
        for (PokemonEntity pokemon : owned) {
            if (!pokemon.isRemoved() && trader.getUUID().equals(pokemon.getPersistentData().getUUID(OWNER_KEY))) {
                pack.add(pokemon);
            }
        }
        return pack;
    }

    /** UUID do mercador dono deste Pokémon, ou null se não for de mercador. */
    static UUID ownerOf(PokemonEntity pokemon) {
        return pokemon.getPersistentData().hasUUID(OWNER_KEY) ? pokemon.getPersistentData().getUUID(OWNER_KEY) : null;
    }

    static boolean isTraderPokemon(com.cobblemon.mod.common.pokemon.Pokemon pokemon) {
        return pokemon.getForcedAspects().contains(BLANKET_ASPECT);
    }

    /** Quando o mercador some (despawn ou morte), os Pokémon dele vão junto. */
    private static void checkOwners() {
        owned.removeIf(pokemon -> {
            if (pokemon.isRemoved()) return true;
            UUID owner = pokemon.getPersistentData().getUUID(OWNER_KEY);
            Entity trader = ((ServerLevel) pokemon.level()).getEntity(owner);
            if (trader == null || !trader.isAlive()) {
                pokemon.discard();
                return true;
            }
            return false;
        });
    }
}
