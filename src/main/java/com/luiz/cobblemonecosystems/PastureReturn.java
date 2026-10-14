package com.luiz.cobblemonecosystems;

import java.util.Map;
import java.util.WeakHashMap;

import com.cobblemon.mod.common.CobblemonBlocks;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pokémon do pasto que saem da área voltam andando para o pasto.
 * O Cobblemon teleporta o Pokémon para a frente do bloco quando ele sai da área e, se não conseguir, manda ele para
 * o PC (por isso era preciso cercar o pasto). Aqui ele anda de volta; só é teleportado se não conseguir voltar a
 * tempo ou se estiver longe demais, e nunca vai para o PC enquanto o bloco do pasto existir.
 * Chamado pelo {@code PokemonEntityMixin} no lugar de {@code PokemonEntity.checkPastureTether()}, a cada tick.
 */
public final class PastureReturn {
    /** Tentando voltar por mais que isso (30 s), é teleportado. */
    private static final long GIVE_UP_TICKS = 600;
    /** Mais longe que isso da área (em blocos), é teleportado direto. */
    private static final double MAX_STRAY = 48;
    /** De quantos em quantos ticks renova o caminho de volta. */
    private static final int REPATH_TICKS = 20;

    /** Pokémon voltando → desde quando. Fraco: some junto com a entidade. */
    private static final Map<PokemonEntity, Long> returning = new WeakHashMap<>();

    private PastureReturn() {}

    /** Devolve true quando cuidou do Pokémon (o Cobblemon não precisa fazer nada neste tick). */
    public static boolean handle(PokemonEntity pokemon) {
        if (pokemon.level().isClientSide() || !Config.PASTURE_RETURN.get()) return false;
        PokemonPastureBlockEntity.Tethering tethering = pokemon.getTethering();
        if (tethering == null || tethering.getBox().contains(pokemon.getX(), pokemon.getY(), pokemon.getZ())) {
            returning.remove(pokemon);
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
