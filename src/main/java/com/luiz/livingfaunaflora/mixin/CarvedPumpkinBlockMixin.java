package com.luiz.livingfaunaflora.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;

/**
 * Golem de ferro não pode mais ser montado com abóbora: os guardiões das vilas agora são Golett e Golurk.
 * A segunda busca de padrão (ordinal 1) é sempre a do golem de ferro; a primeira, do golem de neve, continua.
 */
@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
    /** Ao colocar a abóbora: não acha o padrão, então não gasta os blocos de ferro. */
    @Redirect(method = "trySpawnGolem", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/world/level/block/state/pattern/BlockPattern;find(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;"))
    private BlockPattern.BlockPatternMatch living_fauna_flora$noIronGolem(BlockPattern pattern, LevelReader level, BlockPos pos) {
        return null;
    }

    /** Ejetor (dispenser) também não coloca a abóbora achando que vai montar um golem de ferro. */
    @Redirect(method = "canSpawnGolem", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/world/level/block/state/pattern/BlockPattern;find(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;"))
    private BlockPattern.BlockPatternMatch living_fauna_flora$noIronGolemCheck(BlockPattern pattern, LevelReader level, BlockPos pos) {
        return null;
    }
}
