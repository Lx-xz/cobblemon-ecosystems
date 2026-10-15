package com.luiz.cobblemonecosystems;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.cobblemon.mod.common.CobblemonBlocks;
import com.cobblemon.mod.common.api.spawning.TimeRange;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.ai.RestBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pokémon do pasto: voltar para casa.
 * <ul>
 *   <li>Fora da área do Cobblemon (32 blocos): o Cobblemon teleporta o Pokémon e, se não conseguir, manda para o PC
 *       (por isso era preciso cercar). Aqui ele anda de volta; só é teleportado se não conseguir a tempo ou estiver
 *       longe demais, e nunca vai para o PC enquanto o bloco do pasto existir.</li>
 *   <li>Dentro da área, mas fora do {@link Habitat}: a IA do Cobblemon continua mandando no passeio. O Pokémon só é
 *       chamado de volta para o habitat depois de um tempo fora, que depende do temperamento (caseiro, equilibrado ou
 *       explorador), e na hora de dormir da espécie todos voltam para casa (os noturnos, de dia). Quem não consegue voltar é teleportado e fica triste
 *       por um tempo.</li>
 * </ul>
 * Chamado pelo {@code PokemonEntityMixin} no lugar de {@code PokemonEntity.checkPastureTether()}, a cada tick.
 */
public final class PastureReturn {
    /** Tentando voltar por mais que isso (30 s), é teleportado. */
    private static final long GIVE_UP_TICKS = 600;
    /** Mais longe que isso da área (em blocos), é teleportado direto. */
    private static final double MAX_STRAY = 48;
    /** De quantos em quantos ticks renova o caminho de volta. */
    private static final int REPATH_TICKS = 20;
    /** Tempo fora do habitat antes de voltar: caseiro 10 s, equilibrado 2 min, explorador 6 min. */
    private static final long HOMEBODY_AWAY = 200, BALANCED_AWAY = 2400, EXPLORER_AWAY = 7200;
    /** De quanto em quanto tempo cada Pokémon mostra as partículas do conforto. */
    private static final int PARTICLE_TICKS = 160;

    /** Pokémon voltando para a área do Cobblemon → desde quando. Fraco: some junto com a entidade. */
    private static final Map<PokemonEntity, Long> returning = new WeakHashMap<>();
    /** Pokémon fora do habitat → desde quando. */
    private static final Map<PokemonEntity, Long> away = new WeakHashMap<>();
    /** Pokémon voltando para o habitat → para onde e desde quando. */
    private record Homeward(BlockPos target, long since) {}
    private static final Map<PokemonEntity, Homeward> homeward = new WeakHashMap<>();

    private PastureReturn() {}

    /** Devolve true quando cuidou do Pokémon (o Cobblemon não precisa fazer nada neste tick). */
    public static boolean handle(PokemonEntity pokemon) {
        if (pokemon.level().isClientSide() || !Config.PASTURE_RETURN.get()) return false;
        PokemonPastureBlockEntity.Tethering tethering = pokemon.getTethering();
        if (tethering == null) {
            forget(pokemon);
            return false;
        }
        if (tethering.getBox().contains(pokemon.getX(), pokemon.getY(), pokemon.getZ())) {
            returning.remove(pokemon);
            if (Habitat.enabled() && pokemon.level() instanceof ServerLevel level) roam(pokemon, tethering, level);
            return false;
        }
        BlockPos pasturePos = tethering.getPasturePos();
        BlockState pasture = pokemon.level().getBlockState(pasturePos);
        // Sem o bloco do pasto, deixa o Cobblemon mandar para o PC, como sempre.
        if (!pasture.is(CobblemonBlocks.PASTURE)) return false;
        // Em batalha ou ocupado: espera terminar.
        if (pokemon.isBusy()) return true;

        long now = pokemon.level().getGameTime();
        long since = returning.computeIfAbsent(pokemon, p -> now);
        double stray = Math.sqrt(tethering.getBox().distanceToSqr(pokemon.position()));
        BlockPos front = front(pasturePos, pasture);
        if (now - since < GIVE_UP_TICKS && stray <= MAX_STRAY) {
            if ((now - since) % REPATH_TICKS == 0) PokemonAi.walkTo(pokemon, front, 0.6f, 1);
            return true;
        }
        // Não conseguiu voltar: teleporta para a frente do pasto (ou em volta dele) e tenta de novo depois.
        returning.remove(pokemon);
        if (!teleportNear(pokemon, front)) returning.put(pokemon, now);
        return true;
    }

    private static void forget(PokemonEntity pokemon) {
        returning.remove(pokemon);
        away.remove(pokemon);
        homeward.remove(pokemon);
    }

    /** Dentro da área do Cobblemon: partículas do conforto e a volta para o habitat. */
    private static void roam(PokemonEntity pokemon, PokemonPastureBlockEntity.Tethering tethering, ServerLevel level) {
        long now = level.getGameTime();
        if ((now + pokemon.getId()) % PARTICLE_TICKS == 0) showComfort(pokemon, level);
        if ((now + pokemon.getId()) % REPATH_TICKS != 0 || pokemon.isBusy() || pokemon.isPassenger()) return;

        BlockPos pasturePos = tethering.getPasturePos();
        Habitat.Survey survey = Habitat.survey(level, pasturePos);
        // Sem espaço em volta do bloco não há habitat para voltar.
        if (survey.isEmpty() || survey.contains(pokemon.blockPosition())) {
            away.remove(pokemon);
            homeward.remove(pokemon);
            return;
        }

        long awaySince = away.computeIfAbsent(pokemon, p -> now);
        Homeward going = homeward.get(pokemon);
        if (going == null) {
            if (!bedtime(pokemon, level) && now - awaySince < awayLimit(Habitat.temperament(pokemon))) return;
            List<BlockPos> floor = survey.floor();
            going = new Homeward(floor.get(pokemon.getRandom().nextInt(floor.size())), now);
            homeward.put(pokemon, going);
        }
        if (now - going.since() < GIVE_UP_TICKS) {
            PokemonAi.walkTo(pokemon, going.target(), 0.5f, 1);
            return;
        }
        // Não achou o caminho de casa: volta por teleporte, mas fica triste por um tempo.
        homeward.remove(pokemon);
        away.remove(pokemon);
        if (teleportNear(pokemon, going.target())) pokemon.getPersistentData().putLong(Habitat.LOST_AT_KEY, now);
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
        if (!Config.HABITAT_PARTICLES.get() || pokemon.isBusy()) return;
        Habitat.Report report = Habitat.report(pokemon);
        if (report == null) return;
        double y = pokemon.getY() + pokemon.getBbHeight() + 0.2;
        if (report.comfort() == Habitat.Comfort.HAPPY) {
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
