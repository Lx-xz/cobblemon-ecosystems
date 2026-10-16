package com.luiz.cobblemonecosystems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.luiz.cobblemonecosystems.ConfigLists.Family;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * O livro dos habitats, que cada jogador ganha na primeira vez que entra no mundo: como funciona o habitat do pasto,
 * como ver o conforto, os temperamentos, as farms e uma página por tipo com do que ele gosta e não gosta.
 * As páginas saem da configuração na hora (se as listas forem editadas, o livro acompanha) e os textos são chaves
 * de tradução, então cada jogador lê na língua do seu jogo.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public final class GuideBook {
    private static final String KEY = CobblemonEcosystems.MOD_ID + ".guide.";
    private static final String GROUP = CobblemonEcosystems.MOD_ID + ".habitat.group.";
    private static final String GIVEN_KEY = CobblemonEcosystems.MOD_ID + ":guide_given";
    /** A ordem dos tipos nas páginas (a da Pokédex). */
    private static final List<String> TYPES = List.of("normal", "fire", "water", "grass", "electric", "ice", "fighting", "poison",
            "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark", "steel", "fairy");

    private GuideBook() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Config.GUIDE_BOOK.get() || !Habitat.enabled()) return;
        // Os dados "persistidos" do jogador sobrevivem à morte.
        CompoundTag data = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (data.getBoolean(GIVEN_KEY)) return;
        data.putBoolean(GIVEN_KEY, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, data);
        ItemStack book = create();
        if (!player.getInventory().add(book)) player.drop(book, false);
    }

    public static ItemStack create() {
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(page(Component.translatable(KEY + "intro.title").withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GREEN),
                Component.literal("\n\n"), Component.translatable(KEY + "intro")));
        pages.add(page(title(KEY + "see.title"), Component.literal("\n\n"), Component.translatable(KEY + "see")));
        pages.add(page(title(KEY + "comfort.title"), Component.literal("\n\n"), Component.translatable(KEY + "comfort")));
        pages.add(page(title(KEY + "temperament.title"), Component.literal("\n\n"), Component.translatable(KEY + "temperament")));

        Map<String, List<Family>> preferences = ConfigLists.keyed(Config.HABITAT_PREFERENCES);
        for (String type : TYPES) {
            List<Family> prefs = preferences.get(type);
            if (prefs != null) pages.add(typePage(Component.translatable("cobblemon.type." + type)
                    .withStyle(s -> s.withBold(true).withColor(TextColor.fromRgb(darker(Hunts.typeRgb(type))))), prefs));
        }
        // Espécies com gosto próprio (ex.: Zubat).
        for (Map.Entry<String, List<Family>> entry : preferences.entrySet()) {
            if (!TYPES.contains(entry.getKey())) {
                pages.add(typePage(Component.translatable("cobblemon.species." + entry.getKey() + ".name")
                        .withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_PURPLE), entry.getValue()));
            }
        }

        if (Config.PASTURE_FARMS_ON.get() && !PastureFarms.farms().isEmpty()) {
            MutableComponent farms = Component.empty();
            for (PastureFarms.Farm farm : PastureFarms.farms()) {
                Component who = farm.who().startsWith("type:")
                        ? Component.translatable("cobblemon.type." + farm.who().substring(5))
                        : Component.translatable("cobblemon.species." + farm.who() + ".name");
                farms.append(Component.literal("\n\n"));
                Component item = farm.item() != null ? farm.item().getDescription()
                        : Component.translatable(CobblemonEcosystems.MOD_ID + ".habitat.farm.biome_log");
                farms.append(Component.translatable(KEY + "farm", who, item, farm.minutes()));
                if (farm.on() != null) {
                    farms.append(Component.literal(" "));
                    farms.append(Component.translatable(KEY + "farm.on", group(farm.on())));
                }
                if (!farm.near().isEmpty()) {
                    MutableComponent near = Component.empty();
                    for (int i = 0; i < farm.near().size(); i++) {
                        if (i > 0) near.append(Component.literal(", "));
                        near.append(group(farm.near().get(i)));
                    }
                    farms.append(Component.literal(" "));
                    farms.append(Component.translatable(KEY + "farm.near", near));
                }
            }
            pages.add(page(title(KEY + "farms.title"), Component.literal("\n\n"), Component.translatable(KEY + "farms"), farms));
        }

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(Filterable.passThrough("Pokémon Habitats"), "Cobblemon Ecosystems", 0, pages, true));
        return book;
    }

    private static MutableComponent title(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.BOLD);
    }

    private static Filterable<Component> page(Component... parts) {
        MutableComponent text = Component.empty();
        for (Component part : parts) text.append(part);
        return Filterable.passThrough(text);
    }

    /** Página de um tipo (ou espécie): o nome, do que gosta e do que não gosta. */
    private static Filterable<Component> typePage(MutableComponent name, List<Family> prefs) {
        MutableComponent likes = Component.empty(), dislikes = Component.empty();
        for (Family pref : prefs) {
            boolean like = !pref.base().startsWith("-");
            MutableComponent list = like ? likes : dislikes;
            list.append(Component.literal("\n- ")).append(group(like ? pref.base() : pref.base().substring(1)));
            if (pref.weight() > 10) list.append(Component.literal(" ★").withStyle(ChatFormatting.GOLD));
        }
        MutableComponent text = Component.empty().append(name);
        text.append(Component.literal("\n\n")).append(Component.translatable(KEY + "likes").withStyle(ChatFormatting.DARK_GREEN));
        text.append(likes.getSiblings().isEmpty() ? Component.literal("\n-") : likes);
        if (!dislikes.getSiblings().isEmpty()) {
            text.append(Component.literal("\n\n")).append(Component.translatable(KEY + "dislikes").withStyle(ChatFormatting.DARK_RED));
            text.append(dislikes);
        }
        return Filterable.passThrough(text);
    }

    private static Component group(String name) {
        return Component.translatableWithFallback(GROUP + name, name);
    }

    /** As cores dos tipos são claras demais para o papel do livro: escurece um pouco. */
    private static int darker(int rgb) {
        int r = (rgb >> 16 & 0xFF) * 7 / 10, g = (rgb >> 8 & 0xFF) * 7 / 10, b = (rgb & 0xFF) * 7 / 10;
        return r << 16 | g << 8 | b;
    }
}
