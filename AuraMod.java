package com.example.aura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class AuraMod implements ClientModInitializer {
    public static boolean enabled = true;
    public static double range = 4.0;

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aura.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.aura"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                client.setScreen(new AuraScreen());
            }
            tickAura(client);
        });
    }

    private void tickAura(MinecraftClient client) {
        ClientPlayerEntity self = client.player;
        if (!enabled || self == null || client.world == null || client.interactionManager == null) return;
        if (client.currentScreen != null) return;
        if (self.getAttackCooldownProgress(0.5f) < 1.0f) return;

        PlayerEntity best = null;
        double bestDist = range * range;

        for (PlayerEntity p : client.world.getPlayers()) {
            if (p == self || !p.isAlive() || p.isSpectator()) continue;
            double d = self.squaredDistanceTo(p);
            if (d <= bestDist) {
                bestDist = d;
                best = p;
            }
        }
        if (best == null) return;

        double dx = best.getX() - self.getX();
        double dz = best.getZ() - self.getZ();
        double dy = best.getEyeY() - self.getEyeY();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        self.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        self.setPitch((float) -Math.toDegrees(Math.atan2(dy, horiz)));

        client.interactionManager.attackEntity(self, best);
        self.swingHand(Hand.MAIN_HAND);
    }
}
