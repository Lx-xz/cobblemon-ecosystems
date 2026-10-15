package com.luiz.cobblemonecosystems;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelOption;

import net.minecraft.client.Minecraft;
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
 * - põe o botão de casinha "Habitat" no menu radial dos Pokémon do Cobblemon (agachar + clique direito no seu
 *   Pokémon), que pede ao servidor o relatório do habitat ({@link HabitatRequest}).
 */
@Mod(value = CobblemonEcosystems.MOD_ID, dist = Dist.CLIENT)
public class CobblemonEcosystemsClient {
    public CobblemonEcosystemsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION.subscribe(Priority.NORMAL, event -> {
            if (Config.HABITAT.get()) {
                event.addFillingOption(new InteractWheelOption(HABITAT_ICON, null, true,
                        CobblemonEcosystems.MOD_ID + ".ui.interact.habitat", () -> null, () -> {
                            PacketDistributor.sendToServer(new HabitatRequest(event.getPokemonID()));
                            Minecraft.getInstance().setScreen(null);
                            return kotlin.Unit.INSTANCE;
                        }));
            }
            return kotlin.Unit.INSTANCE;
        });
    }

    private static final ResourceLocation HABITAT_ICON =
            ResourceLocation.fromNamespaceAndPath(CobblemonEcosystems.MOD_ID, "textures/gui/interact_wheel_icon_habitat.png");
}
