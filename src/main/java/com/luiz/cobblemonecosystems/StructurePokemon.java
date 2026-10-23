package com.luiz.cobblemonecosystems;

import com.luiz.cobblemonecosystems.ConfigLists.Family;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Animais colocados por estruturas (currais e estábulos das vilas, camelos do deserto, gato da cabana da bruxa)
 * e os gatos de vila viram Pokémon que combinam com o tipo de vila (bioma), e ficam no lugar.
 * - Currais, estábulos e currais de açougue são enchidos pelo mod (não pelo vanilla, que mistura os animais, às
 *   vezes põe 6 e às vezes nenhum): um animal só por curral (só vacas, só porcos...), sorteado por curral, galinhas
 *   também, e sempre 2 ou 3 Pokémon.
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

    /** Sorteio do animal de cada curral, com os pesos do vanilla (vacas e porcos comuns, ovelhas raras) e galinhas. */
    private record PenKind(EntityType<?> type, int weight) {}
    private static final List<PenKind> PEN_KINDS = List.of(new PenKind(EntityType.COW, 7), new PenKind(EntityType.PIG, 7),
            new PenKind(EntityType.HORSE, 5), new PenKind(EntityType.CHICKEN, 5), new PenKind(EntityType.SHEEP, 2));
    private static final List<PenKind> STABLE_KINDS = List.of(new PenKind(EntityType.HORSE, 1));
    private static final List<PenKind> BUTCHER_KINDS = List.of(new PenKind(EntityType.COW, 3), new PenKind(EntityType.PIG, 3),
            new PenKind(EntityType.CHICKEN, 3), new PenKind(EntityType.SHEEP, 2));

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
    /** No animal marcado: nasceu num curral, que é enchido por {@link #fillPens}; ele só some. */
    private static final String IN_PEN_KEY = CobblemonEcosystems.MOD_ID + ":replace_in_pen";
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
    /** Chunks recém-gerados, para encher os currais no fim do tick. */
    private record NewChunk(ServerLevel level, ChunkPos pos) {}
    private static final Queue<NewChunk> newChunks = new ConcurrentLinkedQueue<>();
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
        if (reason == MobSpawnType.STRUCTURE && entity.getType() != EntityType.CAT && inPen(event.getLevel(), entity.blockPosition())) {
            data.putBoolean(IN_PEN_KEY, true);
        }
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

    /**
     * Os animais que a peça de vila pode ter, se ela for um curral (currais, estábulos e o curral do açougueiro);
     * null nas outras (as casas de pastor, que já só têm ovelhas, não mudam).
     */
    private static List<PenKind> penKinds(StructurePiece piece) {
        if (!(piece instanceof PoolElementStructurePiece poolPiece)) return null;
        // Ex.: "Single[Left[minecraft:village/plains/houses/plains_animal_pen_1]]"
        String name = poolPiece.getElement().toString();
        if (!name.contains("village/")) return null;
        return name.contains("_animal_pen_") ? PEN_KINDS
                : name.contains("_stable_") ? STABLE_KINDS
                : name.contains("butcher") ? BUTCHER_KINDS
                : null;
    }

    /** O animal nasceu dentro de um curral de vila? */
    private static boolean inPen(ServerLevelAccessor level, BlockPos pos) {
        StructureStart village = level.getLevel().structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
        if (!village.isValid()) return false;
        for (StructurePiece piece : village.getPieces()) {
            if (piece.getBoundingBox().isInside(pos) && penKinds(piece) != null) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.isNewChunk() && event.getLevel() instanceof ServerLevel level && Config.STRUCTURE_POKEMON.get()) {
            newChunks.add(new NewChunk(level, event.getChunk().getPos()));
        }
    }

    /**
     * Enche os currais das vilas que começam neste chunk novo. Cada curral tem um animal só, sorteado pela semente
     * do mundo e pela posição do curral, e 2 Pokémon (3 nos currais grandes), nos pontos onde o vanilla poria os
     * animais. O curral é enchido uma vez, quando nasce o chunk do primeiro desses pontos.
     */
    private static void fillPens(ServerLevel level, ChunkPos chunk) {
        for (StructureStart start : level.structureManager().startsForStructure(chunk, structure -> true)) {
            for (StructurePiece piece : start.getPieces()) {
                List<PenKind> kinds = penKinds(piece);
                if (kinds == null) continue;
                PoolElementStructurePiece poolPiece = (PoolElementStructurePiece) piece;
                List<BlockPos> slots = new ArrayList<>();
                for (StructureTemplate.StructureBlockInfo jigsaw : poolPiece.getElement().getShuffledJigsawBlocks(
                        level.getStructureManager(), poolPiece.getPosition(), poolPiece.getRotation(), RandomSource.create(0))) {
                    if (jigsaw.nbt() != null && jigsaw.nbt().getString("pool").contains("animals")) {
                        slots.add(jigsaw.pos().relative(JigsawBlock.getFrontFacing(jigsaw.state())));
                    }
                }
                if (slots.isEmpty()) continue;
                slots.sort(Comparator.comparingLong(BlockPos::asLong));
                if (!new ChunkPos(slots.get(0)).equals(chunk)) continue;

                BoundingBox box = piece.getBoundingBox();
                RandomSource random = RandomSource.create(level.getSeed() ^ BlockPos.asLong(box.minX(), box.minY(), box.minZ()) * 6364136223846793005L);
                int roll = random.nextInt(kinds.stream().mapToInt(PenKind::weight).sum());
                EntityType<?> type = kinds.get(0).type();
                for (PenKind kind : kinds) {
                    if ((roll -= kind.weight()) < 0) {
                        type = kind.type();
                        break;
                    }
                }
                int count = kinds == STABLE_KINDS ? 2 : Math.min(slots.size() + 1, 3);
                String village = villageType(level.getBiome(slots.get(0)));
                for (int i = 0; i < count; i++) {
                    BlockPos slot = slots.get(i % slots.size());
                    if (!level.isLoaded(slot)) slot = slots.get(0);
                    Vec3 pos = Vec3.atBottomCenterOf(slot).add((random.nextDouble() - 0.5) * 0.6, 0, (random.nextDouble() - 0.5) * 0.6);
                    pending.add(new Pending(level, type, pos, random.nextFloat() * 360, false, village, start.getChunkPos().toLong(), false, false));
                }
            }
        }
    }

    /** Chamado por CobblemonEcosystems quando um animal bloqueado estava marcado para virar Pokémon. */
    static boolean isMarked(Entity entity) {
        return entity.getPersistentData().getBoolean(REPLACE_KEY);
    }

    static void replaceIfMarked(Entity animal, ServerLevel level) {
        var data = animal.getPersistentData();
        if (!data.getBoolean(REPLACE_KEY)) return;
        if (data.getBoolean(IN_PEN_KEY)) return; // o curral é enchido por fillPens
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
        for (NewChunk chunk; (chunk = newChunks.poll()) != null; ) fillPens(chunk.level(), chunk.pos());
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
