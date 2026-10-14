package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Traças viram Dwebble: o inseto que mora dentro de uma pedra combina com os blocos infestados.
 * - Toda traça que apareceria (bloco infestado quebrado, mundos antigos) vira um Dwebble selvagem e bravo.
 * - Bravo, ele ataca o jogador mais perto por um tempo, com os próprios golpes ({@link Hunts}). Depois se acalma.
 * - Machucado por um jogador, chama os outros: blocos infestados por perto se abrem e soltam mais Dwebble,
 *   como as traças fazem.
 * Os spawners de traça viram um ninho de Dwebble ({@link SpawnerNests}).
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class InfestedDwebble {
    private static final int MIN_LEVEL = 8, MAX_LEVEL = 20;
    private static final long ANGRY_TICKS = 600;
    private static final double TARGET_RANGE = 16;
    /** Área onde ele chama os outros (a mesma das traças). */
    private static final int CALL_XZ = 10, CALL_Y = 5;
    /** Entre um chamado e outro do mesmo Dwebble. */
    private static final long CALL_COOLDOWN = 40;

    /** No Dwebble: até quando está bravo e quando pode chamar os outros de novo. */
    private static final String ANGRY_UNTIL_KEY = CobblemonEcosystems.MOD_ID + ":dwebble_angry_until";
    private static final String NEXT_CALL_KEY = CobblemonEcosystems.MOD_ID + ":dwebble_next_call";

    private record Pending(ServerLevel level, Vec3 pos) {}
    private static final List<Pending> pending = new ArrayList<>();
    private static final Set<PokemonEntity> angry = new HashSet<>();
    private static int tick;

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity().getType() == EntityType.SILVERFISH && Config.SILVERFISH_DWEBBLE.get()) {
            event.setCanceled(true);
            // Não dá para criar entidades no meio do evento de entrada; fica para o fim do tick.
            pending.add(new Pending(level, event.getEntity().position()));
        } else if (event.getEntity() instanceof PokemonEntity pokemon && pokemon.getPersistentData().contains(ANGRY_UNTIL_KEY)) {
            angry.add(pokemon);
        }
    }

    /** Machucado por um jogador: fica bravo de novo e chama os outros escondidos nos blocos. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof PokemonEntity dwebble) || !dwebble.getPersistentData().contains(ANGRY_UNTIL_KEY)
                || !(event.getSource().getEntity() instanceof Player) || !(dwebble.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        dwebble.getPersistentData().putLong(ANGRY_UNTIL_KEY, now + ANGRY_TICKS);
        angry.add(dwebble);
        if (now < dwebble.getPersistentData().getLong(NEXT_CALL_KEY)) return;
        dwebble.getPersistentData().putLong(NEXT_CALL_KEY, now + CALL_COOLDOWN);
        callFriends(level, dwebble.blockPosition());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Pending p : List.copyOf(pending)) spawn(p.level(), p.pos());
        pending.clear();

        if (++tick % 20 != 0) return;
        angry.removeIf(dwebble -> {
            if (dwebble.isRemoved() || !PokemonAi.isWild(dwebble)) return true;
            if (dwebble.level().getGameTime() >= dwebble.getPersistentData().getLong(ANGRY_UNTIL_KEY)) return true; // acalmou
            if (dwebble.isBusy() || Hunts.isHunting(dwebble)) return false;
            Player target = nearestPlayer(dwebble);
            if (target != null) Hunts.start(dwebble, target, 2f + dwebble.getPokemon().getLevel() / 10f, 0.1, 20);
            return false;
        });
    }

    private static Player nearestPlayer(PokemonEntity dwebble) {
        return dwebble.level().players().stream()
                .filter(player -> !player.isCreative() && !player.isSpectator() && player.isAlive()
                        && player.distanceTo(dwebble) < TARGET_RANGE && dwebble.hasLineOfSight(player))
                .min(Comparator.comparingDouble(dwebble::distanceToSqr))
                .orElse(null);
    }

    static PokemonEntity spawn(ServerLevel level, Vec3 pos) {
        int pokemonLevel = MIN_LEVEL + level.random.nextInt(MAX_LEVEL - MIN_LEVEL + 1);
        PokemonEntity dwebble = PokemonProperties.Companion.parse("dwebble level=" + pokemonLevel).createEntity(level);
        dwebble.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360f, 0f);
        dwebble.getPersistentData().putLong(ANGRY_UNTIL_KEY, level.getGameTime() + ANGRY_TICKS);
        if (!level.addFreshEntity(dwebble)) return null;
        dwebble.spawnAnim();
        angry.add(dwebble);
        return dwebble;
    }

    /** Como as traças: os blocos infestados por perto se abrem, um a um, até a sorte parar (metade das vezes). */
    private static void callFriends(ServerLevel level, BlockPos center) {
        List<BlockPos> infested = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-CALL_XZ, -CALL_Y, -CALL_XZ), center.offset(CALL_XZ, CALL_Y, CALL_XZ))) {
            if (level.getBlockState(pos).getBlock() instanceof InfestedBlock) infested.add(pos.immutable());
        }
        Collections.shuffle(infested, new Random(level.random.nextLong()));
        boolean griefing = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        for (BlockPos pos : infested) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof InfestedBlock block)) continue;
            if (griefing) {
                // O bloco se abre (sem soltar item) e o Dwebble sai dele.
                level.destroyBlock(pos, false);
                pending.add(new Pending(level, Vec3.atBottomCenterOf(pos)));
            } else {
                // Sem mobGriefing o bloco fica inteiro (vira o normal) e o Dwebble aparece ao lado de quem chamou.
                level.setBlockAndUpdate(pos, block.hostStateByInfested(state));
                level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
                pending.add(new Pending(level, Vec3.atBottomCenterOf(center)));
            }
            if (level.random.nextBoolean()) return;
        }
    }
}
