package com.luiz.cobblemonecosystems;

import java.util.List;

import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Treinador ligado a uma entidade do Minecraft (o mercador, um aldeão). Sem uma entidade, o Cobblemon não sabe de
 * onde lançar os Pokémon de nenhum dos lados e a batalha fica parada depois do "Vai!".
 * O nome é uma chave de tradução: o Cobblemon traduz o nome do treinador na tela de batalha.
 */
public class MobTrainerActor extends TrainerBattleActor implements EntityBackedBattleActor<LivingEntity> {
    private final LivingEntity entity;
    private final Vec3 initialPos;

    public MobTrainerActor(String nameKey, LivingEntity entity, List<BattlePokemon> team, int skill) {
        super(nameKey, entity.getUUID(), team, new StrongBattleAI(skill));
        this.entity = entity;
        this.initialPos = entity.position();
    }

    @Override
    public LivingEntity getEntity() {
        return entity;
    }

    @Override
    public Vec3 getInitialPos() {
        return initialPos;
    }
}
