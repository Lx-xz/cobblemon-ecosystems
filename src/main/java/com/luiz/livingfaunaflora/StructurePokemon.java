package com.luiz.livingfaunaflora;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Animais colocados por estruturas (currais e estábulos das vilas, camelos do deserto,
 * gato da cabana da bruxa) e os gatos de vila viram Pokémon parecidos, que ficam no lugar.
 * Animais do mesmo cercado viram a mesma espécie; alguns vêm evoluídos.
 */
@EventBusSubscriber(modid = LivingFaunaFlora.MOD_ID)
public class StructurePokemon {
    /** Uma linha evolutiva: a forma básica e a evoluída (ou null se não evolui). */
    private record Family(String base, String evolved) {}

    /** Animal vanilla → famílias que podem aparecer no lugar dele (sorteada uma por cercado). */
    private static final Map<EntityType<?>, List<Family>> REPLACEMENTS = Map.of(
            EntityType.COW, List.of(new Family("miltank", null), new Family("tauros", null)),
            EntityType.MOOSHROOM, List.of(new Family("miltank", null)),
            EntityType.PIG, List.of(new Family("lechonk", "oinkologne")),
            EntityType.SHEEP, List.of(new Family("wooloo", "dubwool"), new Family("mareep", "flaaffy")),
            EntityType.CHICKEN, List.of(new Family("torchic", "combusken")),
            EntityType.HORSE, List.of(new Family("ponyta", "rapidash"), new Family("mudbray", "mudsdale")),
            EntityType.DONKEY, List.of(new Family("mudbray", "mudsdale")),
            EntityType.MULE, List.of(new Family("mudbray", "mudsdale")),
            EntityType.CAMEL, List.of(new Family("numel", "camerupt")),
            EntityType.CAT, List.of(new Family("meowth", "persian"), new Family("skitty", "delcatty"),
                    new Family("purrloin", "liepard"), new Family("espurr", "meowstic"))
    );
    /** Chance de cada Pokémon vir na forma evoluída. */
    private static final float EVOLVED_CHANCE = 0.2f;
    private static final int BASE_MIN_LEVEL = 5, BASE_MAX_LEVEL = 15;
    private static final int EVOLVED_MIN_LEVEL = 25, EVOLVED_MAX_LEVEL = 35;
    /** Distância (em blocos) em que outro Pokémon conta como "do mesmo cercado". */
    private static final double SAME_PEN_RADIUS = 8;

    private static final String REPLACE_KEY = LivingFaunaFlora.MOD_ID + ":replace";
    /** Guardados no Pokémon para os vizinhos copiarem a espécie: de qual animal veio e qual família virou. */
    private static final String SOURCE_KEY = LivingFaunaFlora.MOD_ID + ":source";
    private static final String FAMILY_KEY = LivingFaunaFlora.MOD_ID + ":family";

    private record Pending(ServerLevel level, EntityType<?> type, Vec3 pos, float yaw) {}
    private static final List<Pending> pending = new ArrayList<>();

    /**
     * Marca o animal na hora em que a estrutura o cria. Ele é salvo com a marca no chunk e, quando o
     * chunk carregar, LivingFaunaFlora bloqueia o animal e chama replaceIfMarked() para pôr o Pokémon no lugar.
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Entity entity = event.getEntity();
        if (!REPLACEMENTS.containsKey(entity.getType())) return;

        MobSpawnType reason = event.getSpawnType();
        // Gatos não têm spawn de bioma: NATURAL para gato é sempre o CatSpawner das vilas.
        boolean villageCat = reason == MobSpawnType.NATURAL && entity.getType() == EntityType.CAT;
        if (reason == MobSpawnType.STRUCTURE || villageCat) {
            entity.getPersistentData().putBoolean(REPLACE_KEY, true);
        }
    }

    /** Chamado por LivingFaunaFlora quando um animal bloqueado estava marcado para virar Pokémon. */
    static void replaceIfMarked(Entity animal, ServerLevel level) {
        if (animal.getPersistentData().getBoolean(REPLACE_KEY)) {
            // Não dá para criar entidades no meio do evento de entrada; fica para o fim do tick.
            pending.add(new Pending(level, animal.getType(), animal.position(), animal.getYRot()));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (pending.isEmpty()) return;
        // Um por vez: cada Pokémon já está no mundo quando o vizinho de cercado procurar por ele.
        for (Pending p : List.copyOf(pending)) spawn(p);
        pending.clear();
    }

    private static void spawn(Pending p) {
        RandomSource random = p.level().random;
        String source = BuiltInRegistries.ENTITY_TYPE.getKey(p.type()).toString();
        Family family = familyOfNeighbor(p, source);
        if (family == null) {
            List<Family> options = REPLACEMENTS.get(p.type());
            family = options.get(random.nextInt(options.size()));
        }

        boolean evolved = family.evolved() != null && random.nextFloat() < EVOLVED_CHANCE;
        String species = evolved ? family.evolved() : family.base();
        int level = evolved
                ? EVOLVED_MIN_LEVEL + random.nextInt(EVOLVED_MAX_LEVEL - EVOLVED_MIN_LEVEL + 1)
                : BASE_MIN_LEVEL + random.nextInt(BASE_MAX_LEVEL - BASE_MIN_LEVEL + 1);

        PokemonEntity pokemon = PokemonProperties.Companion.parse(species + " level=" + level).createEntity(p.level());
        pokemon.moveTo(p.pos().x, p.pos().y, p.pos().z, p.yaw(), 0f);
        pokemon.getPersistentData().putString(SOURCE_KEY, source);
        pokemon.getPersistentData().putString(FAMILY_KEY, family.base());
        KeptPokemon.keep(pokemon);
        p.level().addFreshEntity(pokemon);
    }

    /** Se já há um Pokémon do mesmo tipo de animal aqui perto (mesmo cercado), usa a família dele. */
    private static Family familyOfNeighbor(Pending p, String source) {
        AABB area = new AABB(p.pos(), p.pos()).inflate(SAME_PEN_RADIUS);
        List<PokemonEntity> nearby = p.level().getEntitiesOfClass(PokemonEntity.class, area,
                e -> source.equals(e.getPersistentData().getString(SOURCE_KEY)));
        for (PokemonEntity neighbor : nearby) {
            String base = neighbor.getPersistentData().getString(FAMILY_KEY);
            for (Family family : REPLACEMENTS.get(p.type())) {
                if (family.base().equals(base)) return family;
            }
        }
        return null;
    }
}
