package com.luiz.cobblemonecosystems;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Só no client: liga o botão "Config" da lista de mods à tela de configuração do próprio NeoForge, para editar
 * o {@link Config} dentro do jogo. As dicas de cada opção vêm dos comentários do arquivo.
 */
@Mod(value = CobblemonEcosystems.MOD_ID, dist = Dist.CLIENT)
public class CobblemonEcosystemsClient {
    public CobblemonEcosystemsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
