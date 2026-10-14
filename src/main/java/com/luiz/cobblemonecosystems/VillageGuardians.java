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
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Golett e Golurk no lugar dos golens de ferro: nascem com a vila e quando os aldeões chamam um golem.
 *
 * Para testar: /summon iron_golem ~ ~ ~ {NeoForgeData:{"cobblemon_ecosystems:guardian_replace":"STRUCTURE","cobblemon_ecosystems:guardian_species":"golurk"}}
 * Atacam mobs hostis (menos creepers, como o golem de ferro), não se afastam muito de casa e têm o ícone
 * da vila no nome.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class VillageGuardians {
    private static final float GOLURK_CHANCE = 0.4f;
    /** Os aldeões chamam golem com frequência. Limite por vila: 1 guardião a cada 5 aldeões, entre 2 e 4. */
    private static final int VILLAGERS_PER_GUARDIAN = 5;
    private static final int MIN_GUARDIANS = 2, MAX_GUARDIANS = 4;
    /** Parte do dano de monstros que os guardiões recebem (o golem de ferro tem 100 de vida; Pokémon têm menos). */
    private static final float MONSTER_DAMAGE_TAKEN = 0.4f;
    private static final double NEARBY_RADIUS = 32;
    private static final double HOME_RADIUS = 24;
    private static final double ENEMY_RANGE = 16;

    private static final String REPLACE_KEY = CobblemonEcosystems.MOD_ID + ":guardian_replace";
    private static final String HOME_KEY = CobblemonEcosystems.MOD_ID + ":guardian_home";
    /** No golem marcado (por comando): força a espécie, "golett" ou "golurk". */
    private static final String SPECIES_KEY = CobblemonEcosystems.MOD_ID + ":guardian_species";
    /** Valor de REPLACE_KEY do golem da jaula dos postos de saqueadores ({@link OutpostCaptives}). */
    private static final String CAGED = "CAGED";

    private record Pending(ServerLevel level, Vec3 pos, float yaw, boolean summoned, boolean caged, String species) {}
    private static final List<Pending> pending = new ArrayList<>();
    private static final Set<PokemonEntity> guardians = new HashSet<>();
    private static int tick;

    /** Marca o golem de ferro na hora em que a vila o cria ou um aldeão o chama. */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (!Config.VILLAGE_GUARDIANS.get() || event.getEntity().getType() != EntityType.IRON_GOLEM) return;
        MobSpawnType reason = event.getSpawnType();
        if (reason == MobSpawnType.STRUCTURE && Config.OUTPOST_CAPTIVES.get()
                && OutpostCaptives.isInOutpost(event.getLevel(), event.getEntity().blockPosition())) {
            // O golem preso na jaula do posto de saqueadores: vira um Pokémon preso, não um guardião.
            event.getEntity().getPersistentData().putString(REPLACE_KEY, CAGED);
        } else if (reason == MobSpawnType.STRUCTURE || reason == MobSpawnType.MOB_SUMMONED) {
            event.getEntity().getPersistentData().putString(REPLACE_KEY, reason.name());
        }
    }

    /** Chamado por CobblemonEcosystems quando o golem bloqueado estava marcado. */
    static boolean isMarked(Entity entity) {
        return entity.getPersistentData().contains(REPLACE_KEY);
    }

    static void replaceIfMarked(Entity golem, ServerLevel level) {
        String reason = golem.getPersistentData().getString(REPLACE_KEY);
        if (reason.isEmpty()) return;
        pending.add(new Pending(level, golem.position(), golem.getYRot(), reason.equals(MobSpawnType.MOB_SUMMONED.name()),
                reason.equals(CAGED), golem.getPersistentData().getString(SPECIES_KEY)));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof PokemonEntity pokemon
                && pokemon.getPersistentData().contains(HOME_KEY)) {
            PokemonAi.markAsVillagePokemon(pokemon); // também corrige nomes antigos
            guardians.add(pokemon);
        }
    }

    /** Guardiões aguentam mais os monstros, como o golem de ferro aguenta. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof PokemonEntity pokemon && pokemon.getPersistentData().contains(HOME_KEY)
                && event.getSource().getEntity() instanceof Enemy) {
            event.setAmount(event.getAmount() * MONSTER_DAMAGE_TAKEN);
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
                boolean golurk = PokemonAi.species(guardian).equals("golurk");
                Hunts.start(guardian, enemy, damage(guardian), golurk ? 0.5 : 0.4, golurk ? 25 : 20);
                continue;
            }
            // Fora da luta, se recupera aos poucos.
            if (guardian.getHealth() < guardian.getMaxHealth()) guardian.heal(1f);
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

    /** Dano mínimo de cada golpe (o golpe do moveset pode dar mais). O golem de ferro dá de 7 a 21. */
    private static float damage(PokemonEntity guardian) {
        int level = guardian.getPokemon().getLevel();
        return PokemonAi.species(guardian).equals("golurk") ? 12f + level / 4f : 7f + level / 4f;
    }

    private static void spawn(Pending p) {
        if (p.caged()) {
            OutpostCaptives.spawn(p.level(), p.pos(), p.yaw());
            return;
        }
        if (p.summoned() && countNear(p.level(), p.pos()) >= maxGuardians(p.level(), p.pos())) return;

        RandomSource random = p.level().random;
        boolean golurk = p.species().isEmpty() ? random.nextFloat() < GOLURK_CHANCE : p.species().equals("golurk");
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

    /** 1 guardião a cada 5 aldeões por perto, entre 2 e 4. */
    private static int maxGuardians(ServerLevel level, Vec3 pos) {
        int villagers = level.getEntitiesOfClass(Villager.class, new AABB(pos, pos).inflate(NEARBY_RADIUS)).size();
        return Math.max(MIN_GUARDIANS, Math.min(MAX_GUARDIANS, villagers / VILLAGERS_PER_GUARDIAN));
    }

    private static int countNear(ServerLevel level, Vec3 pos) {
        int count = 0;
        for (PokemonEntity guardian : guardians) {
            if (!guardian.isRemoved() && guardian.level() == level && guardian.position().closerThan(pos, NEARBY_RADIUS)) count++;
        }
        return count;
    }
}
