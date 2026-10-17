package com.luiz.cobblemonecosystems;

import java.util.List;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Nomes com ícone sobre a cabeça de quem tem Pokémon. Como todo nome no Minecraft, só aparece quando se olha para a
 * pessoa. O ícone também diz ao cliente que dá para desafiar ({@code ClientHints}).
 * <ul>
 *   <li>Todo aldeão ganha um primeiro nome ("Ash"); quem tem Pokémon ganha também a Pokébola antes do nome. O nome
 *       aparece na batalha e no chat no lugar da profissão.</li>
 *   <li>Mercador ambulante: mochila (+ estrela se for o shiny).</li>
 *   <li>Bruxa da cabana: chapéu.</li>
 * </ul>
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public final class NameTags {
    private static final ResourceLocation ICON_FONT = ResourceLocation.fromNamespaceAndPath(CobblemonEcosystems.MOD_ID, "icons");
    /** Caracteres da fonte de ícones (assets/cobblemon_ecosystems/font/icons.json). */
    public static final String POKEBALL = "", WITCH_HAT = "", BACKPACK = "", STAR = "";
    /** No aldeão: o primeiro nome sorteado. */
    private static final String NAME_KEY = CobblemonEcosystems.MOD_ID + ":name";
    private static final String KEY = CobblemonEcosystems.MOD_ID + ".name.";

    private NameTags() {}

    public static MutableComponent icon(String icon) {
        return Component.literal(icon).withStyle(style -> style.withFont(ICON_FONT));
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !Config.NAME_TAGS.get()) return;
        Entity entity = event.getEntity();
        if (entity instanceof Villager villager) {
            nameVillager(villager);
        } else if (entity instanceof PokemonEntity pokemon && pokemon.getPersistentData().hasUUID(VillagerCompanions.VILLAGER_KEY)) {
            clearOldPetName(pokemon);
        } else if (entity instanceof WanderingTrader trader && Config.TRADER_POKEMON.get()
                && (!trader.hasCustomName() || startsWithIcon(trader))) {
            MutableComponent name = Component.empty().append(icon(BACKPACK));
            if (TraderPokemon.isShinyTrader(trader.getUUID())) name.append(icon(STAR));
            trader.setCustomName(name.append(" ").append(Component.translatable(CobblemonEcosystems.MOD_ID + ".trader.name")));
            trader.setCustomNameVisible(false);
        } else if (entity instanceof Witch witch && Config.WITCH_PET.get() && HutWitch.isHutWitch(witch)
                && (!witch.hasCustomName() || startsWithIcon(witch))) {
            witch.setCustomName(Component.empty().append(icon(WITCH_HAT)).append(" ").append(Component.translatable(KEY + "witch")));
            witch.setCustomNameVisible(false);
        }
    }

    /** Chamado quando o aldeão adota um Pokémon: o nome ganha a Pokébola. */
    static void onAdopted(Villager villager, PokemonEntity pokemon) {
        if (Config.NAME_TAGS.get()) nameVillager(villager);
    }

    /** O primeiro nome do aldeão, ou vazio se ele não tem (ou foi batizado pelo jogador). */
    static String firstName(Villager villager) {
        return villager.getPersistentData().getString(NAME_KEY);
    }

    /** Sorteia o nome (uma vez) e monta "Ash", com a Pokébola se ele tem Pokémon. Não mexe em aldeão batizado pelo jogador. */
    private static void nameVillager(Villager villager) {
        var data = villager.getPersistentData();
        if (!data.contains(NAME_KEY)) {
            if (villager.hasCustomName()) return;
            List<? extends String> names = Config.VILLAGER_NAMES.get();
            if (names.isEmpty()) return;
            data.putString(NAME_KEY, names.get(villager.getRandom().nextInt(names.size())).trim());
        }
        MutableComponent name = Component.empty();
        if (data.hasUUID(VillagerCompanions.COMPANION_KEY)) name.append(icon(POKEBALL)).append(" ");
        villager.setCustomName(name.append(Component.literal(data.getString(NAME_KEY))));
        villager.setCustomNameVisible(false);
    }

    /** Versões anteriores davam ao Pokémon do aldeão o apelido "Aron do Ash"; agora ele usa o nome normal. */
    private static void clearOldPetName(PokemonEntity pokemon) {
        Component nickname = pokemon.getPokemon().getNickname();
        if (nickname != null && nickname.getContents() instanceof TranslatableContents t && t.getKey().equals(KEY + "pet")) {
            pokemon.getPokemon().setNickname(null);
        }
    }

    private static boolean startsWithIcon(Entity entity) {
        String name = entity.getCustomName() == null ? "" : entity.getCustomName().getString();
        return name.startsWith(BACKPACK) || name.startsWith(WITCH_HAT) || name.startsWith(POKEBALL);
    }
}
