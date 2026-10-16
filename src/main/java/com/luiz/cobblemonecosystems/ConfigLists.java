package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Lê as listas editáveis do {@link Config}. Os formatos, iguais em todas as listas:
 * <ul>
 *   <li>espécie com peso: {@code "golett:25"} (sem peso, vale 10);</li>
 *   <li>linha evolutiva: {@code "mudbray>mudsdale:10"} (forma básica, evoluída e peso; os dois últimos opcionais);</li>
 *   <li>com chave: {@code "plains = miltank:7, tauros:3"} (várias entradas separadas por vírgula; a mesma chave
 *       pode aparecer em mais de uma linha);</li>
 *   <li>mobs: {@code "minecraft:fox"}, {@code "#minecraft:tag"} para uma tag e {@code "-minecraft:fox"} para deixar
 *       um mob de fora.</li>
 * </ul>
 * Entradas com erro são ignoradas (com um aviso no log). O resultado fica guardado até a lista mudar.
 */
public final class ConfigLists {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_WEIGHT = 10;

    /** Uma linha evolutiva (evolved é null se não evolui) e o peso dela no sorteio. */
    public record Family(String base, String evolved, int weight) {
        /** A forma básica e a evoluída. */
        public List<String> species() {
            return evolved == null ? List.of(base) : List.of(base, evolved);
        }
    }

    private record Parsed(Object raw, Object value) {}
    private static final Map<ModConfigSpec.ConfigValue<?>, Parsed> CACHE = new ConcurrentHashMap<>();

    private ConfigLists() {}

    /** Validador das listas no arquivo e na tela de configuração: qualquer texto (linhas vazias são ignoradas). */
    public static boolean validEntry(Object entry) {
        return entry instanceof String;
    }

    /** Para listas com formato próprio (ex.: as farms): o resultado do parser fica guardado até a lista mudar. */
    public static <T> T cachedList(ModConfigSpec.ConfigValue<List<? extends String>> value, Function<List<? extends String>, T> parser) {
        return cached(value, parser);
    }

