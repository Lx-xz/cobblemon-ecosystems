package com.luiz.cobblemonecosystems;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.cobblemon.mod.common.CobblemonBlocks;
import com.cobblemon.mod.common.api.spawning.TimeRange;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.ai.RestBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pokémon do pasto: voltar para casa.
 * <ul>
 *   <li>Dentro da área do Cobblemon (32 blocos), mas fora do {@link Habitat}: a IA do Cobblemon continua mandando no
 *       passeio. O Pokémon só é chamado de volta para o habitat depois de um tempo fora, que depende do temperamento
 *       (caseiro, equilibrado ou explorador), e na hora de dormir da espécie todos voltam para casa (os noturnos, de dia).</li>
 *   <li>Fora da área do Cobblemon: o Cobblemon teleportaria o Pokémon ou mandaria para o PC. Aqui ele anda de volta, e
 *       nunca vai para o PC enquanto o bloco do pasto existir.</li>
 *   <li>Quem não acha o caminho de volta em 30 s <b>se perde</b>: fica triste, o dono recebe um aviso com onde ele está,
 *       e de tempos em tempos ele tenta voltar de novo. O jeito certo é ir buscar: chegando perto, ele segue o dono até
 *       em casa. (Com {@code pasture_get_lost} desligado, ele é teleportado para casa, como antes.)</li>
 * </ul>
 * Chamado pelo {@code PokemonEntityMixin} no lugar de {@code PokemonEntity.checkPastureTether()}, a cada tick.
 */
public final class PastureReturn {
    /** Tentando voltar por mais que isso (30 s), se perde. */
    private static final long GIVE_UP_TICKS = 600;
    /** Perdido, tenta voltar sozinho de novo a cada 5 minutos. */
    private static final long LOST_RETRY_TICKS = 6000;
    /** O dono a essa distância "encontra" o Pokémon perdido, que passa a segui-lo. */
    private static final double FOUND_DISTANCE = 6;
    /** De quantos em quantos ticks renova o caminho de volta. */
    private static final int REPATH_TICKS = 20;
    /** Tempo fora do habitat antes de voltar: caseiro 10 s, equilibrado 2 min, explorador 6 min. */
    private static final long HOMEBODY_AWAY = 200, BALANCED_AWAY = 2400, EXPLORER_AWAY = 7200;
    /** De quanto em quanto tempo cada Pokémon mostra as partículas do conforto. */
    private static final int PARTICLE_TICKS = 160;

    /** Pokémon fora do habitat → desde quando. Fracos: somem junto com a entidade. */
    private static final Map<PokemonEntity, Long> away = new WeakHashMap<>();
    /** Pokémon voltando para casa → para onde e desde quando. */
    private record Homeward(BlockPos target, long since) {}
    private static final Map<PokemonEntity, Homeward> homeward = new WeakHashMap<>();

    private PastureReturn() {}

    /** Devolve true quando cuidou do Pokémon (o Cobblemon não precisa fazer nada neste tick). */
    public static boolean handle(PokemonEntity pokemon) {
        if (pokemon.level().isClientSide() || !Config.PASTURE_RETURN.get()) return false;
        PokemonPastureBlockEntity.Tethering tethering = pokemon.getTethering();
        if (tethering == null || !(pokemon.level() instanceof ServerLevel level)) {
            forget(pokemon);
            return false;
        }
        boolean inArea = tethering.getBox().contains(pokemon.getX(), pokemon.getY(), pokemon.getZ());
        if (inArea && Habitat.enabled()) PastureFarms.tick(pokemon, level);
        if (inArea && !Habitat.enabled()) return false;

        BlockPos pasturePos = tethering.getPasturePos();
        BlockState pasture = level.getBlockState(pasturePos);
        // Sem o bloco do pasto, deixa o Cobblemon mandar para o PC, como sempre.
        if (!pasture.is(CobblemonBlocks.PASTURE)) return false;
        guide(pokemon, tethering, level, pasturePos, pasture, inArea);
        // Fora da área, o Cobblemon não faz nada (ele mandaria para o PC); dentro, deixa ele seguir normal.
        return !inArea;
    }

    private static void forget(PokemonEntity pokemon) {
        away.remove(pokemon);
        homeward.remove(pokemon);
    }

    /** Decide se o Pokémon precisa voltar para casa, se perdeu ou foi encontrado. */
    private static void guide(PokemonEntity pokemon, PokemonPastureBlockEntity.Tethering tethering, ServerLevel level,
                              BlockPos pasturePos, BlockState pasture, boolean inArea) {
        long now = level.getGameTime();
        if ((now + pokemon.getId()) % PARTICLE_TICKS == 0) showComfort(pokemon, level);
        if ((now + pokemon.getId()) % REPATH_TICKS != 0 || pokemon.isBusy() || pokemon.isPassenger()) return;

        Habitat.Survey survey = Habitat.enabled() ? Habitat.survey(level, pasturePos) : null;
        boolean home = survey == null || survey.isEmpty() ? inArea : survey.contains(pokemon.blockPosition());
        if (home) {
            away.remove(pokemon);
            homeward.remove(pokemon);
            if (isLost(pokemon)) found(pokemon, tethering, level);
            return;
        }

        // Perdido: se o dono chegou perto, segue o dono; senão tenta voltar sozinho de vez em quando.
        if (isLost(pokemon)) {
            Player owner = level.getPlayerByUUID(tethering.getPlayerId());
            if (owner != null && owner.distanceTo(pokemon) <= FOUND_DISTANCE) {
                homeward.remove(pokemon);
                PokemonAi.walkTo(pokemon, owner, 0.6f, 2);
                return;
            }
            long lostAt = pokemon.getPersistentData().getLong(Habitat.LOST_AT_KEY);
            if (!homeward.containsKey(pokemon) && (now - lostAt) % LOST_RETRY_TICKS >= REPATH_TICKS) return;
        }

        long awaySince = away.computeIfAbsent(pokemon, p -> now);
        Homeward going = homeward.get(pokemon);
        if (going == null) {
            boolean mustReturn = !inArea || isLost(pokemon) || bedtime(pokemon, level)
                    || now - awaySince >= awayLimit(Habitat.temperament(pokemon));
            if (!mustReturn) return;
            List<BlockPos> floor = survey == null ? List.of() : survey.floor();
            BlockPos target = floor.isEmpty() ? front(pasturePos, pasture) : floor.get(pokemon.getRandom().nextInt(floor.size()));
            going = new Homeward(target, now);
            homeward.put(pokemon, going);
        }
        if (now - going.since() < GIVE_UP_TICKS) {
            PokemonAi.walkTo(pokemon, going.target(), inArea ? 0.5f : 0.6f, 1);
            return;
        }
        // Não achou o caminho de casa.
        homeward.remove(pokemon);
        if (!Config.PASTURE_GET_LOST.get()) {
            away.remove(pokemon);
            teleportNear(pokemon, going.target());
        } else if (!isLost(pokemon)) {
            getLost(pokemon, tethering, level);
        }
    }

