package com.luiz.cobblemonecosystems;

import com.luiz.cobblemonecosystems.ConfigLists.Family;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Gender;

import net.neoforged.neoforge.common.ModConfigSpec;
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
import net.minecraft.world.item.ItemStack;
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
    /** Tipos de vila (pelo bioma onde ela nasce). DEFAULT vale para qualquer outro lugar (estruturas de mods...). */
    private static final String DEFAULT = "default";
    private static final List<String> VILLAGE_TYPES = List.of("plains", "desert", "savanna", "snowy", "taiga");
    private static final Map<String, TagKey<Biome>> VILLAGE_BIOMES = Map.of(
            "plains", villageTag("plains"), "desert", villageTag("desert"), "savanna", villageTag("savanna"),
            "snowy", villageTag("snowy"), "taiga", villageTag("taiga"));

    private static TagKey<Biome> villageTag(String type) {
        return TagKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("has_structure/village_" + type));
    }

    /** Animal vanilla → a lista da configuração com as famílias de cada tipo de vila ([village_pens]). */
    private static final Map<EntityType<?>, ModConfigSpec.ConfigValue<List<? extends String>>> PENS = Map.of(
            EntityType.HORSE, Config.PEN_HORSE, EntityType.DONKEY, Config.PEN_DONKEY, EntityType.MULE, Config.PEN_MULE,
            EntityType.CAMEL, Config.PEN_CAMEL, EntityType.COW, Config.PEN_COW, EntityType.MOOSHROOM, Config.PEN_MOOSHROOM,
            EntityType.PIG, Config.PEN_PIG, EntityType.SHEEP, Config.PEN_SHEEP, EntityType.CHICKEN, Config.PEN_CHICKEN,
            EntityType.CAT, Config.PEN_CAT);
    /** O gato da cabana da bruxa vira o Purrloin, o gato de rua sombrio (a textura de gato preto é só dele). */
    private static final Family WITCH_CAT = new Family("purrloin", "liepard", 10);

    private static final int BASE_MIN_LEVEL = 5, BASE_MAX_LEVEL = 15;
    private static final int EVOLVED_MIN_LEVEL = 25, EVOLVED_MAX_LEVEL = 35;
    /** Ovos: como a galinha, um a cada 5 a 10 minutos. */
    private static final int EGG_MIN_TICKS = 6000, EGG_MAX_TICKS = 12000;

    private static final String REPLACE_KEY = CobblemonEcosystems.MOD_ID + ":replace";
    private static final String WITCH_KEY = CobblemonEcosystems.MOD_ID + ":witch_cat";
    /** Aspect do gato da bruxa: pelo preto e olhos laranja (no shiny, branco de olhos roxos). Passa para a evolução. */
    static final String WITCH_ASPECT = "ecosystems_witch";
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
        if (!Config.STRUCTURE_POKEMON.get() || !PENS.containsKey(entity.getType())) return;

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
                && ("minecraft:chicken".equals(pokemon.getPersistentData().getString(SOURCE_KEY))
                    || ConfigLists.species(Config.EGG_LAYING_SPECIES).contains(PokemonAi.species(pokemon)))) {
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
        if (family == null) return; // lista vazia na configuração: o animal só some

        boolean evolved = family.evolved() != null && (p.forceEvolved() || random.nextFloat() < Config.STRUCTURE_EVOLVED_CHANCE.get());
        String species = evolved ? family.evolved() : family.base();
        int level = evolved
                ? EVOLVED_MIN_LEVEL + random.nextInt(EVOLVED_MAX_LEVEL - EVOLVED_MIN_LEVEL + 1)
                : BASE_MIN_LEVEL + random.nextInt(BASE_MAX_LEVEL - BASE_MIN_LEVEL + 1);

        // O gato da bruxa é dela ({@link HutWitch}): só dá para ganhar vencendo a bruxa.
        boolean witchPet = p.witchCat() && Config.WITCH_PET.get();
        PokemonEntity pokemon = PokemonProperties.Companion.parse(species + " level=" + level + (p.forceShiny() ? " shiny=true" : "")
                + (witchPet ? " uncatchable" : "")).createEntity(p.level());
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
        Map<String, List<Family>> byVillage = ConfigLists.keyed(PENS.get(p.type()));
        List<Family> options = byVillage.getOrDefault(p.village(), byVillage.getOrDefault(DEFAULT, List.of()));
        long seed = p.level().getSeed() ^ (p.villageId() * 341873128712L) ^ BuiltInRegistries.ENTITY_TYPE.getKey(p.type()).hashCode();
        return ConfigLists.pick(options, RandomSource.create(seed));
    }

    /**
     * As fêmeas das aves que vieram das galinhas (selvagens, nos currais) e as fêmeas dessas espécies num pasto
     * botam ovos de vez em quando. Machos e Pokémon seguindo o jogador não botam. No pasto, o
     * dono pode desligar com o botão de cesta ({@link PastureFarms#isOff}).
     */
    private static void layEggs() {
        egglayers.removeIf(Entity::isRemoved);
        if (!Config.EGG_LAYERS.get()) return;
        for (PokemonEntity pokemon : egglayers) {
            boolean fromChicken = "minecraft:chicken".equals(pokemon.getPersistentData().getString(SOURCE_KEY));
            boolean inPen = fromChicken && PokemonAi.isWild(pokemon);
            boolean inPasture = pokemon.getTethering() != null
                    && !PastureFarms.isOff(pokemon)
                    && ConfigLists.species(Config.EGG_LAYING_SPECIES).contains(PokemonAi.species(pokemon));
            if (pokemon.isBusy() || (!inPen && !inPasture) || pokemon.getPokemon().getGender() != Gender.FEMALE) continue;
            var data = pokemon.getPersistentData();
            long now = pokemon.level().getGameTime();
            if (!data.contains(NEXT_EGG_KEY)) data.putLong(NEXT_EGG_KEY, now + nextEggDelay(pokemon.getRandom()));
            if (now < data.getLong(NEXT_EGG_KEY)) continue;
            if (inPasture) PastureFarms.deliver(pokemon, new ItemStack(Items.EGG));
            else pokemon.spawnAtLocation(Items.EGG);
            pokemon.level().playSound(null, pokemon.getX(), pokemon.getY(), pokemon.getZ(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL,
                    1f, (pokemon.getRandom().nextFloat() - pokemon.getRandom().nextFloat()) * 0.2f + 1f);
            data.putLong(NEXT_EGG_KEY, now + nextEggDelay(pokemon.getRandom()));
        }
    }

    private static int nextEggDelay(RandomSource random) {
        return EGG_MIN_TICKS + random.nextInt(EGG_MAX_TICKS - EGG_MIN_TICKS);
    }
}
