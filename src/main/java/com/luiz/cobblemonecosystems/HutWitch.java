package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * A bruxa da cabana do pântano e o Purrloin preto dela (o gato da cabana, que {@link StructurePokemon} troca).
 * - Só a bruxa que nasce com a cabana; as que aparecem depois (farm de bruxa) continuam normais.
 * - Ela é neutra: só ataca quem a atacou, por um tempo.
 * - O Purrloin segue a bruxa, não leva dano de jogadores e não pode ser capturado.
 * - Para ganhar o Purrloin: batalhar com a bruxa (agachar + clique direito, ou lançar um Pokémon nela ou no gato).
 *   Vencer libera a captura, só para quem venceu; a bruxa fica sem gato e só adota outro 3 dias depois.
 * - Se a bruxa morrer, o Purrloin foge e some.
 *
 * Para testar: /summon witch ~ ~ ~ {NeoForgeData:{"cobblemon_ecosystems:hut_witch":1b}}
 * (sem um Purrloin da bruxa por perto, ela conjura um depois de 1 minuto).
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class HutWitch {
    /** Nome na batalha e no chat, traduzido pelo próprio Minecraft. */
    private static final String NAME = "entity.minecraft.witch";
    private static final String KEY = CobblemonEcosystems.MOD_ID + ".witch.";
    private static final int AI_SKILL = 3;
    private static final double SEND_OUT_RADIUS = 2.5;
    /** O resto do time da bruxa (um deles, na Pokébola): Pokémon sombrios, de bruxaria. */
    private static final List<String> EXTRA_TEAM = List.of("murkrow", "misdreavus", "gastly");

    private static final double CLAIM_RADIUS = 16;
    private static final double FOLLOW_DISTANCE = 6;
    private static final double TELEPORT_DISTANCE = 16;
    /** Sem gato por perto por esse tempo (1 minuto), a bruxa conjura um. */
    private static final long CONJURE_AFTER = 1200;
    /** Gato sumido (chunk descarregado, /kill) por esse tempo (5 minutos): a bruxa esquece dele. */
    private static final long FORGET_PET_AFTER = 6000;
    /** Depois de perder o gato numa batalha, 3 dias até adotar outro (horário do dia: dormir conta). */
    private static final long READOPT_AFTER = 72000;
    /** Atacada, fica brava com quem a atacou por 2 minutos. */
    private static final long ANGRY_TICKS = 2400;
    /** Com a bruxa morta, o gato corre por 8 segundos e some. */
    private static final long FLEE_TICKS = 160;

    static final String HUT_KEY = CobblemonEcosystems.MOD_ID + ":hut_witch";
    /** Na bruxa: o gato, quando ele foi visto, desde quando está sem gato e quando pode adotar de novo. */
    private static final String PET_KEY = CobblemonEcosystems.MOD_ID + ":witch_pet";
    private static final String PET_SEEN_KEY = CobblemonEcosystems.MOD_ID + ":witch_pet_seen";
    private static final String NO_PET_SINCE_KEY = CobblemonEcosystems.MOD_ID + ":witch_no_pet_since";
    private static final String READOPT_AT_KEY = CobblemonEcosystems.MOD_ID + ":witch_readopt_at";
    /** Na bruxa: quem a atacou e até quando ela fica brava. */
    private static final String ANGRY_AT_KEY = CobblemonEcosystems.MOD_ID + ":witch_angry_at";
    private static final String ANGRY_UNTIL_KEY = CobblemonEcosystems.MOD_ID + ":witch_angry_until";
    /** No gato: a bruxa dona dele; quem venceu a bruxa (o único que pode capturá-lo); se está fugindo. */
    private static final String OWNER_KEY = CobblemonEcosystems.MOD_ID + ":witch_owner";
    private static final String WINNER_KEY = CobblemonEcosystems.MOD_ID + ":witch_winner";
    private static final String FLEEING_KEY = CobblemonEcosystems.MOD_ID + ":witch_fleeing";

    private record Challenge(ServerPlayer player, Witch witch, UUID leadingPokemon) {}
    private static final List<Challenge> pendingChallenges = new ArrayList<>();
    private record ActiveBattle(ServerPlayer player, PokemonEntity pet) {}
    private static final Map<Witch, ActiveBattle> activeBattles = new HashMap<>();

    private static final Set<Witch> witches = new HashSet<>();
    /** Gatos fugindo → hora de sumir. */
    private static final Map<PokemonEntity, Long> fleeing = new HashMap<>();
    private static int tick;

    // ----- Entradas -----

    /** Marca a bruxa que a cabana cria junto com ela (no mesmo lugar onde o gato nasce preto). */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity().getType() == EntityType.WITCH && event.getSpawnType() == MobSpawnType.STRUCTURE
                && event.getLevel().getLevel().structureManager()
                        .getStructureWithPieceAt(event.getEntity().blockPosition(), StructureTags.CATS_SPAWN_AS_BLACK).isValid()) {
            event.getEntity().getPersistentData().putBoolean(HUT_KEY, true);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if (event.getEntity() instanceof Witch witch && isHutWitch(witch)) {
            witches.add(witch);
        } else if (event.getEntity() instanceof PokemonEntity pokemon && pokemon.getPersistentData().getBoolean(FLEEING_KEY)) {
            // Fugiu e o chunk descarregou antes de sumir: some agora.
            event.setCanceled(true);
        }
    }

    private static boolean isHutWitch(Entity entity) {
        return entity instanceof Witch && entity.getPersistentData().getBoolean(HUT_KEY);
    }

    // ----- Bruxa neutra e gato protegido -----

    /** A bruxa da cabana só mira um jogador se ele a atacou há pouco. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (Config.WITCH_PET.get() && isHutWitch(event.getEntity()) && event.getNewAboutToBeSetTarget() instanceof Player player
                && !isAngryAt((Witch) event.getEntity(), player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (isHutWitch(event.getEntity()) && attacker instanceof Player player) {
            CompoundTag data = event.getEntity().getPersistentData();
            data.putUUID(ANGRY_AT_KEY, player.getUUID());
            data.putLong(ANGRY_UNTIL_KEY, event.getEntity().level().getGameTime() + ANGRY_TICKS);
        } else if (event.getEntity() instanceof PokemonEntity pokemon && pokemon.getPersistentData().hasUUID(OWNER_KEY)
                && (attacker instanceof Player || isHutWitch(attacker) || event.getSource().is(DamageTypes.IN_WALL))) {
            // Para ganhar o gato, batalhe com a bruxa. As poções dela também não acertam o próprio gato.
            event.setCanceled(true);
        }
    }

    private static boolean isAngryAt(Witch witch, Player player) {
        CompoundTag data = witch.getPersistentData();
        return data.hasUUID(ANGRY_AT_KEY) && data.getUUID(ANGRY_AT_KEY).equals(player.getUUID())
                && witch.level().getGameTime() < data.getLong(ANGRY_UNTIL_KEY);
    }

    /** Bruxa morta: o gato foge e some. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Witch witch) || !isHutWitch(witch) || !(witch.level() instanceof ServerLevel)) return;
        PokemonEntity pet = petOf(witch);
        if (pet == null) return;
        pet.getPersistentData().remove(OWNER_KEY);
        pet.getPersistentData().putBoolean(FLEEING_KEY, true);
        fleeing.put(pet, witch.level().getGameTime() + FLEE_TICKS);
    }

    // ----- Rotina -----

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Challenge c : List.copyOf(pendingChallenges)) challenge(c.player(), c.witch(), c.leadingPokemon());
        pendingChallenges.clear();

        activeBattles.entrySet().removeIf(entry -> {
            Witch witch = entry.getKey();
            ActiveBattle battle = entry.getValue();
            if (PlayerExtensionsKt.isInBattle(battle.player()) && !witch.isRemoved()) return false;
            witch.setNoAi(false);
            if (!battle.pet().isRemoved()) {
                battle.pet().setInvisible(false);
                battle.pet().setNoAi(false);
            }
            return true;
        });

        if (++tick % 5 == 0) flee();
        if (tick % 40 != 0) return;
        witches.removeIf(Entity::isRemoved);
        if (!Config.WITCH_PET.get()) return;
        for (Witch witch : witches) {
            if (!activeBattles.containsKey(witch)) care(witch);
        }
    }

    /** A bruxa e o gato: segue a bruxa; sem gato, ela adota um que esteja por perto ou conjura um novo. */
    private static void care(Witch witch) {
        ServerLevel level = (ServerLevel) witch.level();
        CompoundTag data = witch.getPersistentData();
        long now = level.getGameTime();
        PokemonEntity pet = petOf(witch);
        if (pet != null) {
            data.putLong(PET_SEEN_KEY, now);
            if (pet.isBusy()) return;
            double distance = pet.distanceTo(witch);
            if (distance > TELEPORT_DISTANCE) pet.teleportTo(witch.getX(), witch.getY(), witch.getZ());
            else if (distance > FOLLOW_DISTANCE) PokemonAi.walkTo(pet, witch, 0.5f, 2);
            return;
        }
        if (data.hasUUID(PET_KEY)) {
            // O gato pode estar num chunk descarregado; só esquece depois de um tempo.
            if (now - data.getLong(PET_SEEN_KEY) < FORGET_PET_AFTER && data.getLong(PET_SEEN_KEY) <= now) return;
            data.remove(PET_KEY);
        }
        if (waitingToReadopt(witch)) return;
        if (!data.contains(NO_PET_SINCE_KEY) || data.getLong(NO_PET_SINCE_KEY) > now) data.putLong(NO_PET_SINCE_KEY, now);

        PokemonEntity stray = level.getEntitiesOfClass(PokemonEntity.class, witch.getBoundingBox().inflate(CLAIM_RADIUS),
                        cat -> cat.isAlive() && PokemonAi.isWild(cat) && isWitchCat(cat) && !cat.getPersistentData().hasUUID(OWNER_KEY)
                                && !cat.getPersistentData().hasUUID(WINNER_KEY) && !cat.getPersistentData().getBoolean(FLEEING_KEY))
                .stream().findFirst().orElse(null);
        if (stray != null) {
            claim(witch, stray);
        } else if (now - data.getLong(NO_PET_SINCE_KEY) >= CONJURE_AFTER) {
            conjure(witch, level);
        }
    }

    private static boolean isWitchCat(PokemonEntity pokemon) {
        return pokemon.getPokemon().getForcedAspects().contains(StructurePokemon.WITCH_ASPECT);
    }

    /** Ainda esperando para adotar depois de perder o gato? Se o horário voltou (/time set), conta como terminado. */
    private static boolean waitingToReadopt(Witch witch) {
        CompoundTag data = witch.getPersistentData();
        if (!data.contains(READOPT_AT_KEY)) return false;
        long now = witch.level().getDayTime();
        long at = data.getLong(READOPT_AT_KEY);
        if (now < at && at - now <= READOPT_AFTER) return true;
        data.remove(READOPT_AT_KEY);
        return false;
    }

    private static void claim(Witch witch, PokemonEntity pet) {
        UncatchableProperty.INSTANCE.uncatchable().apply(pet);
        pet.getPersistentData().putUUID(OWNER_KEY, witch.getUUID());
        KeptPokemon.keep(pet);
        witch.getPersistentData().putUUID(PET_KEY, pet.getUUID());
        witch.getPersistentData().putLong(PET_SEEN_KEY, witch.level().getGameTime());
        witch.getPersistentData().remove(NO_PET_SINCE_KEY);
    }

    /** Um Purrloin preto aparece numa nuvem de fumaça ao lado da bruxa. */
    private static void conjure(Witch witch, ServerLevel level) {
        RandomSource random = level.random;
        PokemonEntity pet = PokemonProperties.Companion.parse("purrloin level=" + (10 + random.nextInt(11)) + " uncatchable").createEntity(level);
        pet.getPokemon().setForcedAspects(new HashSet<>(Set.of(StructurePokemon.WITCH_ASPECT)));
        pet.getPokemon().updateAspects();
        pet.moveTo(witch.getX(), witch.getY(), witch.getZ(), witch.getYRot(), 0f);
        if (!level.addFreshEntity(pet)) return;
        claim(witch, pet);
        level.sendParticles(ParticleTypes.WITCH, pet.getX(), pet.getY() + 0.5, pet.getZ(), 20, 0.4, 0.4, 0.4, 0);
        level.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.WITCH_CELEBRATE, SoundSource.HOSTILE, 1f, 1f);
    }

    /** O gato da bruxa morta corre para longe dos jogadores e some numa nuvem de fumaça. */
    private static void flee() {
        fleeing.entrySet().removeIf(entry -> {
            PokemonEntity pet = entry.getKey();
            if (pet.isRemoved()) return true;
            ServerLevel level = (ServerLevel) pet.level();
            if (level.getGameTime() >= entry.getValue()) {
                level.sendParticles(ParticleTypes.POOF, pet.getX(), pet.getY() + 0.5, pet.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
                pet.discard();
                return true;
            }
            if (pet.isBusy()) return false;
            Player player = level.getNearestPlayer(pet, 24);
            Vec3 away = player == null ? DefaultRandomPos.getPos(pet, 16, 7) : DefaultRandomPos.getPosAway(pet, 16, 7, player.position());
            if (away != null) PokemonAi.walkTo(pet, away, 1.0f, 0);
            return false;
        });
    }

    /** O gato da bruxa, se estiver carregado no mundo. */
    private static PokemonEntity petOf(Witch witch) {
        if (!witch.getPersistentData().hasUUID(PET_KEY)) return null;
        Entity entity = ((ServerLevel) witch.level()).getEntity(witch.getPersistentData().getUUID(PET_KEY));
        return entity instanceof PokemonEntity pet && pet.isAlive() ? pet : null;
    }

    // ----- Batalha -----

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!Config.WITCH_BATTLE.get() || !(event.getTarget() instanceof Witch witch) || !isHutWitch(witch)
                || !(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND) challenge(player, witch, null);
    }

    /** Eventos do Cobblemon; chamado no construtor do mod. */
    static void registerCobblemonEvents() {
        // R mirando no gato da bruxa abriria uma batalha selvagem: vira desafio à bruxa.
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL, event -> {
            Witch witch = null;
            ServerPlayer player = null;
            UUID leading = null;
            for (BattleActor actor : event.getBattle().getActors()) {
                if (actor instanceof PokemonBattleActor wild && wild.getEntity() != null
                        && wild.getEntity().getPersistentData().hasUUID(OWNER_KEY)) {
                    if (findWitch(wild.getEntity().getPersistentData().getUUID(OWNER_KEY)) instanceof Witch w) witch = w;
                } else if (actor instanceof PlayerBattleActor playerActor) {
                    player = server() == null ? null : server().getPlayerList().getPlayer(playerActor.getUuid());
                    if (!playerActor.getPokemonList().isEmpty()) {
                        leading = playerActor.getPokemonList().get(0).getOriginalPokemon().getUuid();
                    }
                }
            }
            if (witch == null) return;
            event.setReason(Component.translatable(KEY + "challenge_witch"));
            event.cancel();
            if (player != null && Config.WITCH_BATTLE.get()) pendingChallenges.add(new Challenge(player, witch, leading));
        });

        // R mirando na bruxa só solta o Pokémon perto dela: se cair perto, vira desafio.
        CobblemonEvents.POKEMON_SENT_POST.subscribe(Priority.NORMAL, event -> {
            if (!Config.WITCH_BATTLE.get()) return;
            ServerPlayer player = event.getPokemon().getOwnerPlayer();
            if (player == null || PlayerExtensionsKt.isInBattle(player)) return;
            AABB area = new AABB(event.getPosition(), event.getPosition()).inflate(SEND_OUT_RADIUS);
            for (Witch witch : event.getLevel().getEntitiesOfClass(Witch.class, area)) {
                if (isHutWitch(witch) && witch.getPersistentData().hasUUID(PET_KEY)) {
                    pendingChallenges.add(new Challenge(player, witch, event.getPokemon().getUuid()));
                    return;
                }
            }
        });

        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            for (BattleActor loser : event.getLosers()) {
                if (loser instanceof TrainerBattleActor && findWitch(loser.getUuid()) instanceof Witch witch) {
                    for (BattleActor winner : event.getWinners()) {
                        if (winner instanceof PlayerBattleActor && server() != null
                                && server().getPlayerList().getPlayer(winner.getUuid()) instanceof ServerPlayer player) {
                            reward(witch, player);
                            return;
                        }
                    }
                }
            }
            for (BattleActor winner : event.getWinners()) {
                if (winner instanceof TrainerBattleActor && findWitch(winner.getUuid()) instanceof Witch witch
                        && activeBattles.get(witch) instanceof ActiveBattle battle) {
                    say(battle.player(), "lose");
                }
            }
        });

        // Só quem venceu a bruxa pode capturar o gato.
        CobblemonEvents.THROWN_POKEBALL_HIT.subscribe(Priority.NORMAL, event -> {
            PokemonEntity pokemon = event.getPokemon();
            if (!pokemon.getPersistentData().hasUUID(WINNER_KEY)) return;
            Entity thrower = event.getPokeBall().getOwner();
            if (thrower == null || !thrower.getUUID().equals(pokemon.getPersistentData().getUUID(WINNER_KEY))) {
                event.cancel();
                if (thrower instanceof ServerPlayer player) say(player, "not_your_prize");
            }
        });
    }

    private static void challenge(ServerPlayer player, Witch witch, UUID leadingPokemon) {
        if (witch.isRemoved() || activeBattles.containsKey(witch) || PlayerExtensionsKt.isInBattle(player)) return;
        if (isAngryAt(witch, player)) {
            say(player, "angry");
            return;
        }
        PokemonEntity pet = petOf(witch);
        if (pet == null) {
            say(player, "no_pet");
            return;
        }
        List<BattlePokemon> playerTeam = PlayerExtensionsKt.party(player).toBattleTeam(false, false, leadingPokemon);
        if (playerTeam.stream().allMatch(p -> p.getOriginalPokemon().isFainted())) {
            say(player, "party_fainted");
            return;
        }

        // Cópias: o gato de verdade continua inteiro, ganhe ou perca. Mais um Pokémon sombrio na Pokébola.
        RandomSource random = witch.getRandom();
        int level = pet.getPokemon().getLevel();
        List<BattlePokemon> witchTeam = new ArrayList<>();
        witchTeam.add(BattlePokemon.Companion.safeCopyOf(pet.getPokemon()));
        String extra = EXTRA_TEAM.get(random.nextInt(EXTRA_TEAM.size()));
        witchTeam.add(BattlePokemon.Companion.safeCopyOf(
                PokemonProperties.Companion.parse(extra + " level=" + Math.max(5, level - 2 + random.nextInt(5))).create()));

        say(player, "challenge");
        BattleRegistry.startBattle(
                BattleFormat.Companion.getGEN_9_SINGLES(),
                new BattleSide(new PlayerBattleActor(player.getUUID(), playerTeam)),
                new BattleSide(new MobTrainerActor(NAME, witch, witchTeam, AI_SKILL)),
                false);

        // A bruxa fica parada olhando para o jogador; o gato de verdade some enquanto a cópia batalha.
        witch.getNavigation().stop();
        witch.setTarget(null);
        witch.setNoAi(true);
        witch.lookAt(player, 360f, 360f);
        pet.setNoAi(true);
        pet.setInvisible(true);
        activeBattles.put(witch, new ActiveBattle(player, pet));
    }

    /** O gato deixa de ser da bruxa e só quem venceu pode capturá-lo; ela só adota outro dias depois. */
    private static void reward(Witch witch, ServerPlayer player) {
        PokemonEntity pet = petOf(witch);
        CompoundTag data = witch.getPersistentData();
        data.remove(PET_KEY);
        data.remove(NO_PET_SINCE_KEY);
        data.putLong(READOPT_AT_KEY, witch.level().getDayTime() + READOPT_AFTER);
        if (pet != null) {
            pet.getPersistentData().remove(OWNER_KEY);
            pet.getPersistentData().putUUID(WINNER_KEY, player.getUUID());
            UncatchableProperty.INSTANCE.catchable().apply(pet);
        }
        say(player, "victory");
    }

    // ----- Utilidades -----

    private static void say(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("<").append(Component.translatable(NAME)).append("> ")
                .withStyle(ChatFormatting.DARK_PURPLE)
                .append(Component.translatable(KEY + message).withStyle(ChatFormatting.WHITE)));
    }

    private static Witch findWitch(UUID uuid) {
        if (server() == null) return null;
        for (ServerLevel level : server().getAllLevels()) {
            if (level.getEntity(uuid) instanceof Witch witch && isHutWitch(witch)) return witch;
        }
        return null;
    }

    private static MinecraftServer server() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
