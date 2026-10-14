package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Animais colocados por estruturas (currais e estábulos das vilas, camelos do deserto, gato da cabana da bruxa)
 * e os gatos de vila viram Pokémon que combinam com o tipo de vila (bioma), e ficam no lugar.
 * - Cada vila escolhe uma espécie por tipo de animal (uma vila tem Miltank, outra Tauros), com pesos: as raras
 *   aparecem em poucas vilas. A escolha é sempre a mesma dentro da vila.
 * - Alguns vêm evoluídos.
 * - As aves que substituem as galinhas botam ovos, como elas.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public class StructurePokemon {
    /** Uma linha evolutiva (a forma básica e a evoluída, ou null se não evolui) e o peso dela no sorteio. */
    private record Family(String base, String evolved, int weight) {}

    private static Family f(String base, String evolved) {
        return new Family(base, evolved, 10);
    }

    private static Family f(String base, String evolved, int weight) {
        return new Family(base, evolved, weight);
    }

    /** Tipos de vila (pelo bioma onde ela nasce). DEFAULT vale para qualquer outro lugar (estruturas de mods...). */
    private static final String DEFAULT = "default";
    private static final List<String> VILLAGE_TYPES = List.of("plains", "desert", "savanna", "snowy", "taiga");
    private static final Map<String, TagKey<Biome>> VILLAGE_BIOMES = Map.of(
            "plains", villageTag("plains"), "desert", villageTag("desert"), "savanna", villageTag("savanna"),
            "snowy", villageTag("snowy"), "taiga", villageTag("taiga"));

    private static TagKey<Biome> villageTag(String type) {
        return TagKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("has_structure/village_" + type));
    }

    /**
     * Animal vanilla → tipo de vila → famílias que podem aparecer no lugar dele, com pesos (padrão 10).
     * Cada vila sorteia uma família por animal: zebras na savana, renas na neve...
     */
    private static final Map<EntityType<?>, Map<String, List<Family>>> REPLACEMENTS = Map.of(
            EntityType.HORSE, Map.of(
                    "plains", List.of(f("mudbray", "mudsdale")),
                    "desert", List.of(f("ponyta", "rapidash")),
                    "savanna", List.of(f("blitzle", "zebstrika")),
                    "snowy", List.of(f("stantler", "wyrdeer")),
                    "taiga", List.of(f("deerling", "sawsbuck")),
                    DEFAULT, List.of(f("ponyta", "rapidash"), f("mudbray", "mudsdale"))),
            EntityType.COW, Map.of(
                    "plains", List.of(f("miltank", null, 7), f("tauros", null, 3)),
                    "desert", List.of(f("tauros", null)),
                    "savanna", List.of(f("bouffalant", null, 6), f("tauros", null, 4)),
                    "snowy", List.of(f("miltank", null)),
                    "taiga", List.of(f("miltank", null, 6), f("bouffalant", null, 4)),
                    DEFAULT, List.of(f("miltank", null, 7), f("tauros", null, 3))),
            EntityType.PIG, Map.of(
                    "plains", List.of(f("lechonk", "oinkologne")),
                    "desert", List.of(f("spoink", "grumpig")),
                    "savanna", List.of(f("tepig", "pignite")),
                    "snowy", List.of(f("swinub", "piloswine")),
                    "taiga", List.of(f("lechonk", "oinkologne", 6), f("swinub", "piloswine", 4)),
                    DEFAULT, List.of(f("lechonk", "oinkologne"))),
            EntityType.SHEEP, Map.of(
                    "plains", List.of(f("wooloo", "dubwool", 6), f("mareep", "flaaffy", 4)),
                    "desert", List.of(f("mareep", "flaaffy")),
                    "savanna", List.of(f("mareep", "flaaffy")),
                    "snowy", List.of(f("wooloo", "dubwool")),
                    "taiga", List.of(f("wooloo", "dubwool", 6), f("skiddo", "gogoat", 4)),
                    DEFAULT, List.of(f("wooloo", "dubwool", 6), f("mareep", "flaaffy", 4))),
            // Aves que botam ovos no lugar das galinhas: Doduo e Psyduck comuns, Torchic e Piplup raros.
            EntityType.CHICKEN, Map.of(
                    "plains", List.of(f("doduo", "dodrio", 45), f("psyduck", "golduck", 45), f("torchic", "combusken", 10)),
                    "desert", List.of(f("doduo", "dodrio", 90), f("torchic", "combusken", 10)),
                    "savanna", List.of(f("doduo", "dodrio", 55), f("psyduck", "golduck", 40), f("torchic", "combusken", 5)),
                    "snowy", List.of(f("psyduck", "golduck", 50), f("delibird", null, 45), f("piplup", "prinplup", 5)),
                    "taiga", List.of(f("psyduck", "golduck", 55), f("doduo", "dodrio", 45)),
                    DEFAULT, List.of(f("doduo", "dodrio", 45), f("psyduck", "golduck", 45), f("torchic", "combusken", 10))),
            EntityType.CAT, Map.of(
                    "desert", List.of(f("meowth", "persian", 7), f("espurr", "meowstic", 3)),
                    "savanna", List.of(f("litleo", "pyroar", 6), f("shinx", "luxio", 4)),
                    "snowy", List.of(f("glameow", "purugly", 6), f("sneasel", "weavile", 4)),
                    "taiga", List.of(f("purrloin", "liepard", 4), f("glameow", "purugly", 3), f("skitty", "delcatty", 3)),
                    DEFAULT, List.of(f("meowth", "persian", 4), f("skitty", "delcatty", 3), f("purrloin", "liepard", 2),
                            f("espurr", "meowstic", 1))),
            EntityType.CAMEL, Map.of(DEFAULT, List.of(f("numel", "camerupt"))),
            EntityType.MOOSHROOM, Map.of(DEFAULT, List.of(f("miltank", null))),
            EntityType.DONKEY, Map.of(DEFAULT, List.of(f("mudbray", "mudsdale"))),
            EntityType.MULE, Map.of(DEFAULT, List.of(f("mudbray", "mudsdale")))
    );
    /** O gato da cabana da bruxa vira o Purrloin, o gato de rua sombrio, que combina com a bruxa. */
    private static final Family WITCH_CAT = f("purrloin", "liepard");

    private static final int BASE_MIN_LEVEL = 5, BASE_MAX_LEVEL = 15;
    private static final int EVOLVED_MIN_LEVEL = 25, EVOLVED_MAX_LEVEL = 35;
    /** Ovos: como a galinha, um a cada 5 a 10 minutos. */
    private static final int EGG_MIN_TICKS = 6000, EGG_MAX_TICKS = 12000;

    private static final String REPLACE_KEY = CobblemonEcosystems.MOD_ID + ":replace";
    private static final String WITCH_KEY = CobblemonEcosystems.MOD_ID + ":witch_cat";
    /** Aspect do gato da bruxa: pelo preto e olhos laranja (no shiny, branco de olhos roxos). Passa para a evolução. */
    private static final String WITCH_ASPECT = "ecosystems_witch";
    /** No animal marcado: o tipo de vila e qual vila (para todos da mesma vila sortearem igual). */
    private static final String VILLAGE_TYPE_KEY = CobblemonEcosystems.MOD_ID + ":replace_village";
    private static final String VILLAGE_ID_KEY = CobblemonEcosystems.MOD_ID + ":replace_village_id";
    /** No Pokémon: de qual animal veio e quando bota o próximo ovo. */
    private static final String SOURCE_KEY = CobblemonEcosystems.MOD_ID + ":source";
    private static final String NEXT_EGG_KEY = CobblemonEcosystems.MOD_ID + ":next_egg";

    /**
     * Para testes com /summon: forçam o Pokémon a vir shiny ou evoluído. Ex.:
     * {NeoForgeData:{"cobblemon_ecosystems:replace":1b,"cobblemon_ecosystems:replace_shiny":1b,"cobblemon_ecosystems:replace_evolved":1b}}
     */
    private static final String FORCE_SHINY_KEY = CobblemonEcosystems.MOD_ID + ":replace_shiny";
    private static final String FORCE_EVOLVED_KEY = CobblemonEcosystems.MOD_ID + ":replace_evolved";

    private record Pending(ServerLevel level, EntityType<?> type, Vec3 pos, float yaw, boolean witchCat, String village, long villageId,
                           boolean forceShiny, boolean forceEvolved) {}
    private static final List<Pending> pending = new ArrayList<>();
    private static final Set<PokemonEntity> egglayers = new HashSet<>();
    private static int tick;

    /**
     * Marca o animal na hora em que a estrutura o cria. Ele é salvo com a marca no chunk e, quando o
     * chunk carregar, CobblemonEcosystems bloqueia o animal e chama replaceIfMarked() para pôr o Pokémon no lugar.
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Entity entity = event.getEntity();
        if (!Config.STRUCTURE_POKEMON.get() || !REPLACEMENTS.containsKey(entity.getType())) return;

        MobSpawnType reason = event.getSpawnType();
        // Gatos não têm spawn de bioma: NATURAL para gato é sempre o CatSpawner das vilas (e da cabana da bruxa).
        boolean villageCat = reason == MobSpawnType.NATURAL && entity.getType() == EntityType.CAT;
        if (reason != MobSpawnType.STRUCTURE && !villageCat) return;

        var data = entity.getPersistentData();
        data.putBoolean(REPLACE_KEY, true);
        data.putString(VILLAGE_TYPE_KEY, villageType(event.getLevel().getBiome(entity.blockPosition())));
        data.putLong(VILLAGE_ID_KEY, villageId(event.getLevel(), entity.blockPosition()));
        // A cabana da bruxa é o mesmo lugar onde o vanilla faz o gato nascer preto.
        if (entity.getType() == EntityType.CAT && event.getLevel().getLevel().structureManager()
                .getStructureWithPieceAt(entity.blockPosition(), StructureTags.CATS_SPAWN_AS_BLACK).isValid()) {
            data.putBoolean(WITCH_KEY, true);
        }
    }

    /** Tipo de vila pelo bioma: as mesmas tags que o Minecraft usa para escolher onde cada vila nasce. */
    private static String villageType(Holder<Biome> biome) {
        for (String type : VILLAGE_TYPES) {
            if (biome.is(VILLAGE_BIOMES.get(type))) return type;
        }
        return DEFAULT;
    }

    /** Identifica a vila pelo chunk onde a estrutura começa; fora de vila, por região de 64 blocos. */
    private static long villageId(ServerLevelAccessor level, BlockPos pos) {
        StructureStart village = level.getLevel().structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
        return village.isValid() ? village.getChunkPos().toLong() : BlockPos.asLong(pos.getX() >> 6, 0, pos.getZ() >> 6);
    }

    /** Chamado por CobblemonEcosystems quando um animal bloqueado estava marcado para virar Pokémon. */
    static boolean isMarked(Entity entity) {
        return entity.getPersistentData().getBoolean(REPLACE_KEY);
    }

    static void replaceIfMarked(Entity animal, ServerLevel level) {
        var data = animal.getPersistentData();
        if (!data.getBoolean(REPLACE_KEY)) return;
        // Não dá para criar entidades no meio do evento de entrada; fica para o fim do tick.
        pending.add(new Pending(level, animal.getType(), animal.position(), animal.getYRot(), data.getBoolean(WITCH_KEY),
                data.contains(VILLAGE_TYPE_KEY) ? data.getString(VILLAGE_TYPE_KEY) : DEFAULT,
                data.contains(VILLAGE_ID_KEY) ? data.getLong(VILLAGE_ID_KEY) : BlockPos.containing(animal.position()).asLong() >> 6,
                data.getBoolean(FORCE_SHINY_KEY), data.getBoolean(FORCE_EVOLVED_KEY)));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof PokemonEntity pokemon
                && "minecraft:chicken".equals(pokemon.getPersistentData().getString(SOURCE_KEY))) {
            egglayers.add(pokemon);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Pending p : List.copyOf(pending)) spawn(p);
        pending.clear();
        if (++tick % 100 == 0) layEggs();
    }

    private static void spawn(Pending p) {
        RandomSource random = p.level().random;
        Family family = p.witchCat() ? WITCH_CAT : familyOfVillage(p);

        boolean evolved = family.evolved() != null && (p.forceEvolved() || random.nextFloat() < Config.STRUCTURE_EVOLVED_CHANCE.get());
        String species = evolved ? family.evolved() : family.base();
        int level = evolved
                ? EVOLVED_MIN_LEVEL + random.nextInt(EVOLVED_MAX_LEVEL - EVOLVED_MIN_LEVEL + 1)
                : BASE_MIN_LEVEL + random.nextInt(BASE_MAX_LEVEL - BASE_MIN_LEVEL + 1);

        PokemonEntity pokemon = PokemonProperties.Companion.parse(species + " level=" + level + (p.forceShiny() ? " shiny=true" : "")).createEntity(p.level());
        if (p.witchCat()) {
            pokemon.getPokemon().setForcedAspects(new HashSet<>(Set.of(WITCH_ASPECT)));
            pokemon.getPokemon().updateAspects();
        }
        pokemon.moveTo(p.pos().x, p.pos().y, p.pos().z, p.yaw(), 0f);
        pokemon.getPersistentData().putString(SOURCE_KEY, BuiltInRegistries.ENTITY_TYPE.getKey(p.type()).toString());
        KeptPokemon.keep(pokemon);
        p.level().addFreshEntity(pokemon);
    }

    /**
     * A família desta vila para este animal. O sorteio usa a semente do mundo, a vila e o animal, então dá sempre
     * o mesmo resultado dentro da vila, mesmo para cercados em chunks que carregam em momentos diferentes.
     */
    private static Family familyOfVillage(Pending p) {
        Map<String, List<Family>> byVillage = REPLACEMENTS.get(p.type());
        List<Family> options = byVillage.getOrDefault(p.village(), byVillage.get(DEFAULT));
        long seed = p.level().getSeed() ^ (p.villageId() * 341873128712L) ^ BuiltInRegistries.ENTITY_TYPE.getKey(p.type()).hashCode();
        int total = options.stream().mapToInt(Family::weight).sum();
        int roll = RandomSource.create(seed).nextInt(total);
        for (Family family : options) {
            roll -= family.weight();
            if (roll < 0) return family;
        }
        return options.get(0);
    }

    /** As aves que vieram das galinhas botam ovos de vez em quando. */
    private static void layEggs() {
        egglayers.removeIf(Entity::isRemoved);
        for (PokemonEntity pokemon : egglayers) {
            if (pokemon.isBusy() || !PokemonAi.isWild(pokemon)) continue;
            var data = pokemon.getPersistentData();
            long now = pokemon.level().getGameTime();
            if (!data.contains(NEXT_EGG_KEY)) data.putLong(NEXT_EGG_KEY, now + nextEggDelay(pokemon.getRandom()));
            if (now < data.getLong(NEXT_EGG_KEY)) continue;
            pokemon.spawnAtLocation(Items.EGG);
            pokemon.level().playSound(null, pokemon.getX(), pokemon.getY(), pokemon.getZ(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL,
                    1f, (pokemon.getRandom().nextFloat() - pokemon.getRandom().nextFloat()) * 0.2f + 1f);
            data.putLong(NEXT_EGG_KEY, now + nextEggDelay(pokemon.getRandom()));
        }
    }

    private static int nextEggDelay(RandomSource random) {
        return EGG_MIN_TICKS + random.nextInt(EGG_MAX_TICKS - EGG_MIN_TICKS);
    }
}
