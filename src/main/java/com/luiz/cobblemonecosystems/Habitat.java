package com.luiz.cobblemonecosystems;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.luiz.cobblemonecosystems.ConfigLists.BlockGroup;
import com.luiz.cobblemonecosystems.ConfigLists.Family;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * O habitat de um pasto: o espaço que os Pokémon alcançam a partir do bloco do pasto, até {@code habitat_radius}
 * blocos, parando nas paredes e cercas. Se o bloco está num canto, o habitat fica só do lado aberto; num cercado,
 * é o cercado. O que encosta nesse espaço (chão, paredes, teto, e o que está dentro, como água e lava) é contado
 * nos grupos de {@code habitat_features}, e cada tipo gosta ou não gosta de alguns grupos ({@code habitat_preferences}).
 * Daí sai o {@link Comfort} de cada Pokémon do pasto.
 *
 * O levantamento é um por pasto (não por Pokémon) e é refeito a cada 30 segundos, só quando alguém pergunta.
 */
public final class Habitat {
    /** Do pior para o melhor. */
    public enum Comfort { UNCOMFORTABLE, NEUTRAL, COMFORTABLE, HAPPY }

    /** Pela natureza: caseiros quase não saem do habitat, exploradores passeiam longe. */
    public enum Temperament { HOMEBODY, BALANCED, EXPLORER }

    /** Quanto o habitat se estende para baixo e para cima do bloco do pasto. */
    private static final int BELOW = 3, ABOVE = 5;
    /** Limite de células, para um pasto em campo aberto não pesar. */
    private static final int MAX_CELLS = 12000;
    private static final long SURVEY_TICKS = 600;
    /** Abaixo disso de chão por Pokémon, o pasto está apertado. */
    private static final int CRAMPED_AREA = 4, TIGHT_AREA = 8;
    /** Perdido (não achou o caminho de casa): fica triste até voltar. Quando se perdeu, para tentar de novo. */
    public static final String LOST_KEY = CobblemonEcosystems.MOD_ID + ":lost";
    public static final String LOST_AT_KEY = CobblemonEcosystems.MOD_ID + ":lost_at";

    /** Os grupos prontos, calculados (0 a 1) em vez de contados. */
    public static final List<String> BUILT_IN = List.of("open_sky", "roof", "darkness", "bright", "cold_biome", "hot_biome", "rain");

    /**
     * O levantamento de um pasto.
     * @param columns colunas (x, z) do habitat, para saber se um Pokémon está dentro dele
     * @param floor células com chão firme embaixo (onde dá para ficar em pé)
     * @param counts quantos blocos de cada grupo encostam no habitat
     */
    public record Survey(long time, Set<Long> columns, List<BlockPos> floor, Map<String, Integer> counts,
                         float skyFraction, float light, float temperature) {
        public boolean isEmpty() {
            return floor.isEmpty();
        }

        public boolean contains(BlockPos pos) {
            return columns.contains(column(pos));
        }
    }

    /** Uma linha do relatório: um grupo de que o tipo gosta (ou não), quanto ele tem (0 a 1) e quantos blocos. */
    public record Factor(String group, boolean likes, float value, int blocks) {}

    /** O conforto de um Pokémon e o porquê. */
    public record Report(Comfort comfort, float score, List<Factor> factors, int areaPerPokemon, boolean cramped, boolean lost,
                         Temperament temperament) {}

    private static final Map<GlobalPos, Survey> surveys = new HashMap<>();

    private Habitat() {}

    private static long column(BlockPos pos) {
        return BlockPos.asLong(pos.getX(), 0, pos.getZ());
    }

    public static boolean enabled() {
        return Config.HABITAT.get();
    }

    /** O levantamento do pasto, refeito se estiver velho. */
    public static Survey survey(ServerLevel level, BlockPos pasture) {
        GlobalPos key = GlobalPos.of(level.dimension(), pasture);
        Survey survey = surveys.get(key);
        long now = level.getGameTime();
        if (survey == null || now - survey.time() > SURVEY_TICKS) {
            survey = measure(level, pasture, now);
            surveys.put(key, survey);
            if (surveys.size() > 512) surveys.entrySet().removeIf(e -> now - e.getValue().time() > SURVEY_TICKS * 4);
        }
        return survey;
    }

