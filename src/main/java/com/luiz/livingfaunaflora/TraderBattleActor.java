package com.luiz.livingfaunaflora;

import java.util.List;

import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;

import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.phys.Vec3;

/**
 * Treinador ligado à entidade do mercador. Sem uma entidade, o Cobblemon não sabe de onde
 * lançar os Pokémon de nenhum dos lados e a batalha fica parada depois do "Vai!".
 */
public class TraderBattleActor extends TrainerBattleActor implements EntityBackedBattleActor<WanderingTrader> {
    private final WanderingTrader trader;
    private final Vec3 initialPos;

    public TraderBattleActor(String name, WanderingTrader trader, List<BattlePokemon> team, int skill) {
        super(name, trader.getUUID(), team, new StrongBattleAI(skill));
        this.trader = trader;
        this.initialPos = trader.position();
    }

    @Override
    public WanderingTrader getEntity() {
        return trader;
    }

    @Override
    public Vec3 getInitialPos() {
        return initialPos;
    }
}
