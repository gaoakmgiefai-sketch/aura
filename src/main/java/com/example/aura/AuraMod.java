package com.example.aura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class AuraMod implements ClientModInitializer {
    public static boolean enabled = true;
    public static boolean smooth = true;
    public static boolean hud = true;
    public static double range = 4.0;
    public static float rotSpeed = 18f; // градусов за тик

    private static KeyBinding openKey;
    private static KeyBinding toggleKey;

    private PlayerEntity cached;
    private int searchTimer = 0;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aura.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.aura"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aura.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.aura"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) client.setScreen(new AuraScreen());
            while (toggleKey.wasPressed()) enabled = !enabled;
            tick(client);
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (!hud || !enabled || mc.player == null || mc.options.hudHidden) return;
            ctx.drawTextWithShadow(mc.textRenderer, "Aura  " + range, 6, 6, 0xFF7C5CFF);
        });
    }

    private void tick(MinecraftClient client) {
        ClientPlayerEntity self = client.player;
        if (!enabled || self == null || client.world == null || client.interactionManager == null) return;
        if (client.currentScreen != null) return;

        // кэш цели: полный поиск раз в 3 тика, проверка кэша каждый тик
        if (cached != null && !valid(self, cached)) cached = null;
        if (cached == null && --searchTimer <= 0) {
            searchTimer = 3;
            cached = find(client, self);
        }
        if (cached == null) return;

        boolean aimed = aim(self, cached);

        // правило 1.9+: бьём только с полным кулдауном
        if (!aimed || self.getAttackCooldownProgress(0.5f) < 1.0f) return;

        client.interactionManager.attackEntity(self, cached);
        self.swingHand(Hand.MAIN_HAND);
    }

    private boolean valid(ClientPlayerEntity self, PlayerEntity p) {
        return p.isAlive() && !p.isRemoved() && !p.isSpectator()
                && self.squaredDistanceTo(p) <= range * range;
    }

    private PlayerEntity find(MinecraftClient client, ClientPlayerEntity self) {
        var players = client.world.getPlayers();
        if (players.size() <= 1) return null;
        PlayerEntity best = null;
        double bestDist = range * range;
        for (int i = 0; i < players.size(); i++) {
            PlayerEntity p = players.get(i);
            if (p == self || !p.isAlive() || p.isSpectator()) continue;
            double d = self.squaredDistanceTo(p);
            if (d <= bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    /** Возвращает true, если прицел уже на цели. */
    private boolean aim(ClientPlayerEntity self, PlayerEntity t) {
        double dx = t.getX() - self.getX();
        double dz = t.getZ() - self.getZ();
        double dy = (t.getY() + t.getHeight() * 0.5) - self.getEyeY();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));

        float yawDiff = MathHelper.wrapDegrees(wantYaw - self.getYaw());
        float pitchDiff = wantPitch - self.getPitch();

        if (smooth) {
            float yawStep = MathHelper.clamp(yawDiff * 0.6f, -rotSpeed, rotSpeed);
            float pitchStep = MathHelper.clamp(pitchDiff * 0.6f, -rotSpeed, rotSpeed);
            self.setYaw(self.getYaw() + yawStep);
            self.setPitch(MathHelper.clamp(self.getPitch() + pitchStep, -90f, 90f));
            return Math.abs(yawDiff - yawStep) < 8f && Math.abs(pitchDiff - pitchStep) < 8f;
        }

        self.setYaw(self.getYaw() + yawDiff);
        self.setPitch(MathHelper.clamp(wantPitch, -90f, 90f));
        return true;
    }
}
