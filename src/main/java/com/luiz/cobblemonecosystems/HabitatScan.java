package com.luiz.cobblemonecosystems;

import java.util.Comparator;
import java.util.List;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.item.PokedexItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Agachar e clicar com o botão direito num Pokémon do pasto segurando a Pokédex mostra no chat o habitat dele: o
 * conforto, o temperamento, do que ele gosta e não gosta ali em volta, e o espaço.
 * (O escaneamento normal da Pokédex só termina quando há informação nova, e os Pokémon do jogador já estão
 * registrados; por isso o clique. Se um escaneamento terminar, o relatório também aparece.)
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID)
public final class HabitatScan {
    private static final String KEY = CobblemonEcosystems.MOD_ID + ".habitat.";

    private HabitatScan() {}

    public static void registerCobblemonEvents() {
        CobblemonEvents.POKEMON_SCANNED.subscribe(Priority.NORMAL, event -> {
            if (Habitat.enabled() && event.getScannedEntity().resolveEntityScan() instanceof PokemonEntity pokemon
                    && pokemon.getTethering() != null) {
                show(event.getPlayer(), pokemon);
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!Habitat.enabled() || !event.getEntity().isShiftKeyDown()
                || !(event.getItemStack().getItem() instanceof PokedexItem)
                || !(event.getTarget() instanceof PokemonEntity pokemon)) return;
        // No cliente não dá para saber se é do pasto; nos dois lados o clique não faz mais nada.
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player && event.getHand() == InteractionHand.MAIN_HAND) {
            if (pokemon.getTethering() != null) show(player, pokemon);
            else player.displayClientMessage(Component.translatable(KEY + "not_pastured").withStyle(ChatFormatting.GRAY), true);
        }
    }

    static void show(ServerPlayer player, PokemonEntity pokemon) {
        Habitat.Report report = Habitat.report(pokemon);
        if (report == null) return;

        ChatFormatting comfortColor = switch (report.comfort()) {
            case HAPPY -> ChatFormatting.GREEN;
            case COMFORTABLE -> ChatFormatting.DARK_GREEN;
            case NEUTRAL -> ChatFormatting.YELLOW;
            case UNCOMFORTABLE -> ChatFormatting.RED;
        };
        player.sendSystemMessage(Component.translatable(KEY + "title", pokemon.getName(),
                Component.translatable(KEY + "comfort." + report.comfort().name().toLowerCase()).withStyle(comfortColor, ChatFormatting.BOLD))
                .withStyle(ChatFormatting.GOLD));

        Habitat.Survey survey = Habitat.survey(player.serverLevel(), pokemon.getTethering().getPasturePos());
        if (survey.isEmpty()) {
            player.sendSystemMessage(Component.translatable(KEY + "empty").withStyle(ChatFormatting.RED));
            return;
        }

        player.sendSystemMessage(Component.translatable(KEY + "temperament",
                Component.translatable(KEY + "temperament." + report.temperament().name().toLowerCase()).withStyle(ChatFormatting.WHITE),
                Component.translatable(pokemon.getPokemon().getNature().getDisplayName())).withStyle(ChatFormatting.GRAY));

        List<Habitat.Factor> likes = report.factors().stream().filter(Habitat.Factor::likes)
                .sorted(Comparator.comparing(Habitat.Factor::value).reversed()).toList();
        List<Habitat.Factor> dislikes = report.factors().stream().filter(f -> !f.likes())
                .sorted(Comparator.comparing(Habitat.Factor::value).reversed()).toList();
        if (!likes.isEmpty()) player.sendSystemMessage(Component.translatable(KEY + "likes", join(likes)).withStyle(ChatFormatting.GRAY));
        if (!dislikes.isEmpty()) {
            player.sendSystemMessage(Component.translatable(KEY + "dislikes", join(dislikes)).withStyle(ChatFormatting.GRAY));
        }

        MutableComponent space = Component.translatable(KEY + "space", report.areaPerPokemon()).withStyle(ChatFormatting.GRAY);
        if (report.cramped()) space.append(Component.translatable(KEY + "cramped").withStyle(ChatFormatting.RED));
        player.sendSystemMessage(space);
        if (report.lost()) player.sendSystemMessage(Component.translatable(KEY + "lost").withStyle(ChatFormatting.BLUE));

        PastureFarms.State farm = Config.PASTURE_FARMS_ON.get() ? PastureFarms.state(pokemon, report) : null;
        if (farm != null) {
            MutableComponent status = switch (farm.status()) {
                case READY -> Component.translatable(KEY + "farm.ready", farm.minutesLeft()).withStyle(ChatFormatting.GREEN);
                case OFF -> Component.translatable(KEY + "farm.off").withStyle(ChatFormatting.YELLOW);
                case NOT_COMFORTABLE -> Component.translatable(KEY + "farm.not_comfortable").withStyle(ChatFormatting.RED);
                case NOT_NEAR -> Component.translatable(KEY + "farm.not_near",
                        Component.translatableWithFallback(KEY + "group." + farm.missing(), farm.missing())).withStyle(ChatFormatting.RED);
                case NOT_ON_BLOCK -> Component.translatable(KEY + "farm.not_on_block",
                        Component.translatableWithFallback(KEY + "group." + farm.missing(), farm.missing())).withStyle(ChatFormatting.RED);
                case NOT_IN_FOREST -> Component.translatable(KEY + "farm.not_in_forest").withStyle(ChatFormatting.RED);
            };
            Component item = farm.item() != null ? farm.item().getDescription() : Component.translatable(KEY + "farm.biome_log");
            player.sendSystemMessage(Component.translatable(KEY + "farm", item, farm.minutes(), status).withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Os grupos separados por vírgula. Do que gosta: verde se tem bastante, amarelo se tem um pouco, cinza se não tem.
     * Do que não gosta: vermelho se tem, cinza se não tem.
     */
    private static MutableComponent join(List<Habitat.Factor> factors) {
        MutableComponent text = Component.empty();
        for (int i = 0; i < factors.size(); i++) {
            Habitat.Factor f = factors.get(i);
            if (i > 0) text.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            ChatFormatting color = f.likes()
                    ? (f.value() >= 0.75f ? ChatFormatting.GREEN : f.value() >= 0.25f ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY)
                    : (f.value() >= 0.25f ? ChatFormatting.RED : ChatFormatting.DARK_GRAY);
            MutableComponent name = Component.translatableWithFallback(KEY + "group." + f.group(), f.group()).withStyle(color);
            if (!Habitat.BUILT_IN.contains(f.group()) && f.blocks() > 0) name.append(Component.literal(" (" + f.blocks() + ")").withStyle(color));
            text.append(name);
        }
        return text;
    }
}
