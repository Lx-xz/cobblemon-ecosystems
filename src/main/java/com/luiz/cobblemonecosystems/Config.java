package com.luiz.cobblemonecosystems;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Arquivo de configuração: config/cobblemon_ecosystems-common.toml.
 * Os valores são lidos na hora em que cada coisa acontece, então a maioria das mudanças vale sem reiniciar.
 * A exceção é a remoção dos spawns naturais nos biomas, que é aplicada quando o mundo abre.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ----- Remoção -----
    public static final ModConfigSpec.BooleanValue REMOVE_PASSIVE_ANIMALS;
    public static final ModConfigSpec.BooleanValue REMOVE_HOSTILE_ANIMALS;
    public static final ModConfigSpec.BooleanValue REMOVE_SLIMES;
    public static final ModConfigSpec.BooleanValue REMOVE_FROM_EXISTING_WORLDS;
    // ----- Vilas -----
    public static final ModConfigSpec.BooleanValue STRUCTURE_POKEMON;
    public static final ModConfigSpec.DoubleValue STRUCTURE_EVOLVED_CHANCE;
    public static final ModConfigSpec.BooleanValue VILLAGE_GUARDIANS;
    public static final ModConfigSpec.BooleanValue VILLAGER_POKEMON;
    public static final ModConfigSpec.DoubleValue VILLAGER_ADOPT_CHANCE;
    public static final ModConfigSpec.BooleanValue VILLAGER_BATTLES;
    // ----- Mercador -----
    public static final ModConfigSpec.BooleanValue TRADER_POKEMON;
    public static final ModConfigSpec.BooleanValue TRADER_BATTLE;
    // ----- Comportamentos -----
    public static final ModConfigSpec.BooleanValue HIVE_POKEMON;
    public static final ModConfigSpec.BooleanValue DOG_HUNTERS;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("Which vanilla mobs are removed. Natural spawn changes apply when the world is (re)opened.",
                "Quais mobs vanilla são removidos. Mudanças nos spawns naturais valem ao (re)abrir o mundo.").push("removal");
        REMOVE_PASSIVE_ANIMALS = BUILDER.comment(
                "Passive and neutral animals: cows, pigs, sheep, horses, cats, wolves, fish, bees, bats...",
                "Edit the list with the tag cobblemon_ecosystems:removed_animals.",
                "Animais passivos e neutros. Lista editável pela tag cobblemon_ecosystems:removed_animals.")
                .define("remove_passive_animals", true);
        REMOVE_HOSTILE_ANIMALS = BUILDER.comment(
                "Hostile animals with a Pokémon counterpart: spiders, cave spiders and hoglins.",
                "Tag: cobblemon_ecosystems:removed_hostile_animals.",
                "Animais hostis com contraparte Pokémon: aranhas, aranhas de caverna e hoglins.")
                .define("remove_hostile_animals", true);
        REMOVE_SLIMES = BUILDER.comment(
                "Slimes and magma cubes. Grimer, Goomy and Solosis drop slime balls; Slugma, Magcargo, Numel and",
                "Camerupt drop magma cream. Tag: cobblemon_ecosystems:removed_slimes.",
                "Slimes e magma cubes. Pokémon já dão slimeball e creme de magma.")
                .define("remove_slimes", false);
        REMOVE_FROM_EXISTING_WORLDS = BUILDER.comment(
                "Also remove matching mobs already saved in the world when their chunk loads (tamed pets included).",
                "Também remove os mobs que já estavam salvos no mundo quando o chunk carrega (inclusive pets domados).")
                .define("remove_from_existing_worlds", true);
        BUILDER.pop();

        BUILDER.comment("Villages / Vilas").push("villages");
        STRUCTURE_POKEMON = BUILDER.comment(
                "Animals placed by structures (village pens and stables, desert camels, witch hut cat) become Pokémon.",
                "Animais colocados por estruturas viram Pokémon.")
                .define("structure_pokemon", true);
        STRUCTURE_EVOLVED_CHANCE = BUILDER.comment(
                "Chance for each structure Pokémon to be an evolved form. / Chance de vir evoluído.")
                .defineInRange("structure_evolved_chance", 0.2, 0.0, 1.0);
        VILLAGE_GUARDIANS = BUILDER.comment(
                "Golett and Golurk replace iron golems (also stops building iron golems with a pumpkin).",
                "Golett e Golurk no lugar do golem de ferro (e o golem de ferro não pode mais ser montado).")
                .define("village_guardians", true);
        VILLAGER_POKEMON = BUILDER.comment(
                "Villagers from newly generated villages may adopt a Pokémon matching their profession.",
                "Aldeões de vilas novas podem adotar um Pokémon da profissão.")
                .define("villager_pokemon", true);
        VILLAGER_ADOPT_CHANCE = BUILDER.comment(
                "Chance for each of those villagers to adopt a Pokémon. / Chance de cada aldeão adotar.")
                .defineInRange("villager_adopt_chance", 0.5, 0.0, 1.0);
        VILLAGER_BATTLES = BUILDER.comment(
                "Players can battle villagers that have Pokémon (sneak + right-click, or send out a Pokémon at them).",
                "Winning gives trade discounts; villagers without a profession pay in emeralds.",
                "Jogadores podem batalhar com aldeões que têm Pokémon. Vencer dá desconto nas trocas (ou esmeraldas).")
                .define("villager_battles", true);
        BUILDER.pop();

        BUILDER.comment("Wandering Trader / Mercador ambulante").push("wandering_trader");
        TRADER_POKEMON = BUILDER.comment(
                "The Wandering Trader arrives with two Mudbray wearing his blanket.",
                "O mercador chega com dois Mudbray com a manta dele.")
                .define("trader_pokemon", true);
        TRADER_BATTLE = BUILDER.comment(
                "Players can battle the trader and win one of his Mudbray.",
                "Jogadores podem batalhar com o mercador e ganhar um dos Mudbray.")
                .define("trader_battle", true);
        BUILDER.pop();

        BUILDER.comment("Behaviours / Comportamentos").push("behaviours");
        HIVE_POKEMON = BUILDER.comment(
                "Combee, Vespiquen, Cutiefly and Ribombee live in beehives instead of bees.",
                "Pokémon abelha moram nas colmeias no lugar das abelhas.")
                .define("hive_pokemon", true);
        DOG_HUNTERS = BUILDER.comment(
                "Dog Pokémon hunt skeletons; wild ones sometimes hunt wild sheep Pokémon.",
                "Pokémon cachorro caçam esqueletos; os selvagens às vezes caçam Pokémon ovelha.")
                .define("dog_hunters", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private Config() {}
}
