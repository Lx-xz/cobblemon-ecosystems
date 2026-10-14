package com.luiz.cobblemonecosystems;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Pokémon cachorro fazem o papel dos lobos: caçam esqueletos (do jogador ou selvagens) e, se forem
 * selvagens, de vez em quando caçam Pokémon ovelha selvagens.
 * (Esqueletos fugirem deles e gatos espantarem creepers e phantoms o Cobblemon já faz sozinho.)
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class DogHunters {
    private static final Set<String> DOGS = Set.of(
            "growlithe", "arcanine", "lillipup", "herdier", "stoutland", "rockruff", "lycanroc",
            "poochyena", "mightyena", "houndour", "houndoom", "electrike", "manectric",
            "yamper", "boltund", "maschiff", "mabosstiff", "snubbull", "granbull", "fidough", "dachsbun");
    private static final Set<String> SHEEP = Set.of("wooloo", "dubwool", "mareep");
    private static final double SKELETON_RANGE = 12;
    private static final double SHEEP_RANGE = 10;
    /** Por checagem (a cada segundo). Lobos selvagens também não caçam ovelha o tempo todo. */
    private static final float SHEEP_HUNT_CHANCE = 0.02f;

    private static final Set<PokemonEntity> dogs = new HashSet<>();
    private static int tick;

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof PokemonEntity pokemon
                && DOGS.contains(PokemonAi.species(pokemon))) {
            dogs.add(pokemon);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tick % 20 != 0 || !Config.DOG_HUNTERS.get()) return;
        dogs.removeIf(Entity::isRemoved);
        for (PokemonEntity dog : dogs) {
            if (dog.isBusy() || Hunts.isHunting(dog)) continue;
            LivingEntity target = nearestSkeleton(dog);
            if (target == null && PokemonAi.isWild(dog) && dog.getRandom().nextFloat() < SHEEP_HUNT_CHANCE) {
                target = nearestWildSheep(dog);
            }
            if (target != null) Hunts.start(dog, target, damage(dog), 0.1, 15);
        }
    }

    /** Golpe de 3 a ~9 de dano conforme o nível (lobo do vanilla dá 4). */
    private static float damage(PokemonEntity dog) {
        return 3f + dog.getPokemon().getLevel() / 15f;
    }

    private static LivingEntity nearestSkeleton(PokemonEntity dog) {
        return dog.level().getEntitiesOfClass(AbstractSkeleton.class, dog.getBoundingBox().inflate(SKELETON_RANGE),
                        skeleton -> skeleton.isAlive() && dog.hasLineOfSight(skeleton))
                .stream().min(Comparator.comparingDouble(dog::distanceToSqr)).orElse(null);
    }

    /** Só selvagens soltos: nunca Pokémon de jogador, de vila, de mercador ou de colmeia. */
    private static LivingEntity nearestWildSheep(PokemonEntity dog) {
        return dog.level().getEntitiesOfClass(PokemonEntity.class, dog.getBoundingBox().inflate(SHEEP_RANGE),
                        sheep -> sheep.isAlive() && !sheep.isBusy() && PokemonAi.isWild(sheep) && !KeptPokemon.isKept(sheep)
                                && SHEEP.contains(PokemonAi.species(sheep)) && dog.hasLineOfSight(sheep))
                .stream().min(Comparator.comparingDouble(dog::distanceToSqr)).orElse(null);
    }
}
