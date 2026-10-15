package com.luiz.cobblemonecosystems;

import java.util.List;

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
    public static final ModConfigSpec.BooleanValue SILVERFISH_DWEBBLE;
    public static final ModConfigSpec.BooleanValue SPIDER_SPAWNER_NESTS;
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
    // ----- Saqueadores e bruxas -----
    public static final ModConfigSpec.BooleanValue WITCH_PET;
    public static final ModConfigSpec.BooleanValue WITCH_BATTLE;
    public static final ModConfigSpec.BooleanValue OUTPOST_CAPTIVES;
    // ----- Pasto -----
    public static final ModConfigSpec.BooleanValue PASTURE_RETURN;
    public static final ModConfigSpec.BooleanValue PASTURE_BEES;
    public static final ModConfigSpec.BooleanValue EGG_LAYERS;
    // ----- Comportamentos -----
    public static final ModConfigSpec.BooleanValue HIVE_POKEMON;
    public static final ModConfigSpec.BooleanValue DOG_HUNTERS;

    // ----- Listas editáveis (formatos em ConfigLists) -----
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PASSIVE_ANIMALS_LIST, HOSTILE_ANIMALS_LIST, SLIMES_LIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> INFESTED_POKEMON, SPIDER_NEST_POKEMON;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> PEN_HORSE, PEN_DONKEY, PEN_MULE, PEN_CAMEL, PEN_COW,
            PEN_MOOSHROOM, PEN_PIG, PEN_SHEEP, PEN_CHICKEN, PEN_CAT;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VILLAGER_PARTNERS, HEAD_RIDERS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WITCH_BATTLE_TEAM, OUTPOST_CAPTIVE_POKEMON;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EGG_LAYING_SPECIES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DOG_SPECIES, WOLF_SPECIES, SHEEP_SPECIES;
    // ----- Habitat do pasto -----
    public static final ModConfigSpec.BooleanValue HABITAT;
    public static final ModConfigSpec.IntValue HABITAT_RADIUS;
    public static final ModConfigSpec.EnumValue<Habitat.TypeRule> HABITAT_TYPE_RULE;
    public static final ModConfigSpec.BooleanValue HABITAT_PARTICLES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HABITAT_FEATURES, HABITAT_PREFERENCES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HOMEBODY_NATURES, EXPLORER_NATURES;

    public static final ModConfigSpec SPEC;

    private static final String SPECIES_FORMAT = "Format / Formato: \"species:weight\" (weight optional / peso opcional, default 10).";
    private static final String FAMILY_FORMAT =
            "Format / Formato: \"base>evolved:weight\" (evolved and weight optional / evolução e peso opcionais, peso padrão 10).";
    private static final String PEN_FORMAT = "Format / Formato: \"village = base>evolved:weight, ...\". Village types / tipos de vila: "
            + "plains, desert, savanna, snowy, taiga, default (any other place / qualquer outro lugar).";
    private static final String MOB_FORMAT =
            "Mob ids (\"minecraft:fox\"), tags (\"#minecraft:tag\") and exclusions (\"-minecraft:fox\") / ids, tags e exceções.";

    private static ModConfigSpec.ConfigValue<List<? extends String>> list(String key, List<String> defaults, String... comment) {
        return BUILDER.comment(comment).defineListAllowEmpty(key, defaults, () -> "", ConfigLists::validEntry);
    }

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
        SILVERFISH_DWEBBLE = BUILDER.comment(
                "Silverfish become angry Dwebble: they come out of infested blocks, attack, and call the others hiding nearby.",
                "Spawners of silverfish are replaced by a few Dwebble.",
                "Traças viram Dwebble bravos: saem dos blocos infestados, atacam e chamam os outros escondidos por perto.")
                .define("silverfish_dwebble", true);
        SPIDER_SPAWNER_NESTS = BUILDER.comment(
                "Spider spawners (mineshafts, dungeons) are removed and leave a nest of a few spider Pokémon of one species.",
                "Only while hostile animals are removed. No endless spider Pokémon farms.",
                "Spawners de aranha viram um ninho com alguns Pokémon aranha de uma espécie (sem farm infinita).")
                .define("spider_spawner_nests", true);
        PASSIVE_ANIMALS_LIST = list("passive_animals_list", List.of("#cobblemon_ecosystems:removed_animals"),
                "Mobs removed by remove_passive_animals. / Mobs removidos por remove_passive_animals.", MOB_FORMAT);
        HOSTILE_ANIMALS_LIST = list("hostile_animals_list", List.of("#cobblemon_ecosystems:removed_hostile_animals"),
                "Mobs removed by remove_hostile_animals. / Mobs removidos por remove_hostile_animals.", MOB_FORMAT);
        SLIMES_LIST = list("slimes_list", List.of("#cobblemon_ecosystems:removed_slimes"),
                "Mobs removed by remove_slimes. / Mobs removidos por remove_slimes.", MOB_FORMAT);
        INFESTED_POKEMON = list("infested_pokemon", List.of("dwebble>crustle"),
                "Pokémon hiding in infested blocks and in silverfish spawner nests (needs silverfish_dwebble).",
                "Pokémon escondidos nos blocos infestados e nos ninhos dos spawners de traça.", FAMILY_FORMAT);
        SPIDER_NEST_POKEMON = list("spider_nest_pokemon", List.of("spinarak>ariados:40", "joltik>galvantula:30", "tarountula>spidops:30"),
                "Pokémon of the spider spawner nests (one species per nest). / Pokémon dos ninhos de aranha (uma espécie por ninho).",
                FAMILY_FORMAT);
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
        VILLAGER_PARTNERS = list("villager_partners", List.of(
                        "armorer = aron:20, lairon:10", "weaponsmith = honedge:35, *:65", "toolsmith = klefki, roggenrola",
                        "mason = timburr", "fletcher = rowlet", "farmer = bulbasaur, oddish, skiddo", "fisherman = wingull",
                        "shepherd = wooloo, mareep", "cleric = chansey, audino", "librarian = abra, natu",
                        "cartographer = hoothoot, pidgey", "leatherworker = smeargle", "butcher = farfetchd",
                        "nitwit = slowpoke, psyduck", "none = slowpoke, psyduck", "baby = pichu, togepi", "any = pidgey, pichu"),
                "Villager partners by profession: \"profession = species:weight, ...\". \"baby\" is for baby villagers,",
                "\"any\" is the pool any villager may get (20% of the time), and \"*\" inside a profession means \"pick from any\".",
                "Pokémon dos aldeões por profissão. \"baby\" = aldeão criança, \"any\" = qualquer aldeão (20% das vezes),",
                "\"*\" = sortear do \"any\".");
        HEAD_RIDERS = list("head_riders", List.of("pichu", "pidgey", "natu", "rowlet", "togepi"),
                "Small partners that ride on the villager's head. / Pokémon pequenos que andam na cabeça do aldeão.");
        BUILDER.pop();

        BUILDER.comment("Pokémon in place of structure animals, by village type. One family is drawn per village and animal.",
                "Pokémon no lugar dos animais das estruturas, por tipo de vila. Uma família é sorteada por vila e animal.",
                PEN_FORMAT).push("village_pens");
        PEN_HORSE = list("horse", List.of("plains = mudbray>mudsdale", "desert = ponyta>rapidash", "savanna = blitzle>zebstrika",
                "snowy = stantler>wyrdeer", "taiga = deerling>sawsbuck", "default = ponyta>rapidash, mudbray>mudsdale"));
        PEN_DONKEY = list("donkey", List.of("default = mudbray>mudsdale"));
        PEN_MULE = list("mule", List.of("default = mudbray>mudsdale"));
        PEN_CAMEL = list("camel", List.of("default = numel>camerupt"));
        PEN_COW = list("cow", List.of("plains = miltank:7, tauros:3", "desert = tauros", "savanna = bouffalant:6, tauros:4",
                "snowy = miltank", "taiga = miltank:6, bouffalant:4", "default = miltank:7, tauros:3"));
        PEN_MOOSHROOM = list("mooshroom", List.of("default = miltank"));
        PEN_PIG = list("pig", List.of("plains = lechonk>oinkologne", "desert = spoink>grumpig", "savanna = tepig>pignite",
                "snowy = swinub>piloswine", "taiga = lechonk>oinkologne:6, swinub>piloswine:4", "default = lechonk>oinkologne"));
        PEN_SHEEP = list("sheep", List.of("plains = wooloo>dubwool:6, mareep>flaaffy:4", "desert = mareep>flaaffy",
                "savanna = mareep>flaaffy", "snowy = wooloo>dubwool", "taiga = wooloo>dubwool:6, skiddo>gogoat:4",
                "default = wooloo>dubwool:6, mareep>flaaffy:4"));
        PEN_CHICKEN = list("chicken", List.of("plains = doduo>dodrio:45, psyduck>golduck:45, torchic>combusken:10",
                "desert = doduo>dodrio:90, torchic>combusken:10", "savanna = doduo>dodrio:55, psyduck>golduck:40, torchic>combusken:5",
                "snowy = psyduck>golduck:50, delibird:45, piplup>prinplup:5", "taiga = psyduck>golduck:55, doduo>dodrio:45",
                "default = doduo>dodrio:45, psyduck>golduck:45, torchic>combusken:10"));
        PEN_CAT = list("cat", List.of("desert = meowth>persian:7, espurr>meowstic:3", "savanna = litleo>pyroar:6, shinx>luxio:4",
                "snowy = glameow>purugly:6, sneasel>weavile:4", "taiga = purrloin>liepard:4, glameow>purugly:3, skitty>delcatty:3",
                "default = meowth>persian:4, skitty>delcatty:3, purrloin>liepard:2, espurr>meowstic:1"));
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

        BUILDER.comment("Illagers and witches / Saqueadores e bruxas").push("illagers");
        WITCH_PET = BUILDER.comment(
                "The swamp hut witch keeps the black Purrloin as her pet. She is neutral: she only attacks who attacks her.",
                "If she dies, the Purrloin runs away. It can't be caught: battle the witch to win it.",
                "A bruxa da cabana fica com o Purrloin preto. Ela é neutra; se morrer, o Purrloin foge (incapturável).")
                .define("witch_pet", true);
        WITCH_BATTLE = BUILDER.comment(
                "Players can battle the swamp hut witch (sneak + right-click, or send out a Pokémon at her) and win her Purrloin.",
                "Jogadores podem batalhar com a bruxa da cabana e ganhar o Purrloin dela.")
                .define("witch_battle", true);
        WITCH_BATTLE_TEAM = list("witch_battle_team", List.of("murkrow", "misdreavus", "gastly"),
                "The witch fights with her Purrloin and one of these. / A bruxa batalha com o Purrloin e um destes.", SPECIES_FORMAT);
        OUTPOST_CAPTIVES = BUILDER.comment(
                "The iron golem cage of pillager outposts holds a captive Pokémon instead (needs village_guardians).",
                "A jaula do golem nos postos de saqueadores guarda um Pokémon preso (precisa de village_guardians).")
                .define("outpost_captives", true);
        OUTPOST_CAPTIVE_POKEMON = list("outpost_captive_pokemon", List.of("golett:25", "growlithe:15", "ponyta:12", "mudbray:12",
                        "riolu:10", "eevee:10", "larvitar:8", "absol:8"),
                "Pokémon that can be held captive in outposts. / Pokémon que podem estar presos nos postos.", SPECIES_FORMAT);
        BUILDER.pop();

        BUILDER.comment("Pasture Block / Bloco de pasto").push("pasture");
        PASTURE_RETURN = BUILDER.comment(
                "Pokémon that leave the pasture area walk back to it, instead of being sent back to the PC.",
                "Pokémon que saem da área do pasto voltam andando, em vez de irem para o PC.")
                .define("pasture_return", true);
        PASTURE_BEES = BUILDER.comment(
                "Bee Pokémon in a pasture use a crafted beehive inside the pasture area: they visit flowers, make honey and stay near it.",
                "Pokémon abelha no pasto usam uma colmeia craftada na área: visitam flores, fazem mel e ficam perto dela.")
                .define("pasture_bees", true);
        EGG_LAYERS = BUILDER.comment(
                "Female bird Pokémon that replace chickens (Doduo, Psyduck, Torchic, Delibird, Piplup and evolutions) lay eggs,",
                "in village pens and in a pasture. Males don't.",
                "Fêmeas das aves que substituem as galinhas botam ovos, nos currais e no pasto. Machos não.")
                .define("egg_layers", true);
        EGG_LAYING_SPECIES = list("egg_laying_species", List.of("doduo", "dodrio", "psyduck", "golduck", "torchic", "combusken",
                        "delibird", "piplup", "prinplup"),
                "Species whose females lay eggs (village pens and pastures). / Espécies cujas fêmeas botam ovos (currais e pasto).");
        BUILDER.pop();

        BUILDER.comment("Pasture habitats: the space Pokémon can reach around the Pasture Block, walls included, is their home.",
                "Each type likes and dislikes some surroundings; that sets how comfortable they are. Sneak and right-click a",
                "pastured Pokémon holding the Pokédex to see its habitat.",
                "Habitat do pasto: o espaço que os Pokémon alcançam em volta do bloco do pasto (as paredes contam) é a casa deles.",
                "Cada tipo gosta e não gosta de certas coisas em volta, e isso decide o conforto. Agache e clique com o botão",
                "direito num Pokémon do pasto segurando a Pokédex para ver o habitat dele.").push("habitat");
        HABITAT = BUILDER.comment("Turn habitats on. / Liga os habitats.").define("habitat", true);
        HABITAT_RADIUS = BUILDER.comment(
                "How far from the Pasture Block the habitat reaches (walls and fences stop it sooner).",
                "Até onde o habitat vai a partir do bloco do pasto (paredes e cercas fecham antes).")
                .defineInRange("habitat_radius", 10, 4, 24);
        HABITAT_TYPE_RULE = BUILDER.comment(
                "How Pokémon with two types are scored: BEST (happy if one type is happy), AVERAGE (both count the same)",
                "or PRIMARY (the first type counts 2/3). What either type dislikes always counts.",
                "Como contam os Pokémon de dois tipos: BEST (feliz se um dos tipos estiver feliz), AVERAGE (os dois contam igual)",
                "ou PRIMARY (o primeiro tipo conta 2/3). O que qualquer um dos tipos não gosta sempre atrapalha.")
                .defineEnum("habitat_type_rule", Habitat.TypeRule.BEST);
        HABITAT_PARTICLES = BUILDER.comment(
                "Happy Pokémon show green sparkles now and then; uncomfortable or lost ones shed tears.",
                "Pokémon felizes soltam brilhos verdes de vez em quando; desconfortáveis ou perdidos choram.")
                .define("habitat_particles", true);
        HABITAT_FEATURES = list("habitat_features", List.of(
                        "heat:6 = minecraft:lava, minecraft:magma_block, minecraft:fire, #minecraft:campfires, minecraft:netherrack",
                        "water:12 = minecraft:water, minecraft:bubble_column, minecraft:lily_pad, minecraft:seagrass, minecraft:tall_seagrass, "
                                + "minecraft:kelp, minecraft:kelp_plant, #minecraft:corals, #minecraft:coral_blocks",
                        "plants:12 = minecraft:grass_block, minecraft:short_grass, minecraft:tall_grass, minecraft:fern, minecraft:large_fern, "
                                + "#minecraft:flowers, #minecraft:leaves, #minecraft:crops, #minecraft:saplings, minecraft:moss_block, "
                                + "minecraft:moss_carpet",
                        "flowers:6 = #minecraft:flowers",
                        "metal:4 = minecraft:iron_block, minecraft:raw_iron_block, minecraft:iron_bars, minecraft:chain, #minecraft:anvil, "
                                + "minecraft:iron_door, minecraft:iron_trapdoor, minecraft:cauldron, minecraft:copper_block, minecraft:cut_copper",
                        "stone:16 = #minecraft:base_stone_overworld, minecraft:cobblestone, minecraft:mossy_cobblestone, #minecraft:stone_bricks, "
                                + "minecraft:gravel",
                        "earth:12 = #minecraft:dirt, #minecraft:sand, minecraft:gravel, minecraft:clay, #minecraft:terracotta, minecraft:dirt_path",
                        "cold:8 = minecraft:snow, minecraft:snow_block, minecraft:powder_snow, #minecraft:ice",
                        "electric:3 = minecraft:lightning_rod, minecraft:redstone_block, minecraft:redstone_lamp, minecraft:redstone_torch, "
                                + "minecraft:copper_block, minecraft:exposed_copper, minecraft:weathered_copper, minecraft:oxidized_copper",
                        "bugs:8 = #minecraft:flowers, #minecraft:logs, #minecraft:leaves, minecraft:cobweb, minecraft:moss_block",
                        "toxic:6 = minecraft:mud, minecraft:red_mushroom, minecraft:brown_mushroom, minecraft:red_mushroom_block, "
                                + "minecraft:brown_mushroom_block, minecraft:mushroom_stem, minecraft:mycelium, minecraft:slime_block",
                        "spooky:6 = minecraft:soul_sand, minecraft:soul_soil, minecraft:soul_fire, minecraft:soul_lantern, minecraft:soul_torch, "
                                + "minecraft:soul_campfire, #minecraft:candles, minecraft:cobweb",
                        "mystic:4 = minecraft:amethyst_block, minecraft:budding_amethyst, minecraft:amethyst_cluster, minecraft:bookshelf, "
                                + "minecraft:chiseled_bookshelf, minecraft:enchanting_table",
                        "fairy:6 = #minecraft:flowers, minecraft:amethyst_block, minecraft:cherry_leaves, minecraft:pink_petals, minecraft:cake",
                        "training:3 = minecraft:target, minecraft:hay_block",
                        "cozy:4 = minecraft:hay_block, #minecraft:wool, #minecraft:wool_carpets, #minecraft:beds",
                        "end:4 = minecraft:obsidian, minecraft:crying_obsidian, minecraft:end_stone, minecraft:end_stone_bricks, "
                                + "minecraft:purpur_block"),
                "Groups of blocks a habitat can have: \"name:amount = blocks and #tags\". The amount is how many blocks are enough",
                "to count in full (default 8). Blocks touching the habitat count: floor, walls, ceiling and what is inside it.",
                "Grupos de blocos de um habitat: \"nome:quantidade = blocos e #tags\". A quantidade é quantos blocos bastam para",
                "contar inteiro (padrão 8). Contam os blocos que tocam o habitat: chão, paredes, teto e o que está dentro.",
                "Built-in groups / grupos prontos: open_sky, roof, darkness, bright, cold_biome, hot_biome, rain.");
        HABITAT_PREFERENCES = list("habitat_preferences", List.of(
                        "normal = plants, cozy", "fire = heat:20, hot_biome:5, -water, -cold, -rain:5",
                        "water = water", "grass = plants:20, flowers:5, open_sky:10, -heat",
                        "electric = electric, -water", "ice = cold:20, cold_biome:10, -heat, -hot_biome",
                        "fighting = training, open_sky:5", "poison = toxic, -flowers:5", "ground = earth, -water",
                        "flying = open_sky", "psychic = mystic", "bug = bugs, -heat", "rock = stone, earth:5",
                        "ghost = spooky:20, darkness:10, -bright:5", "dragon = end, open_sky:5", "dark = darkness, -bright:5",
                        "steel = metal:20, stone:5, -water:5", "fairy = fairy, flowers:5",
                        "zubat = darkness:20, roof:10, -bright", "golbat = darkness:20, roof:10, -bright",
                        "woobat = darkness:20, roof:10, -bright", "swoobat = darkness:20, roof:10, -bright"),
                "What each type likes (and dislikes, with \"-\"): \"type = group:weight, -group:weight\". The weight is how",
                "much it matters (default 10). Pokémon with two types take the average.",
                "Do que cada tipo gosta (e não gosta, com \"-\"): \"tipo = grupo:peso, -grupo:peso\". O peso é o quanto importa",
                "(padrão 10). Pokémon com dois tipos fazem a média.",
                "A line for a species (\"zubat = ...\") replaces its types' lines. / Uma linha de espécie vale no lugar dos tipos dela.");
        HOMEBODY_NATURES = list("homebody_natures", List.of("timid", "relaxed", "calm", "careful", "gentle", "bashful", "quiet"),
                "Natures of homebodies: they barely leave the habitat. / Naturezas dos caseiros: quase não saem do habitat.");
        EXPLORER_NATURES = list("explorer_natures", List.of("jolly", "naive", "hasty", "rash", "naughty", "lax", "quirky"),
                "Natures of explorers: they roam far, but come home to sleep. The others go out now and then.",
                "Naturezas dos exploradores: passeiam longe, mas voltam para dormir. As outras saem de vez em quando.");
        BUILDER.pop();

        BUILDER.comment("Behaviours / Comportamentos").push("behaviours");
        HIVE_POKEMON = BUILDER.comment(
                "Combee, Vespiquen, Cutiefly and Ribombee live in beehives instead of bees.",
                "Pokémon abelha moram nas colmeias no lugar das abelhas.")
                .define("hive_pokemon", true);
        DOG_HUNTERS = BUILDER.comment(
                "Dog Pokémon hunt skeletons; wild wolf Pokémon sometimes hunt wild sheep Pokémon.",
                "Pokémon cachorro caçam esqueletos; os lobos selvagens às vezes caçam Pokémon ovelha.")
                .define("dog_hunters", true);
        DOG_SPECIES = list("dog_species", List.of("growlithe", "arcanine", "lillipup", "herdier", "stoutland", "rockruff", "lycanroc",
                        "poochyena", "mightyena", "houndour", "houndoom", "electrike", "manectric", "yamper", "boltund",
                        "maschiff", "mabosstiff", "snubbull", "granbull", "fidough", "dachsbun"),
                "Dog Pokémon that hunt skeletons. / Pokémon cachorro que caçam esqueletos.");
        WOLF_SPECIES = list("wolf_species", List.of("poochyena", "mightyena", "lycanroc"),
                "Wild wolf Pokémon that sometimes hunt sheep Pokémon. / Lobos selvagens que às vezes caçam Pokémon ovelha.");
        SHEEP_SPECIES = list("sheep_species", List.of("wooloo", "dubwool", "mareep"),
                "Sheep Pokémon hunted by wild wolves. / Pokémon ovelha caçados pelos lobos selvagens.");
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private Config() {}
}