    /** Dá para ocupar: sem colisão (ar, plantas, água, lava, fogo, neve fina...). */
    private static boolean open(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static Survey measure(ServerLevel level, BlockPos pasture, long now) {
        int radius = Config.HABITAT_RADIUS.get();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (Direction dir : Direction.values()) {
            BlockPos start = pasture.relative(dir);
            if (open(level, start) && visited.add(start.asLong())) queue.add(start);
        }

        Set<Long> touched = new HashSet<>();
        Set<Long> columns = new HashSet<>();
        List<BlockPos> floor = new ArrayList<>();
        List<BlockPos> cells = new ArrayList<>();
        while (!queue.isEmpty() && cells.size() < MAX_CELLS) {
            BlockPos cell = queue.poll();
            cells.add(cell);
            columns.add(column(cell));
            if (!level.getBlockState(cell).isAir()) touched.add(cell.asLong());
            if (!open(level, cell.below())) floor.add(cell);
            for (Direction dir : Direction.values()) {
                BlockPos next = cell.relative(dir);
                int dx = next.getX() - pasture.getX(), dz = next.getZ() - pasture.getZ(), dy = next.getY() - pasture.getY();
                if (open(level, next)) {
                    if (dx * dx + dz * dz <= radius * radius && dy >= -BELOW && dy <= ABOVE && visited.add(next.asLong())) {
                        queue.add(next);
                    }
                } else if (!next.equals(pasture)) {
                    touched.add(next.asLong()); // chão, parede ou teto
                }
            }
        }

        // Conta cada bloco uma vez em cada grupo.
        List<BlockGroup> groups = ConfigLists.blockGroups(Config.HABITAT_FEATURES);
        Map<String, Integer> counts = new HashMap<>();
        Map<BlockState, List<String>> memo = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (long packed : touched) {
            pos.set(packed);
            BlockState state = level.getBlockState(pos);
            for (String group : memo.computeIfAbsent(state, s -> {
                List<String> names = new ArrayList<>();
                for (BlockGroup g : groups) if (g.matches(s)) names.add(g.name());
                return names;
            })) {
                counts.merge(group, 1, Integer::sum);
            }
        }

        int sky = 0;
        float light = 0;
        for (BlockPos cell : floor) {
            if (level.canSeeSky(cell)) sky++;
            light += Math.max(level.getBrightness(LightLayer.BLOCK, cell), level.getBrightness(LightLayer.SKY, cell));
        }
        float skyFraction = floor.isEmpty() ? 0 : (float) sky / floor.size();
        light = floor.isEmpty() ? 0 : light / floor.size();
        float temperature = level.getBiome(pasture).value().getBaseTemperature();
        return new Survey(now, Set.copyOf(columns), List.copyOf(floor), Map.copyOf(counts), skyFraction, light, temperature);
    }

    /** Quanto o habitat tem de um grupo, de 0 a 1. */
    private static float value(ServerLevel level, BlockPos pasture, Survey survey, String group) {
        return switch (group) {
            case "open_sky" -> Math.min(1f, survey.skyFraction() / 0.6f);
            case "roof" -> Math.min(1f, (1f - survey.skyFraction()) / 0.6f);
            case "darkness" -> clamp((10f - survey.light()) / 6f);
            case "bright" -> clamp((survey.light() - 7f) / 6f);
            case "cold_biome" -> clamp((0.5f - survey.temperature()) / 0.35f);
            case "hot_biome" -> clamp((survey.temperature() - 0.9f) / 0.6f);
            case "rain" -> level.isRainingAt(pasture.above()) || level.isRainingAt(pasture.above(2)) ? 1f : 0f;
            default -> {
                int target = 8;
                for (BlockGroup g : ConfigLists.blockGroups(Config.HABITAT_FEATURES)) {
                    if (g.name().equals(group)) target = g.target();
                }
                yield Math.min(1f, survey.counts().getOrDefault(group, 0) / (float) target);
            }
        };
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public static Temperament temperament(PokemonEntity pokemon) {
        String nature = pokemon.getPokemon().getNature().getName().getPath();
        if (ConfigLists.species(Config.HOMEBODY_NATURES).contains(nature)) return Temperament.HOMEBODY;
        if (ConfigLists.species(Config.EXPLORER_NATURES).contains(nature)) return Temperament.EXPLORER;
        return Temperament.BALANCED;
    }

    /** O conforto de um Pokémon do pasto, com o relatório (null se não estiver num pasto). */
    public static Report report(PokemonEntity pokemon) {
        PokemonPastureBlockEntity.Tethering tethering = pokemon.getTethering();
        if (tethering == null || !(pokemon.level() instanceof ServerLevel level)) return null;
        BlockPos pasture = tethering.getPasturePos();
        Survey survey = survey(level, pasture);

        Map<String, List<Family>> preferences = ConfigLists.keyed(Config.HABITAT_PREFERENCES);
        Map<String, Factor> factors = new HashMap<>();
        // Uma linha com o nome da espécie (ex.: "zubat = ...") vale no lugar das linhas dos tipos dela.
        List<List<Family>> sets = new ArrayList<>();
        List<Family> own = preferences.get(PokemonAi.species(pokemon));
        if (own != null) {
            sets.add(own);
        } else {
            for (ElementalType type : pokemon.getPokemon().getTypes()) {
                sets.add(preferences.getOrDefault(type.getName().toLowerCase(), List.of()));
            }
        }
        // Do que gosta: uma nota de 0 a 1 por tipo, juntadas pela regra da configuração. Do que não gosta: soma, mas
        // um grupo de que os dois tipos não gostam conta uma vez só.
        List<Float> likeScores = new ArrayList<>();
        Map<String, Float> dislikes = new HashMap<>();
        for (List<Family> prefs : sets) {
            float likeWeight = 0, likeSum = 0;
            for (Family pref : prefs) {
                boolean likes = !pref.base().startsWith("-");
                String group = likes ? pref.base() : pref.base().substring(1);
                float v = value(level, pasture, survey, group);
                if (likes) {
                    likeWeight += pref.weight();
                    likeSum += pref.weight() * v;
                } else {
                    dislikes.merge(group, pref.weight() / 10f * v, Math::max);
                }
                factors.putIfAbsent(group, new Factor(group, likes, v, survey.counts().getOrDefault(group, 0)));
            }
            likeScores.add(likeWeight > 0 ? likeSum / likeWeight : 0.5f);
        }
        float score = combine(likeScores) - 0.6f * (float) dislikes.values().stream().mapToDouble(Float::doubleValue).sum();

        int pokemonCount = 1;
        if (level.getBlockEntity(pasture) instanceof PokemonPastureBlockEntity block) {
            pokemonCount = Math.max(1, block.getTetheredPokemon().size());
        }
        int area = survey.floor().size() / pokemonCount;
        boolean cramped = area < TIGHT_AREA;
        if (area < CRAMPED_AREA) score -= 0.4f;
        else if (area < TIGHT_AREA) score -= 0.2f;

        boolean lost = pokemon.getPersistentData().getBoolean(LOST_KEY);
        if (lost) score -= 0.5f;

        Comfort comfort = score >= 0.7f ? Comfort.HAPPY : score >= 0.35f ? Comfort.COMFORTABLE
                : score >= 0f ? Comfort.NEUTRAL : Comfort.UNCOMFORTABLE;
        return new Report(comfort, score, List.copyOf(factors.values()), area, cramped, lost, temperament(pokemon));
    }

    /**
     * Junta as notas dos tipos pela regra {@code habitat_type_rule}: o melhor tipo (padrão), a média, ou o primeiro
     * tipo pesando 2/3.
     */
    private static float combine(List<Float> scores) {
        if (scores.isEmpty()) return 0.5f;
        if (scores.size() == 1) return scores.get(0);
        return switch (Config.HABITAT_TYPE_RULE.get()) {
            case BEST -> Math.max(scores.get(0), scores.get(1));
            case AVERAGE -> (scores.get(0) + scores.get(1)) / 2f;
            case PRIMARY -> scores.get(0) * 2f / 3f + scores.get(1) / 3f;
        };
    }

    /** Como juntar os dois tipos de um Pokémon. */
    public enum TypeRule { BEST, AVERAGE, PRIMARY }

    /** Atalho para as farms: o conforto do Pokémon, ou null se não estiver num pasto (ou os habitats estiverem desligados). */
    public static Comfort comfort(PokemonEntity pokemon) {
        if (!enabled()) return null;
        Report report = report(pokemon);
        return report == null ? null : report.comfort();
    }
}
