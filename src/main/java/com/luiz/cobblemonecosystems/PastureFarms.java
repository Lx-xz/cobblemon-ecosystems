package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.block.PastureBlock;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Gender;
import com.luiz.cobblemonecosystems.ConfigLists.BlockGroup;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Farms passivas do pasto: alguns Pokémon soltam um item de tempos em tempos, sem o jogador fazer nada, desde que
 * estejam confortáveis no {@link Habitat} (e, se a farm pedir, pisando num grupo de blocos). Felizes produzem mais
 * rápido. As farms vêm da lista {@code pasture_farms} da configuração, por exemplo:
 * {@code "type:fire = minecraft:blaze_powder, every:10, on:heat"} (quem = item, a cada X minutos, pisando em) ou
 * {@code "bibarel = from:biome_log, every:15, near:water, near:trees"} (a madeira do bioma, se o habitat tiver água e
 * árvores). Quem pode ser uma espécie ("bibarel") ou um tipo ("type:fire"). O Pokémon não quebra nada: só solta o item,
 * ou o põe num container encostado no bloco do pasto ({@link #deliver}).
 *
 * Não repetimos o que o Cobblemon 1.8 já dá por interação com item na mão (escova, balde, farinha de osso...).
 */
public final class PastureFarms {
    /** De quanto em quanto tempo cada Pokémon confere a farm (5 s). */
    private static final int CHECK_TICKS = 100;
    /** Feliz produz nesse tempo em relação ao normal. */
    private static final float HAPPY_SPEED = 0.6f;
    private static final String NEXT_KEY = CobblemonEcosystems.MOD_ID + ":farm_next";
    /** No Pokémon (não na entidade), para valer de novo quando ele voltar ao pasto: o dono desligou a coleta. */
    private static final String OFF_KEY = CobblemonEcosystems.MOD_ID + ":farm_off";

    /** Item "da floresta": a madeira do bioma do pasto, pela lista {@code biome_logs}. */
    private static final String BIOME_LOG = "from:biome_log";

    /**
     * Uma farm da configuração. {@code item} é null quando vem do bioma ({@link #BIOME_LOG}); {@code on} é null se
     * não pede bloco; {@code near} são os grupos que o habitat precisa ter.
     */
    public record Farm(String who, Item item, int count, int minutes, String on, List<String> near) {
        boolean matches(PokemonEntity pokemon) {
            if (who.startsWith("type:")) {
                for (ElementalType type : pokemon.getPokemon().getTypes()) {
                    if (type.getName().equalsIgnoreCase(who.substring(5))) return true;
                }
                return false;
            }
            return who.equals(PokemonAi.species(pokemon));
        }
    }

    /** Por que a farm está parada (ou READY se está produzindo). OFF: o dono desligou a coleta (botão de cesta). */
    public enum Status { READY, OFF, NOT_COMFORTABLE, NOT_NEAR, NOT_ON_BLOCK, NOT_IN_FOREST }

    /**
     * A farm de um Pokémon, como ela está, o que falta ({@code missing}: o grupo de near/on), o item que sai ali
     * (null fora de floresta) e quantos minutos faltam para o próximo.
     */
    public record State(Farm farm, Status status, String missing, Item item, int minutesLeft, int minutes) {}

    private PastureFarms() {}

    /** As farms da configuração. Entradas com erro são ignoradas (com aviso no log). */
    public static List<Farm> farms() {
        return ConfigLists.cachedList(Config.PASTURE_FARMS, raw -> {
            List<Farm> list = new ArrayList<>();
            for (String entry : raw) {
                Farm farm = parse(entry);
                if (farm != null) list.add(farm);
            }
            return List.copyOf(list);
        });
    }

    private static Farm parse(String entry) {
        if (entry.isBlank()) return null;
        int eq = entry.indexOf('=');
        if (eq <= 0) return ConfigLists.invalid(Config.PASTURE_FARMS, entry, "faltou 'quem =' / missing 'who ='");
        String who = entry.substring(0, eq).trim().toLowerCase();
        String[] parts = entry.substring(eq + 1).split(",");
        String[] itemPart = parts[0].trim().split("\\s+x", 2);
        Item item = null;
        if (!itemPart[0].trim().equalsIgnoreCase(BIOME_LOG)) {
            ResourceLocation itemId = ResourceLocation.tryParse(itemPart[0].trim());
            item = itemId == null ? Items.AIR : BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) return ConfigLists.invalid(Config.PASTURE_FARMS, entry, "item desconhecido / unknown item");
        }
        int count = 1, minutes = 10;
        String on = null;
        List<String> near = new ArrayList<>();
        try {
            if (itemPart.length > 1) count = Math.max(1, Integer.parseInt(itemPart[1].trim()));
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i].trim().toLowerCase();
                if (part.startsWith("every:")) minutes = Math.max(1, Integer.parseInt(part.substring(6).trim()));
                else if (part.startsWith("on:")) on = part.substring(3).trim();
                else if (part.startsWith("near:")) near.add(part.substring(5).trim());
                else if (!part.isEmpty()) return ConfigLists.invalid(Config.PASTURE_FARMS, entry, "parte desconhecida / unknown part: " + part);
            }
        } catch (NumberFormatException e) {
            return ConfigLists.invalid(Config.PASTURE_FARMS, entry, "número inválido / invalid number");
        }
        return new Farm(who, item, count, minutes, on, List.copyOf(near));
    }

    /** A primeira farm da lista que vale para este Pokémon, ou null. */
    public static Farm farmOf(PokemonEntity pokemon) {
        for (Farm farm : farms()) {
            if (farm.matches(pokemon)) return farm;
        }
        return null;
    }

    /** Como está a farm deste Pokémon do pasto agora (null se ele não tem farm). */
    public static State state(PokemonEntity pokemon, Habitat.Report report) {
        Farm farm = farmOf(pokemon);
        if (farm == null) return null;
        Status status = Status.READY;
        String missing = null;
        Item item = farm.item() != null ? farm.item() : biomeLog(pokemon);
        if (isOff(pokemon)) {
            status = Status.OFF;
        } else if (report == null || report.comfort().ordinal() < Habitat.Comfort.COMFORTABLE.ordinal()) {
            status = Status.NOT_COMFORTABLE;
        } else if ((missing = missingNear(pokemon, farm)) != null) {
            status = Status.NOT_NEAR;
        } else if (farm.on() != null && !standsOn(pokemon, farm.on())) {
            status = Status.NOT_ON_BLOCK;
            missing = farm.on();
        } else if (item == null) {
            status = Status.NOT_IN_FOREST;
        }
        int minutes = report != null && report.comfort() == Habitat.Comfort.HAPPY
                ? Math.max(1, Math.round(farm.minutes() * HAPPY_SPEED)) : farm.minutes();
        long next = pokemon.getPersistentData().getLong(NEXT_KEY);
        long left = next == 0 ? minutes * 1200L : next - pokemon.level().getGameTime();
        return new State(farm, status, missing, item, (int) Math.max(0, (left + 1199) / 1200), minutes);
    }

    /**
     * Este Pokémon do pasto produz algo que o botão de cesta controla: uma farm daqui ou ovos (fêmeas das espécies de
     * {@code egg_laying_species}, em {@link StructurePokemon}).
     */
    public static boolean collects(PokemonEntity pokemon) {
        if (pokemon.getTethering() == null) return false;
        if (Config.PASTURE_FARMS_ON.get() && farmOf(pokemon) != null) return true;
        return Config.EGG_LAYERS.get() && pokemon.getPokemon().getGender() == Gender.FEMALE
                && ConfigLists.species(Config.EGG_LAYING_SPECIES).contains(PokemonAi.species(pokemon));
    }

    /** O dono desligou a coleta deste Pokémon? */
    public static boolean isOff(PokemonEntity pokemon) {
        return pokemon.getPokemon().getPersistentData().getBoolean(OFF_KEY);
    }

    /**
     * Liga ou desliga a coleta. Ao religar, a contagem recomeça do zero (o item não sai na hora só porque o tempo
     * correu enquanto estava desligada).
     */
    public static void setOff(PokemonEntity pokemon, boolean off) {
        if (off) pokemon.getPokemon().getPersistentData().putBoolean(OFF_KEY, true);
        else pokemon.getPokemon().getPersistentData().remove(OFF_KEY);
        pokemon.getPersistentData().remove(NEXT_KEY);
        pokemon.getPokemon().onChange(null);
    }

    /** O primeiro grupo de {@code near} que o habitat não tem (ao menos 1/4 do que conta inteiro), ou null. */
    private static String missingNear(PokemonEntity pokemon, Farm farm) {
        if (farm.near().isEmpty() || !(pokemon.level() instanceof ServerLevel level) || pokemon.getTethering() == null) return null;
        Habitat.Survey survey = Habitat.survey(level, pokemon.getTethering().getPasturePos());
        for (String group : farm.near()) {
            int target = 8;
            for (BlockGroup g : ConfigLists.blockGroups(Config.HABITAT_FEATURES)) {
                if (g.name().equals(group)) target = g.target();
            }
            if (survey.counts().getOrDefault(group, 0) < Math.max(1, (target + 3) / 4)) return group;
        }
        return null;
    }

    /** A madeira do bioma do pasto (lista {@code biome_logs}), ou null fora dos biomas de floresta. */
    public static Item biomeLog(PokemonEntity pokemon) {
        BlockPos pos = pokemon.getTethering() != null ? pokemon.getTethering().getPasturePos() : pokemon.blockPosition();
        Holder<Biome> biome = pokemon.level().getBiome(pos);
        for (String entry : Config.BIOME_LOGS.get()) {
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            String key = entry.substring(0, eq).trim();
            boolean tag = key.startsWith("#");
            ResourceLocation id = ResourceLocation.tryParse(tag ? key.substring(1) : key);
            if (id == null) continue;
            boolean matches = tag ? biome.is(TagKey.create(Registries.BIOME, id)) : biome.is(id);
            if (!matches) continue;
            ResourceLocation itemId = ResourceLocation.tryParse(entry.substring(eq + 1).trim());
            Item item = itemId == null ? Items.AIR : BuiltInRegistries.ITEM.get(itemId);
            return item == Items.AIR ? null : item;
        }
        return null;
    }

    /** O bloco embaixo do Pokémon é do grupo (de {@code habitat_features})? */
    private static boolean standsOn(PokemonEntity pokemon, String group) {
        for (BlockGroup g : ConfigLists.blockGroups(Config.HABITAT_FEATURES)) {
            if (g.name().equals(group)) {
                return g.matches(pokemon.getBlockStateOn()) || g.matches(pokemon.level().getBlockState(pokemon.blockPosition()));
            }
        }
        return false;
    }

    /**
     * Entrega o que o Pokémon do pasto produziu: vai para um container encostado no bloco do pasto (baú, barril ou
     * de outro mod, pelo item handler do NeoForge); o que não couber, ou sem container, cai no chão perto dele.
     */
    public static void deliver(PokemonEntity pokemon, ItemStack stack) {
        if (pokemon.getTethering() != null) {
            Level level = pokemon.level();
            BlockPos pasture = pokemon.getTethering().getPasturePos();
            BlockState state = level.getBlockState(pasture);
            BlockPos other = state.getBlock() instanceof PastureBlock block
                    ? block.getPositionOfOtherPart(state, pasture) : pasture.above();
            for (BlockPos part : List.of(pasture, other)) {
                for (Direction dir : Direction.values()) {
                    BlockPos pos = part.relative(dir);
                    if (pos.equals(pasture) || pos.equals(other)) continue;
                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir.getOpposite());
                    if (handler != null) stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);
                    if (stack.isEmpty()) return;
                }
            }
        }
        pokemon.spawnAtLocation(stack);
    }

    /** Chamado pelo {@link PastureReturn} a cada tick de cada Pokémon do pasto (dentro da área). */
    static void tick(PokemonEntity pokemon, ServerLevel level) {
        if (!Config.PASTURE_FARMS_ON.get() || (level.getGameTime() + pokemon.getId()) % CHECK_TICKS != 0 || pokemon.isBusy()) return;
        Habitat.Report report = Habitat.report(pokemon);
        State state = state(pokemon, report);
        if (state == null) return;

        long now = level.getGameTime();
        var data = pokemon.getPersistentData();
        if (!data.contains(NEXT_KEY)) data.putLong(NEXT_KEY, now + state.minutes() * 1200L);
        // Parada: o tempo continua correndo, e o item sai assim que as condições voltarem.
        if (now < data.getLong(NEXT_KEY) || state.status() != Status.READY) return;

        Farm farm = state.farm();
        deliver(pokemon, new ItemStack(state.item(), farm.count()));
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pokemon.getX(), pokemon.getY() + pokemon.getBbHeight() * 0.6, pokemon.getZ(),
                6, pokemon.getBbWidth() / 2, pokemon.getBbHeight() / 3, pokemon.getBbWidth() / 2, 0);
        level.playSound(null, pokemon.getX(), pokemon.getY(), pokemon.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4f, 0.8f);
        data.putLong(NEXT_KEY, now + state.minutes() * 1200L);
    }
}
