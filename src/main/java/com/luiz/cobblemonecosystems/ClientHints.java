package com.luiz.cobblemonecosystems;

import com.cobblemon.mod.common.client.keybind.keybinds.PartySendBinding;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Só no cliente. Olhando para quem dá para desafiar (aldeão com Pokémon, mercador, bruxa da cabana; reconhecidos
 * pelo ícone no nome, de {@link NameTags}):
 * - mostra a dica "Shift + clique direito ou R para batalhar" acima da barra de itens;
 * - apertar a tecla de soltar Pokémon do Cobblemon (R) pede o desafio ao servidor ({@link ChallengeRequest}),
 *   porque o Cobblemon não solta a Pokébola nesses alvos.
 */
@EventBusSubscriber(modid = CobblemonEcosystems.MOD_ID, value = Dist.CLIENT)
public final class ClientHints {
    /** Até onde a mira alcança. */
    private static final double RANGE = 10;
    private static boolean sendWasDown;
    private static int hintTicks;

    private ClientHints() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) {
            sendWasDown = false;
            return;
        }
        Entity target = challengeTarget(player);
        boolean sendDown = PartySendBinding.INSTANCE.isDown();
        if (target != null && sendDown && !sendWasDown) PacketDistributor.sendToServer(new ChallengeRequest(target.getId()));
        sendWasDown = sendDown;

        if (target != null && ++hintTicks % 20 == 1) {
            player.displayClientMessage(Component.translatable(CobblemonEcosystems.MOD_ID + ".name.hint",
                    Component.keybind(PartySendBinding.INSTANCE.getName())), true);
        } else if (target == null) {
            hintTicks = 0;
        }
    }

    private static Entity challengeTarget(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1f).scale(RANGE));
        AABB area = player.getBoundingBox().expandTowards(player.getViewVector(1f).scale(RANGE)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, area, ClientHints::challengeable, RANGE * RANGE);
        if (hit == null) return null;
        // Não atravessa paredes.
        var block = player.pick(RANGE, 1f, false);
        if (block.getType() != net.minecraft.world.phys.HitResult.Type.MISS
                && block.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye)) return null;
        return hit.getEntity();
    }

    private static boolean challengeable(Entity entity) {
        if (!(entity instanceof Villager || entity instanceof WanderingTrader || entity instanceof Witch) || entity.getCustomName() == null) {
            return false;
        }
        String name = entity.getCustomName().getString();
        return name.startsWith(NameTags.POKEBALL) || name.startsWith(NameTags.BACKPACK) || name.startsWith(NameTags.WITCH_HAT);
    }
}