    /** Avisa no log que a entrada foi ignorada e devolve null. */
    public static <T> T invalid(ModConfigSpec.ConfigValue<?> value, String entry, String problem) {
        warn(value, entry, problem);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cached(ModConfigSpec.ConfigValue<List<? extends String>> value, Function<List<? extends String>, T> parser) {
        List<? extends String> raw = value.get();
        Parsed parsed = CACHE.get(value);
        if (parsed == null || parsed.raw() != raw) {
            parsed = new Parsed(raw, parser.apply(raw));
            CACHE.put(value, parsed);
        }
        return (T) parsed.value();
    }

    // ----- Espécies -----

    /** Só os nomes das espécies (pesos e evoluções contam como espécies também). */
    public static Set<String> species(ModConfigSpec.ConfigValue<List<? extends String>> value) {
        return cached(value, raw -> {
            Set<String> set = new HashSet<>();
            for (String entry : raw) {
                if (entry.isBlank()) continue;
                Family family = family(entry, value);
                if (family != null) set.addAll(family.species());
            }
            return Set.copyOf(set);
        });
    }

    /** Linhas evolutivas com peso. */
    public static List<Family> families(ModConfigSpec.ConfigValue<List<? extends String>> value) {
        return cached(value, raw -> {
            List<Family> list = new ArrayList<>();
            for (String entry : raw) {
                if (entry.isBlank()) continue;
                Family family = family(entry, value);
                if (family != null) list.add(family);
            }
            return List.copyOf(list);
        });
    }

    /** Linhas evolutivas agrupadas pela chave antes do "=". */
    public static Map<String, List<Family>> keyed(ModConfigSpec.ConfigValue<List<? extends String>> value) {
        return cached(value, raw -> {
            Map<String, List<Family>> map = new LinkedHashMap<>();
            for (String entry : raw) {
                if (entry.isBlank()) continue;
                int eq = entry.indexOf('=');
                if (eq <= 0) {
                    warn(value, entry, "faltou a chave antes do '=' / missing 'key =' prefix");
                    continue;
                }
                String key = entry.substring(0, eq).trim().toLowerCase();
                for (String part : entry.substring(eq + 1).split(",")) {
                    if (part.isBlank()) continue;
                    Family family = family(part, value);
                    if (family != null) map.computeIfAbsent(key, k -> new ArrayList<>()).add(family);
                }
            }
            map.replaceAll((k, v) -> List.copyOf(v));
            return Map.copyOf(map);
        });
    }

    private static Family family(String entry, ModConfigSpec.ConfigValue<?> value) {
        String text = entry.trim().toLowerCase();
        int weight = DEFAULT_WEIGHT;
        int colon = text.lastIndexOf(':');
        if (colon >= 0) {
            try {
                weight = Integer.parseInt(text.substring(colon + 1).trim());
            } catch (NumberFormatException e) {
                warn(value, entry, "peso inválido / invalid weight");
                return null;
            }
            text = text.substring(0, colon).trim();
        }
        String[] parts = text.split(">");
        String base = parts[0].trim();
        String evolved = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : null;
        if (base.isEmpty() || weight <= 0 || parts.length > 2) {
            warn(value, entry, "formato esperado 'espécie>evolução:peso' / expected 'species>evolution:weight'");
            return null;
        }
        return new Family(base, evolved, weight);
    }

    /** Sorteia uma linha evolutiva pelos pesos (null se a lista estiver vazia). */
    public static Family pick(List<Family> options, RandomSource random) {
        int total = options.stream().mapToInt(Family::weight).sum();
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        for (Family family : options) {
            roll -= family.weight();
            if (roll < 0) return family;
        }
        return options.get(0);
    }

    // ----- Mobs -----

    /** Quais mobs a lista pega: ids e tags, menos os marcados com "-". */
    public record MobMatcher(Set<EntityType<?>> types, Set<TagKey<EntityType<?>>> tags,
                             Set<EntityType<?>> excludedTypes, Set<TagKey<EntityType<?>>> excludedTags) {
        public boolean matches(EntityType<?> type) {
            if (excludedTypes.contains(type) || excludedTags.stream().anyMatch(type::is)) return false;
            return types.contains(type) || tags.stream().anyMatch(type::is);
        }
    }

    public static MobMatcher mobs(ModConfigSpec.ConfigValue<List<? extends String>> value) {
        return cached(value, raw -> {
            Set<EntityType<?>> types = new HashSet<>(), excludedTypes = new HashSet<>();
            Set<TagKey<EntityType<?>>> tags = new HashSet<>(), excludedTags = new HashSet<>();
            for (String entry : raw) {
                if (entry.isBlank()) continue;
                String text = entry.trim();
                boolean exclude = text.startsWith("-");
                if (exclude) text = text.substring(1).trim();
                boolean tag = text.startsWith("#");
                if (tag) text = text.substring(1).trim();
                ResourceLocation id = ResourceLocation.tryParse(text);
                if (id == null) {
                    warn(value, entry, "id inválido / invalid id");
                } else if (tag) {
                    (exclude ? excludedTags : tags).add(TagKey.create(Registries.ENTITY_TYPE, id));
                } else {
                    // Mob de um mod que não está instalado: só ignora.
                    BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(type -> (exclude ? excludedTypes : types).add(type));
                }
            }
            return new MobMatcher(Set.copyOf(types), Set.copyOf(tags), Set.copyOf(excludedTypes), Set.copyOf(excludedTags));
        });
    }

    // ----- Blocos -----

    /** Um grupo de blocos com nome (ex.: "heat") e quantos blocos dele bastam para contar inteiro. */
    public record BlockGroup(String name, int target, Set<Block> blocks, Set<TagKey<Block>> tags) {
        public boolean matches(BlockState state) {
            return blocks.contains(state.getBlock()) || tags.stream().anyMatch(state::is);
        }
    }

    /**
     * Grupos de blocos: {@code "heat:6 = minecraft:lava, #minecraft:campfires"} (sem o número, basta 8 blocos).
     * Grupos que vêm por padrão e não estão na lista (ex.: um grupo novo numa atualização, num arquivo de
     * configuração antigo) entram também, para as farms e os gostos que dependem deles não pararem.
     */
    public static List<BlockGroup> blockGroups(ModConfigSpec.ConfigValue<List<? extends String>> value) {
        return cached(value, raw -> {
            List<BlockGroup> groups = new ArrayList<>(parseBlockGroups(raw, value));
            Set<String> names = new HashSet<>();
            for (BlockGroup g : groups) names.add(g.name());
            for (BlockGroup g : parseBlockGroups(value.getDefault(), value)) {
                if (names.add(g.name())) groups.add(g);
            }
            return List.copyOf(groups);
        });
    }

    private static List<BlockGroup> parseBlockGroups(List<? extends String> raw, ModConfigSpec.ConfigValue<?> value) {
        {
            List<BlockGroup> groups = new ArrayList<>();
            for (String entry : raw) {
                if (entry.isBlank()) continue;
                int eq = entry.indexOf('=');
                if (eq <= 0) {
                    warn(value, entry, "formato esperado 'nome:quantidade = blocos' / expected 'name:amount = blocks'");
                    continue;
                }
                String head = entry.substring(0, eq).trim().toLowerCase();
                int target = 8;
                int colon = head.indexOf(':');
                if (colon >= 0) {
                    try {
                        target = Math.max(1, Integer.parseInt(head.substring(colon + 1).trim()));
                    } catch (NumberFormatException e) {
                        warn(value, entry, "quantidade inválida / invalid amount");
                        continue;
                    }
                    head = head.substring(0, colon).trim();
                }
                Set<Block> blocks = new HashSet<>();
                Set<TagKey<Block>> tags = new HashSet<>();
                for (String part : entry.substring(eq + 1).split(",")) {
                    String text = part.trim();
                    if (text.isEmpty()) continue;
                    boolean tag = text.startsWith("#");
                    ResourceLocation id = ResourceLocation.tryParse(tag ? text.substring(1) : text);
                    if (id == null) {
                        warn(value, part, "id inválido / invalid id");
                    } else if (tag) {
                        tags.add(TagKey.create(Registries.BLOCK, id));
                    } else {
                        BuiltInRegistries.BLOCK.getOptional(id).ifPresent(blocks::add);
                    }
                }
                groups.add(new BlockGroup(head, target, Set.copyOf(blocks), Set.copyOf(tags)));
            }
            return groups;
        }
    }

    private static void warn(ModConfigSpec.ConfigValue<?> value, String entry, String problem) {
        LOGGER.warn("[{}] config {}: entrada ignorada / ignored entry \"{}\": {}",
                CobblemonEcosystems.MOD_ID, String.join(".", value.getPath()), entry, problem);
    }
}