    public static boolean isLost(PokemonEntity pokemon) {
        return pokemon.getPersistentData().getBoolean(Habitat.LOST_KEY);
    }

    private static void getLost(PokemonEntity pokemon, PokemonPastureBlockEntity.Tethering tethering, ServerLevel level) {
        pokemon.getPersistentData().putBoolean(Habitat.LOST_KEY, true);
        pokemon.getPersistentData().putLong(Habitat.LOST_AT_KEY, level.getGameTime());
        PokemonAi.stop(pokemon);
        Player owner = level.getPlayerByUUID(tethering.getPlayerId());
        if (owner != null) {
            BlockPos pos = pokemon.blockPosition();
            owner.sendSystemMessage(Component.translatable(CobblemonEcosystems.MOD_ID + ".habitat.got_lost",
                    pokemon.getName(), pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.BLUE));
        }
    }

    private static void found(PokemonEntity pokemon, PokemonPastureBlockEntity.Tethering tethering, ServerLevel level) {
        pokemon.getPersistentData().remove(Habitat.LOST_KEY);
        level.sendParticles(ParticleTypes.HEART, pokemon.getX(), pokemon.getY() + pokemon.getBbHeight() + 0.3, pokemon.getZ(),
                4, 0.3, 0.2, 0.3, 0);
        UUID ownerId = tethering.getPlayerId();
        Player owner = level.getPlayerByUUID(ownerId);
        if (owner != null && owner.distanceTo(pokemon) < 32) {
            owner.displayClientMessage(Component.translatable(CobblemonEcosystems.MOD_ID + ".habitat.back_home", pokemon.getName())
                    .withStyle(ChatFormatting.GREEN), true);
        }
    }

    private static long awayLimit(Habitat.Temperament temperament) {
        return switch (temperament) {
            case HOMEBODY -> HOMEBODY_AWAY;
            case BALANCED -> BALANCED_AWAY;
            case EXPLORER -> EXPLORER_AWAY;
        };
    }

    /**
     * Hora de dormir desta espécie, pelos dados do próprio Cobblemon (behaviour.resting.times): os noturnos, como
     * Oddish e Hoothoot, dormem de dia. Quem dorme "a qualquer hora" (Zubat, Diglett) volta à noite, e quem não dorme
     * não é chamado para casa.
     */
    private static boolean bedtime(PokemonEntity pokemon, ServerLevel level) {
        RestBehaviour rest = pokemon.getBehaviour().getResting();
        if (!rest.getCanSleep()) return false;
        int time = (int) (level.getDayTime() % 24000);
        TimeRange times = rest.getTimes();
        boolean anyTime = times.contains(6000) && times.contains(18000);
        return anyTime ? time >= 12542 && time <= 23460 : times.contains(time);
    }

    /** Feliz: brilhos verdes. Desconfortável ou perdido: lágrimas. */
    private static void showComfort(PokemonEntity pokemon, ServerLevel level) {
        if (!Config.HABITAT_PARTICLES.get() || pokemon.isBusy() || !Habitat.enabled()) return;
        Habitat.Report report = Habitat.report(pokemon);
        if (report == null) return;
        double y = pokemon.getY() + pokemon.getBbHeight() + 0.2;
        if (report.comfort() == Habitat.Comfort.HAPPY && !report.lost()) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pokemon.getX(), y, pokemon.getZ(), 4, 0.3, 0.2, 0.3, 0);
        } else if (report.comfort() == Habitat.Comfort.UNCOMFORTABLE || report.lost()) {
            level.sendParticles(ParticleTypes.FALLING_WATER, pokemon.getX(), y - 0.3, pokemon.getZ(), 3,
                    pokemon.getBbWidth() / 3, 0.05, pokemon.getBbWidth() / 3, 0);
        }
    }

    /** O bloco na frente do pasto (para onde ele está virado). */
    private static BlockPos front(BlockPos pasturePos, BlockState pasture) {
        Direction facing = pasture.hasProperty(HorizontalDirectionalBlock.FACING)
                ? pasture.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return pasturePos.relative(facing);
    }

    private static boolean teleportNear(PokemonEntity pokemon, BlockPos front) {
        for (int i = 0; i < 16; i++) {
            BlockPos pos = i == 0 ? front : front.offset(pokemon.getRandom().nextInt(7) - 3, 0, pokemon.getRandom().nextInt(7) - 3);
            if (pokemon.randomTeleport(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, false)) {
                pokemon.getNavigation().stop();
                return true;
            }
        }
        return false;
    }
}
