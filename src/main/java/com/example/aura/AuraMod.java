package com.example.aura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public class AuraMod implements ClientModInitializer {
    // Настройки киллауры
    public static boolean enabled = true;
    public static boolean mode360 = false; // false = Legit (плавный FOV), true = 360 градусов
    public static double range = 4.2;

    private static KeyBinding menuKey;
    private static int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        // Регистрация клавиши открытия меню (Правый Shift)
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aura.menu",
                InputUtil.Type.KEY_SYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.aura"
        ));

        // Логика тика клиента (оптимизированная киллаура + открытие меню)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && enabled) {
                onTick(client.player);
            }
            while (menuKey.wasPressed()) {
                client.setScreen(new AuraScreen());
            }
        });
    }

    // Оптимизированная логика киллауры
    private void onTick(ClientPlayerEntity player) {
        tickCounter++;
        if (tickCounter < 2) return; // Оптимизация: расчет раз в 2 тика для экономии ресурсов
        tickCounter = 0;

        World world = player.getWorld();
        LivingEntity target = null;
        double minDistance = range;
        float maxFov = mode360 ? 180.0f : 75.0f;

        for (Entity entity : world.getEntities()) {
            if (entity instanceof LivingEntity && entity != player && entity.isAlive()) {
                double distance = player.distanceTo(entity);
                if (distance <= range) {
                    float yawToEntity = getRotations(player, entity)[0];
                    float fovDelta = MathHelper.wrapDegrees(yawToEntity - player.getYaw());

                    if (Math.abs(fovDelta) <= maxFov) {
                        if (distance < minDistance) {
                            minDistance = distance;
                            target = (LivingEntity) entity;
                        }
                    }
                }
            }
        }

        if (target != null) {
            if (!mode360) {
                // Плавная наводка для Legit режима
                float[] rots = getRotations(player, target);
                player.setYaw(player.getYaw() + MathHelper.wrapDegrees(rots[0] - player.getYaw()) * 0.4f);
                player.setPitch(player.getPitch() + (rots[1] - player.getPitch()) * 0.4f);
            }

            // Удар с проверкой кулдауна
            MinecraftClient client = MinecraftClient.getInstance();
            if (player.getAttackCooldown_Progress(0.5f) >= 1.0f && client.interactionManager != null) {
                client.interactionManager.attackEntity(player, target);
                player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private float[] getRotations(ClientPlayerEntity player, Entity target) {
        double x = target.getX() - player.getX();
        double y = (target.getY() + target.getEyeHeight()) - (player.getY() + player.getEyeHeight());
        double z = target.getZ() - player.getZ();
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * (180 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, dist) * (180 / Math.PI)));
        return new float[]{yaw, pitch};
    }

    // Класс красивого графического меню
    public static class AuraScreen extends Screen {
        public AuraScreen() {
            super(Text.literal("Aura Menu"));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context, mouseX, mouseY, delta);

            int x = this.width / 2 - 120;
            int y = this.height / 2 - 100;
            context.fill(x, y, x + 240, y + 200, 0xEE101018); // Темный фон панели
            context.fill(x, y, x + 240, y + 25, 0xFF6200EE);  // Шапка

            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("§l✨ AURA CLIENT ✨"), this.width / 2, y + 8, 0xFFFFFFFF);

            // Кнопка включения/выключения
            int btnY1 = y + 45;
            context.fill(x + 20, btnY1, x + 220, btnY1 + 25, enabled ? 0xFF00C853 : 0xFFB71C1C);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("KillAura: " + (enabled ? "§aON" : "§cOFF")), this.width / 2, btnY1 + 8, 0xFFFFFFFF);

            // Кнопка выбора режима (Legit / 360)
            int btnY2 = y + 80;
            context.fill(x + 20, btnY2, x + 220, btnY2 + 25, 0xFF333333);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Режим: " + (mode360 ? "§6360° (Радиус)" : "§bLegit (Плавный)")), this.width / 2, btnY2 + 8, 0xFFFFFFFF);

            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int x = this.width / 2 - 120;
            int y = this.height / 2 - 100;

            if (mouseX >= x + 20 && mouseX <= x + 220 && mouseY >= y + 45 && mouseY <= y + 70) {
                enabled = !enabled;
            }
            if (mouseX >= x + 20 && mouseX <= x + 220 && mouseY >= y + 80 && mouseY <= y + 105) {
                mode360 = !mode360;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }
}
