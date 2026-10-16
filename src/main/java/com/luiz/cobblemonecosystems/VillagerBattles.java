package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Batalhas contra aldeões que têm Pokémon ({@link VillagerCompanions}), no estilo da batalha do mercador.
 * - Para desafiar: agachar + clique direito no aldeão, ou lançar um Pokémon (tecla R) nele ou no Pokémon dele.
 * - Aldeões pacíficos e crianças recusam. Os outros aceitam, com força e time de acordo com a personalidade.
 * - Durante a batalha o aldeão fica parado e o Pokémon dele vai para a Pokébola (batalha uma cópia).
 * - Vencer dá desconto nas trocas (reputação); aldeões sem profissão pagam em esmeraldas.
 * - Depois, o Pokémon descansa na Pokébola por 2 dias. Se houver uma máquina de cura por perto, o aldeão leva o
 *   Pokémon até ela: ele volta na hora, mas o aldeão só aceita outra batalha 1 dia depois da anterior.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class VillagerBattles {
    private static final long REST_WITHOUT_HEALER = 48000;   // 2 dias
    private static final long REST_WITH_HEALER = 24000;      // 1 dia
    private static final int HEALER_RADIUS = 24;
    private static final double SEND_OUT_RADIUS = 2.5;
    private static final ResourceLocation HEALING_MACHINE = ResourceLocation.fromNamespaceAndPath("cobblemon", "healing_machine");

    private static final String COOLDOWN_UNTIL_KEY = CobblemonEcosystems.MOD_ID + ":battle_cooldown_until";
    private static final String LAST_BATTLE_KEY = CobblemonEcosystems.MOD_ID + ":last_battle";
    private static final String KEY = CobblemonEcosystems.MOD_ID + ".villager.";

    private record Challenge(ServerPlayer player, Villager villager, UUID leadingPokemon) {}
    private static final List<Challenge> pendingChallenges = new ArrayList<>();
    /** Aldeões batalhando agora → jogador. */
    private static final Map<Villager, ServerPlayer> activeBattles = new HashMap<>();
    /** Aldeões com o Pokémon descansando → onde está a máquina de cura (ou null) e quando procurar de novo. */
    private record Healer(BlockPos pos, long nextSearch) {}
    private static final Map<Villager, Healer> resting = new HashMap<>();
    private static int tick;

    // ----- Desafio -----

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!Config.VILLAGER_BATTLES.get() || !(event.getTarget() instanceof Villager villager)
                || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !villager.getPersistentData().hasUUID(VillagerCompanions.COMPANION_KEY)) return;
        // Agachado não abre a tela de trocas; o clique vira o desafio.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND) challenge(player, villager, null);
    }

    /** Eventos do Cobblemon; chamado no construtor do mod. */
    static void registerCobblemonEvents() {
        // R mirando no Pokémon do aldeão abriria uma batalha selvagem: vira desafio ao aldeão (ou recusa).
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL, event -> {
            Villager villager = null;
            ServerPlayer player = null;
            UUID leading = null;
            for (BattleActor actor : event.getBattle().getActors()) {
                if (actor instanceof PokemonBattleActor wild && wild.getEntity() != null
                        && wild.getEntity().getPersistentData().hasUUID(VillagerCompanions.VILLAGER_KEY)) {
                    Entity owner = ((ServerLevel) wild.getEntity().level())
                            .getEntity(wild.getEntity().getPersistentData().getUUID(VillagerCompanions.VILLAGER_KEY));
                    if (owner instanceof Villager v) villager = v;
                } else if (actor instanceof PlayerBattleActor playerActor) {
                    player = server() == null ? null : server().getPlayerList().getPlayer(playerActor.getUuid());
                    if (!playerActor.getPokemonList().isEmpty()) {
                        leading = playerActor.getPokemonList().get(0).getOriginalPokemon().getUuid();
                    }
                }
            }
            if (villager == null) return;
            // Pokémon de aldeão nunca entra em batalha selvagem.
            event.setReason(Component.translatable(KEY + "challenge_villager"));
            event.cancel();
            if (player != null && Config.VILLAGER_BATTLES.get()) pendingChallenges.add(new Challenge(player, villager, leading));
        });

        // R mirando no aldeão só solta o Pokémon perto dele: se cair perto, vira desafio.
        CobblemonEvents.POKEMON_SENT_POST.subscribe(Priority.NORMAL, event -> {
            if (!Config.VILLAGER_BATTLES.get()) return;
            ServerPlayer player = event.getPokemon().getOwnerPlayer();
            if (player == null || PlayerExtensionsKt.isInBattle(player)) return;
            AABB area = new AABB(event.getPosition(), event.getPosition()).inflate(SEND_OUT_RADIUS);
            for (Villager villager : event.getLevel().getEntitiesOfClass(Villager.class, area)) {
                if (villager.getPersistentData().hasUUID(VillagerCompanions.COMPANION_KEY)) {
                    pendingChallenges.add(new Challenge(player, villager, event.getPokemon().getUuid()));
                    return;
                }
            }
        });

        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            for (BattleActor loser : event.getLosers()) {
                if (loser instanceof TrainerBattleActor && findVillager(loser.getUuid()) instanceof Villager villager) {
                    for (BattleActor winner : event.getWinners()) {
                        if (winner instanceof PlayerBattleActor && server() != null
                                && server().getPlayerList().getPlayer(winner.getUuid()) instanceof ServerPlayer player) {
                            reward(villager, player);
                        }
                    }
                    return;
                }
            }
            for (BattleActor winner : event.getWinners()) {
                if (winner instanceof TrainerBattleActor && findVillager(winner.getUuid()) instanceof Villager villager) {
                    ServerPlayer player = activeBattles.get(villager);
                    if (player != null) say(player, villager, "lose");
                }
            }
        });
    }

    private static void challenge(ServerPlayer player, Villager villager, UUID leadingPokemon) {
        if (villager.isRemoved() || activeBattles.containsKey(villager) || PlayerExtensionsKt.isInBattle(player)) return;
        if (villager.isBaby()) {
            say(player, villager, "refuse_baby");
            return;
        }
        VillagerCompanions.Personality personality = VillagerCompanions.personalityOf(villager, villager.getRandom());
        if (!personality.battles()) {
            say(player, villager, "refuse_peaceful");
            return;
        }
        long now = villager.level().getDayTime();
        PokemonEntity pet = VillagerCompanions.companionOf(villager);
        if (pet == null || resting(villager, now)) {
            say(player, villager, "tired");
            return;
        }
        List<BattlePokemon> playerTeam = PlayerExtensionsKt.party(player).toBattleTeam(false, false, leadingPokemon);
        if (playerTeam.stream().allMatch(p -> p.getOriginalPokemon().isFainted())) {
            say(player, villager, "party_fainted");
            return;
        }

        // Pets de versões antigas ainda não têm o resto do time.
        if (!villager.getPersistentData().contains(VillagerCompanions.TEAM_KEY)) {
            VillagerCompanions.rollTeam(villager, personality, PokemonAi.species(pet), villager.getRandom());
        }
        List<BattlePokemon> villagerTeam = new ArrayList<>();
        villagerTeam.add(BattlePokemon.Companion.safeCopyOf(pet.getPokemon()));
        for (String properties : VillagerCompanions.teamOf(villager)) {
            villagerTeam.add(BattlePokemon.Companion.safeCopyOf(PokemonProperties.Companion.parse(properties).create()));
        }

        say(player, villager, "challenge_" + personality.name().toLowerCase());
        BattleRegistry.startBattle(
                BattleFormat.Companion.getGEN_9_SINGLES(),
                new BattleSide(new PlayerBattleActor(player.getUUID(), playerTeam)),
                new BattleSide(new MobTrainerActor(nameKey(villager), villager, villagerTeam, personality.battleSkill)),
                false);

        // O aldeão fica parado olhando para o jogador; o Pokémon dele vai para a Pokébola enquanto a cópia batalha.
        villager.getNavigation().stop();
        villager.setNoAi(true);
        villager.lookAt(player, 360f, 360f);
        VillagerCompanions.storePet(villager, pet);
        villager.getPersistentData().putLong(LAST_BATTLE_KEY, now);
        activeBattles.put(villager, player);
    }

    // ----- Recompensa -----

    /** Desconto nas trocas (reputação, como curar um aldeão zumbi); sem profissão, esmeraldas. */
    private static void reward(Villager villager, ServerPlayer player) {
        VillagerCompanions.Personality personality = VillagerCompanions.personalityOf(villager, villager.getRandom());
        int strength = personality.ordinal(); // FRIENDLY 1, COMPETITIVE 2, CHALLENGER 3
        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            int emeralds = 1 + strength * 2 + villager.getRandom().nextInt(2 + strength);
            ItemStack stack = new ItemStack(Items.EMERALD, emeralds);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            say(player, villager, "win_emeralds");
        } else {
            // Positiva maior é permanente (desconto acumula até o limite do jogo); a menor some aos poucos.
            villager.getGossips().add(player.getUUID(), GossipType.MAJOR_POSITIVE, 1 + strength);
            villager.getGossips().add(player.getUUID(), GossipType.MINOR_POSITIVE, 5 + strength * 5);
            say(player, villager, "win_discount");
        }
        ServerLevel level = (ServerLevel) villager.level();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 2, villager.getZ(), 8, 0.4, 0.3, 0.4, 0);
    }

    // ----- Fim da batalha, descanso e cura -----

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Challenge c : List.copyOf(pendingChallenges)) challenge(c.player(), c.villager(), c.leadingPokemon());
        pendingChallenges.clear();

        activeBattles.entrySet().removeIf(entry -> {
            Villager villager = entry.getKey();
            if (PlayerExtensionsKt.isInBattle(entry.getValue()) && !villager.isRemoved()) return false;
            villager.setNoAi(false);
            long now = villager.level().getDayTime();
            villager.getPersistentData().putLong(COOLDOWN_UNTIL_KEY, now + REST_WITHOUT_HEALER);
            resting.put(villager, new Healer(null, 0));
            return true;
        });

        if (++tick % 100 != 0) return;
        resting.entrySet().removeIf(entry -> rest(entry.getKey(), entry.getValue(), entry));
    }

    /** Um passo do descanso. Devolve true quando o Pokémon já voltou. */
    private static boolean rest(Villager villager, Healer healer, Map.Entry<Villager, Healer> entry) {
        if (villager.isRemoved()) return true;
        CompoundTag data = villager.getPersistentData();
        if (!data.contains(VillagerCompanions.STORED_PET_KEY)) return true;
        ServerLevel level = (ServerLevel) villager.level();
        long now = level.getDayTime();
        if (!resting(villager, now)) {
            VillagerCompanions.releasePet(villager);
            return true;
        }
        // Procura uma máquina de cura de vez em quando; de dia, o aldeão vai até ela.
        if (healer.pos() == null && now >= healer.nextSearch()) {
            entry.setValue(healer = new Healer(findHealer(level, villager.blockPosition()), now + 1200));
        }
        if (healer.pos() == null || !level.isDay() || villager.isSleeping()) return false;
        if (villager.blockPosition().closerThan(healer.pos(), 3)) {
            VillagerCompanions.releasePet(villager);
            // Curado: o Pokémon volta, mas outra batalha só 1 dia depois da anterior.
            data.putLong(COOLDOWN_UNTIL_KEY, data.getLong(LAST_BATTLE_KEY) + REST_WITH_HEALER);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, healer.pos().getX() + 0.5, healer.pos().getY() + 1, healer.pos().getZ() + 0.5,
                    10, 0.4, 0.4, 0.4, 0);
            level.playSound(null, healer.pos(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.4f, 1.6f);
            return true;
        }
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(healer.pos(), 0.5f, 1));
        return false;
    }

    /**
     * Ainda descansando? Os tempos usam o horário do dia (dormir e o /time contam). Se o horário voltou
     * (/time set), um descanso maior que o máximo conta como terminado, para o aldeão não ficar preso.
     */
    private static boolean resting(Villager villager, long now) {
        long until = villager.getPersistentData().getLong(COOLDOWN_UNTIL_KEY);
        return now < until && until - now <= REST_WITHOUT_HEALER;
    }

    /** Pet derrotado fora de batalha (por monstros): descansa como depois de uma batalha. */
    static void startRest(Villager villager) {
        long now = villager.level().getDayTime();
        villager.getPersistentData().putLong(LAST_BATTLE_KEY, now);
        villager.getPersistentData().putLong(COOLDOWN_UNTIL_KEY, now + REST_WITHOUT_HEALER);
        resting.put(villager, new Healer(null, 0));
    }

    private static BlockPos findHealer(ServerLevel level, BlockPos center) {
        Block machine = BuiltInRegistries.BLOCK.get(HEALING_MACHINE);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-HEALER_RADIUS, -6, -HEALER_RADIUS), center.offset(HEALER_RADIUS, 6, HEALER_RADIUS))) {
            if (level.getBlockState(pos).is(machine)) return pos.immutable();
        }
        return null;
    }

    /** Aldeões que já estavam descansando quando o mundo carregou também precisam terminar o descanso. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof Villager villager
                && villager.getPersistentData().contains(VillagerCompanions.STORED_PET_KEY)) {
            resting.putIfAbsent(villager, new Healer(null, 0));
        }
    }

    // ----- Utilidades -----

    /** Nome do aldeão na batalha e no chat: a profissão ("Armeiro"), traduzida pelo próprio Minecraft. */
    private static String nameKey(Villager villager) {
        VillagerProfession profession = villager.getVillagerData().getProfession();
        return profession == VillagerProfession.NONE ? "entity.minecraft.villager" : "entity.minecraft.villager." + profession.name();
    }

    private static void say(ServerPlayer player, Villager villager, String message) {
        player.sendSystemMessage(Component.literal("<").append(Component.translatable(nameKey(villager))).append("> ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.translatable(KEY + message).withStyle(ChatFormatting.WHITE)));
    }

    private static Villager findVillager(UUID uuid) {
        if (server() == null) return null;
        for (ServerLevel level : server().getAllLevels()) {
            if (level.getEntity(uuid) instanceof Villager villager) return villager;
        }
        return null;
    }

    private static MinecraftServer server() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
