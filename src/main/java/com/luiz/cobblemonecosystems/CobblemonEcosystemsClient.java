package com.luiz.cobblemonecosystems;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.interaction.PokemonInteractionGUICreationEvent;
import com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelOption;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Só no client:
 * - liga o botão "Config" da lista de mods à tela de configuração do próprio NeoForge, para editar o {@link Config}
 *   dentro do jogo (as dicas de cada opção vêm dos comentários do arquivo);
 * - põe dois botões no menu radial dos Pokémon do Cobblemon (agachar + clique direito no seu Pokémon), só para os
 *   que estão no pasto ({@link PastureInfo}): a casinha "Habitat", que pede ao servidor o relatório do habitat
 *   ({@link HabitatRequest}), e a cesta com ovo "Coleta", que liga/desliga a farm dele ({@link FarmToggleRequest});
 *   desligada, a cesta aparece cortada.
 */
@Mod(value = CobblemonEcosystems.MOD_ID, dist = Dist.CLIENT)
public class CobblemonEcosystemsClient {
    private static final ResourceLocation HABITAT_ICON = icon("habitat");
    private static final ResourceLocation FARM_ON_ICON = icon("farm_on");
    private static final ResourceLocation FARM_OFF_ICON = icon("farm_off");

    public CobblemonEcosystemsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION.subscribe(Priority.NORMAL, event -> {
            PastureInfo info = PastureInfo.KNOWN.remove(event.getPokemonID());
            if (info == null || !info.pastured()) return kotlin.Unit.INSTANCE;
            if (Config.HABITAT.get()) {
                addButton(event, HABITAT_ICON, "habitat", new HabitatRequest(event.getPokemonID()));
            }
            if (info.hasFarm()) {
                addButton(event, info.farmOn() ? FARM_ON_ICON : FARM_OFF_ICON, info.farmOn() ? "farm_on" : "farm_off",
                        new FarmToggleRequest(event.getPokemonID()));
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    /** Um botão que manda {@code request} ao servidor e fecha o menu. */
    private static void addButton(PokemonInteractionGUICreationEvent event, ResourceLocation icon, String tooltip,
            CustomPacketPayload request) {
        event.addFillingOption(new InteractWheelOption(icon, null, true,
                CobblemonEcosystems.MOD_ID + ".ui.interact." + tooltip, () -> null, () -> {
                    PacketDistributor.sendToServer(request);
                    Minecraft.getInstance().setScreen(null);
                    return kotlin.Unit.INSTANCE;
                }));
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(CobblemonEcosystems.MOD_ID, "textures/gui/interact_wheel_icon_" + name + ".png");
    }
}
