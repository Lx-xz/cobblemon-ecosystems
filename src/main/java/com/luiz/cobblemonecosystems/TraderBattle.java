package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Batalha contra o mercador com os Mudbray dele. Para desafiar: agachar + clique direito nele,
 * ou lançar um Pokémon (tecla R) nele ou em um dos Mudbray.
 * Quem vence pode capturar um deles (só quem venceu, e só um por mercador).
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class TraderBattle {
    /** Chave de tradução do nome. O Cobblemon trata o nome do treinador como chave e traduz na tela de batalha. */
    private static final String TRADER_NAME = "cobblemon_ecosystems.trader.name";
    /** No mercador: quem já venceu ele. No Mudbray: quem pode capturá-lo. */
    private static final String WINNER_KEY = CobblemonEcosystems.MOD_ID + ":winner";
    private static final int AI_SKILL = 3;
    /** Lançar um Pokémon a até essa distância (em blocos) do mercador conta como desafio. */
    private static final double SEND_OUT_RADIUS = 2.5;

    private record Challenge(ServerPlayer player, WanderingTrader trader, UUID leadingPokemon) {}
    /** Desafios vindos de eventos do Cobblemon; começam no fim do tick, fora do evento. */
    private static final List<Challenge> pendingChallenges = new ArrayList<>();

    private record ActiveBattle(ServerPlayer player, List<PokemonEntity> hiddenPack) {}
    /** Mercadores batalhando agora. Quando a batalha acaba, eles e os Mudbray voltam ao normal. */
    private static final Map<WanderingTrader, ActiveBattle> activeBattles = new HashMap<>();

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!Config.TRADER_BATTLE.get() || !(event.getTarget() instanceof WanderingTrader trader)
                || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) return;
        // Agachado não abre a tela de trocas; o clique vira o desafio.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND) challenge(player, trader, null);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Challenge c : List.copyOf(pendingChallenges)) challenge(c.player(), c.trader(), c.leadingPokemon());
        pendingChallenges.clear();

        activeBattles.entrySet().removeIf(entry -> {
            WanderingTrader trader = entry.getKey();
            ActiveBattle battle = entry.getValue();
            if (PlayerExtensionsKt.isInBattle(battle.player()) && !trader.isRemoved()) return false;
            trader.setNoAi(false);
            for (PokemonEntity pokemon : battle.hiddenPack()) {
                if (pokemon.isRemoved()) continue;
                pokemon.setInvisible(false);
                pokemon.setNoAi(false);
                // O prêmio de quem venceu fica solto; os outros voltam para a guia.
                if (!pokemon.getPersistentData().hasUUID(WINNER_KEY) && !trader.isRemoved()) {
                    pokemon.setLeashedTo(trader, true);
                }
            }
            return true;
        });
    }

    private static void challenge(ServerPlayer player, WanderingTrader trader, UUID leadingPokemon) {
        if (trader.isRemoved() || activeBattles.containsKey(trader)) return;
        if (trader.getPersistentData().hasUUID(WINNER_KEY)) {
            say(player, "already_beaten");
            return;
        }
        if (PlayerExtensionsKt.isInBattle(player)) return;

        List<PokemonEntity> pack = TraderPokemon.packOf(trader);
        if (pack.isEmpty()) {
            say(player, "no_pokemon");
            return;
        }

        PlayerPartyStore party = PlayerExtensionsKt.party(player);
        List<BattlePokemon> playerTeam = party.toBattleTeam(false, false, leadingPokemon);
        if (playerTeam.stream().allMatch(p -> p.getOriginalPokemon().isFainted())) {
            say(player, "party_fainted");
            return;
        }

        // Cópias: os Mudbray de verdade continuam inteiros, ganhe ou perca.
        List<BattlePokemon> traderTeam = pack.stream()
                .map(p -> BattlePokemon.Companion.safeCopyOf(p.getPokemon()))
                .toList();

        say(player, "challenge");
        BattleRegistry.startBattle(
                BattleFormat.Companion.getGEN_9_SINGLES(),
                new BattleSide(new PlayerBattleActor(player.getUUID(), playerTeam)),
                new BattleSide(new MobTrainerActor(TRADER_NAME, trader, traderTeam, AI_SKILL)),
                false);

        // Mercador parado olhando para o jogador; os Mudbray de verdade somem enquanto as cópias batalham.
        trader.getNavigation().stop();
        trader.setNoAi(true);
        trader.lookAt(player, 360f, 360f);
        for (PokemonEntity pokemon : pack) {
            pokemon.dropLeash(true, false);
            pokemon.setNoAi(true);
            pokemon.setInvisible(true);
        }
        activeBattles.put(trader, new ActiveBattle(player, pack));
    }

    /** Eventos do Cobblemon; chamado no construtor do mod. */
    static void registerCobblemonEvents() {
        // R mirando num Mudbray do mercador abriria batalha selvagem com ele: vira desafio ao mercador.
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL, event -> {
            if (!Config.TRADER_BATTLE.get()) return;
            WanderingTrader trader = null;
            ServerPlayer player = null;
            UUID leading = null;
            for (BattleActor actor : event.getBattle().getActors()) {
                if (actor instanceof PokemonBattleActor wild && wild.getEntity() != null
                        && !wild.getEntity().getPersistentData().hasUUID(WINNER_KEY)) {
                    UUID owner = TraderPokemon.ownerOf(wild.getEntity());
                    if (owner != null) trader = findTrader(owner);
                } else if (actor instanceof PlayerBattleActor playerActor) {
                    player = findPlayer(playerActor.getUuid());
                    if (!playerActor.getPokemonList().isEmpty()) {
                        leading = playerActor.getPokemonList().get(0).getOriginalPokemon().getUuid();
                    }
                }
            }
            if (trader != null && player != null) {
                event.setReason(Component.translatable("cobblemon_ecosystems.trader.wants_to_battle"));
                event.cancel();
                pendingChallenges.add(new Challenge(player, trader, leading));
            }
        });

        // R mirando no mercador só solta o Pokémon perto dele: se cair perto, vira desafio.
        CobblemonEvents.POKEMON_SENT_POST.subscribe(Priority.NORMAL, event -> {
            if (!Config.TRADER_BATTLE.get()) return;
            Pokemon pokemon = event.getPokemon();
            ServerPlayer player = pokemon.getOwnerPlayer();
            if (player == null || PlayerExtensionsKt.isInBattle(player)) return;
            AABB area = new AABB(event.getPosition(), event.getPosition()).inflate(SEND_OUT_RADIUS);
            for (WanderingTrader trader : event.getLevel().getEntitiesOfClass(WanderingTrader.class, area)) {
                if (!trader.getPersistentData().hasUUID(WINNER_KEY) && !TraderPokemon.packOf(trader).isEmpty()) {
                    pendingChallenges.add(new Challenge(player, trader, pokemon.getUuid()));
                    return;
                }
            }
        });

        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            for (BattleActor loser : event.getLosers()) {
                if (!(loser instanceof TrainerBattleActor)) continue;
                WanderingTrader trader = findTrader(loser.getUuid());
                if (trader == null) continue;
                for (BattleActor winner : event.getWinners()) {
                    if (winner instanceof PlayerBattleActor && findPlayer(winner.getUuid()) instanceof ServerPlayer player) {
                        rewardWinner(trader, player);
                        return;
                    }
                }
            }
        });

        // Só quem venceu o mercador pode capturar o prêmio.
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(Priority.NORMAL, event -> {
            PokemonEntity pokemon = event.getPokemon();
            if (!pokemon.getPersistentData().hasUUID(WINNER_KEY)) return;
            Entity thrower = event.getPokeBall().getOwner();
            if (thrower == null || !thrower.getUUID().equals(pokemon.getPersistentData().getUUID(WINNER_KEY))) {
                event.cancel();
                if (thrower instanceof ServerPlayer player) say(player, "not_your_prize");
            }
        });

        // Capturado, vira um Mudbray comum com a manta: sem o apelido "do Mercador".
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            if (TraderPokemon.isTraderPokemon(event.getPokemon())) event.getPokemon().setNickname(null);
        });
    }

    private static void rewardWinner(WanderingTrader trader, ServerPlayer player) {
        trader.getPersistentData().putUUID(WINNER_KEY, player.getUUID());
        List<PokemonEntity> pack = TraderPokemon.packOf(trader);
        if (pack.isEmpty()) return;

        // Ainda escondido; aparece solto (sem guia) quando a batalha terminar.
        PokemonEntity prize = pack.get(0);
        UncatchableProperty.INSTANCE.catchable().apply(prize);
        prize.getPersistentData().putUUID(WINNER_KEY, player.getUUID());
        say(player, "victory");
    }

    private static WanderingTrader findTrader(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof WanderingTrader trader) return trader;
        }
        return null;
    }

    private static ServerPlayer findPlayer(UUID uuid) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayer(uuid);
    }

    /** Fala do mercador no chat. message é o fim da chave de tradução (cobblemon_ecosystems.trader.<message>). */
    private static void say(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("<").append(Component.translatable(TRADER_NAME)).append("> ")
                .withStyle(ChatFormatting.BLUE)
                .append(Component.translatable("cobblemon_ecosystems.trader." + message).withStyle(ChatFormatting.WHITE)));
    }
}
