package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Golett e Golurk no lugar dos golens de ferro: nascem com a vila e quando os aldeões chamam um golem.
 * Atacam mobs hostis (menos creepers, como o golem de ferro), não se afastam muito de casa e têm o ícone
 * da vila no nome.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class VillageGuardians {
    private static final float GOLURK_CHANCE = 0.4f;
    /** Os aldeões chamam golem com frequência; acima disso por perto, o chamado é ignorado. */
    private static final int MAX_NEARBY_GUARDIANS = 2;
    private static final double NEARBY_RADIUS = 32;
    private static final double HOME_RADIUS = 24;
    private static final double ENEMY_RANGE = 16;

    private static final String REPLACE_KEY = CobblemonEcosystems.MOD_ID + ":guardian_replace";
    private static final String HOME_KEY = CobblemonEcosystems.MOD_ID + ":guardian_home";

    private record Pending(ServerLevel level, Vec3 pos, float yaw, boolean summoned) {}
    private static final List<Pending> pending = new ArrayList<>();
    private static final Set<PokemonEntity> guardians = new HashSet<>();
    private static int tick;

    /** Marca o golem de ferro na hora em que a vila o cria ou um aldeão o chama. */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getEntity().getType() != EntityType.IRON_GOLEM) return;
        MobSpawnType reason = event.getSpawnType();
        if (reason == MobSpawnType.STRUCTURE || reason == MobSpawnType.MOB_SUMMONED) {
            event.getEntity().getPersistentData().putString(REPLACE_KEY, reason.name());
        }
    }

    /** Chamado por CobblemonEcosystems quando o golem bloqueado estava marcado. */
    static void replaceIfMarked(Entity golem, ServerLevel level) {
        String reason = golem.getPersistentData().getString(REPLACE_KEY);
        if (reason.isEmpty()) return;
        pending.add(new Pending(level, golem.position(), golem.getYRot(), reason.equals(MobSpawnType.MOB_SUMMONED.name())));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof PokemonEntity pokemon
                && pokemon.getPersistentData().contains(HOME_KEY)) {
            PokemonAi.markAsVillagePokemon(pokemon); // também corrige nomes antigos
            guardians.add(pokemon);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Pending p : List.copyOf(pending)) spawn(p);
        pending.clear();

        if (++tick % 20 != 0) return;
        guardians.removeIf(Entity::isRemoved);
        for (PokemonEntity guardian : guardians) {
            if (guardian.isBusy() || Hunts.isHunting(guardian)) continue;
            LivingEntity enemy = nearestEnemy(guardian);
            if (enemy != null) {
                Hunts.start(guardian, enemy, damage(guardian), 0.4, 20);
                continue;
            }
            BlockPos home = BlockPos.of(guardian.getPersistentData().getLong(HOME_KEY));
            if (!guardian.blockPosition().closerThan(home, HOME_RADIUS)) PokemonAi.walkTo(guardian, home, 0.6f, 4);
        }
    }

    /** Como o golem de ferro: qualquer mob hostil por perto, menos creepers (para não explodirem na vila). */
    private static LivingEntity nearestEnemy(PokemonEntity guardian) {
        return guardian.level().getEntitiesOfClass(Mob.class, guardian.getBoundingBox().inflate(ENEMY_RANGE),
                        mob -> mob instanceof Enemy && !(mob instanceof Creeper) && mob.isAlive() && guardian.hasLineOfSight(mob))
                .stream().min(Comparator.comparingDouble(guardian::distanceToSqr)).orElse(null);
    }

    /** O golem de ferro dá de 7 a 21; Golett bate menos, Golurk chega perto do golem. */
    private static float damage(PokemonEntity guardian) {
        int level = guardian.getPokemon().getLevel();
        return PokemonAi.species(guardian).equals("golurk") ? 8f + level / 6f : 4f + level / 8f;
    }

    private static void spawn(Pending p) {
        if (p.summoned() && countNear(p.level(), p.pos()) >= MAX_NEARBY_GUARDIANS) return;

        RandomSource random = p.level().random;
        boolean golurk = random.nextFloat() < GOLURK_CHANCE;
        int level = golurk ? 43 + random.nextInt(8) : 20 + random.nextInt(11);
        PokemonEntity pokemon = PokemonProperties.Companion
                .parse((golurk ? "golurk" : "golett") + " level=" + level + " uncatchable")
                .createEntity(p.level());
        pokemon.moveTo(p.pos().x, p.pos().y, p.pos().z, p.yaw(), 0f);
        pokemon.getPersistentData().putLong(HOME_KEY, BlockPos.containing(p.pos()).asLong());
        KeptPokemon.keep(pokemon);
        PokemonAi.markAsVillagePokemon(pokemon);
        p.level().addFreshEntity(pokemon);
    }

    private static int countNear(ServerLevel level, Vec3 pos) {
        int count = 0;
        for (PokemonEntity guardian : guardians) {
            if (!guardian.isRemoved() && guardian.level() == level && guardian.position().closerThan(pos, NEARBY_RADIUS)) count++;
        }
        return count;
    }
}
