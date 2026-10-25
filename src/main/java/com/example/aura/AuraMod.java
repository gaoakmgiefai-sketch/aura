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
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

public class AuraMod implements ClientModInitializer {
    public static boolean killAuraEnabled = true;
    public static boolean silentAim = true;
    public static boolean velocityEnabled = true;
    public static boolean espEnabled = true;
    public static double range = 3.9;

    private static KeyBinding menuKey;
    private static int tickCounter = 0;
    private static int attackTicks = 0;

    @Override
    public void onInitializeClient() {
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.gemini.menu",
                InputUtil.Type.KEYSYM, // Исправлено с KEY_SYM на KEYSYM
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.gemini"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (killAuraEnabled) onTick(client.player);
                
                if (velocityEnabled && client.player.hurtTime > 0) {
                    client.player.setVelocity(client.player.getVelocity().x * 0.6, client.player.getVelocity().y, client.player.getVelocity().z * 0.6);
                }
            }
            while (menuKey.wasPressed()) {
                client.setScreen(new ClickGuiScreen());
            }
        });
    }

    private void onTick(ClientPlayerEntity player) {
        tickCounter++;
        if (tickCounter < 2) return;
        tickCounter = 0;

        World world = player.getWorld();
        LivingEntity target = null;
        double minDistance = range;

        Box searchBox = player.getBoundingBox().expand(range);
        for (Entity entity : world.getOtherEntities(player, searchBox)) {
            if (entity instanceof LivingEntity && entity.isAlive()) {
                double distance = player.distanceTo(entity);
                if (distance <= range) {
                    if (distance < minDistance) {
                        minDistance = distance;
                        target = (LivingEntity) entity;
                    }
                }
            }
        }

        if (target != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            float[] rots = getRotations(player, target);

            if (silentAim && client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                        rots[0], rots[1], player.isOnGround()
                ));
            }

            attackTicks++;
            if (attackTicks >= 10 && client.interactionManager != null) {
                attackTicks = 0;
                client.interactionManager.attackEntity(player, target);
                player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private float[] getRotations(ClientPlayerEntity player, Entity target) {
        double x = target.getX() - player.getX();
        double y = (target.getY() + target.getEyeHeight(target.getPose())) - (player.getY() + player.getEyeHeight(target.getPose()));
        double z = target.getZ() - player.getZ();
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * (180 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, dist) * (180 / Math.PI)));
        return new float[]{yaw, pitch};
    }

    public static class ClickGuiScreen extends Screen {
        private int selectedCategory = 0;
        private float animationProgress = 0.0f;

        public ClickGuiScreen() {
            super(Text.literal("Gemini ClickGUI"));
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (animationProgress < 1.0f) {
                animationProgress += delta * 0.2f;
                if (animationProgress > 1.0f) animationProgress = 1.0f;
            }

            this.renderBackground(context, mouseX, mouseY, delta);

            int baseWidth = 340;
            int baseHeight = 230;
            int startX = this.width / 2 - baseWidth / 2;
            int startY = this.height / 2 - baseHeight / 2;

            context.fill(startX, startY, startX + baseWidth, startY + baseHeight, 0xFF0D0D12);
            context.fill(startX, startY, startX + baseWidth, startY + 30, 0xFF3B1863);

            context.drawTextWithShadow(this.textRenderer, Text.literal("✨ §dGEMINI CLIENT §8| §bBypass Edition"), startX + 12, startY + 10, 0xFFFFFFFF);

            context.fill(startX, startY + 30, startX + 110, startY + baseHeight, 0xFF14141C);
            
            drawCategoryButton(context, startX + 10, startY + 45, 90, 25, "⚔ Combat", selectedCategory == 0, mouseX, mouseY);
            drawCategoryButton(context, startX + 10, startY + 75, 90, 25, "⚡ Movement", selectedCategory == 1, mouseX, mouseY);
            drawCategoryButton(context, startX + 10, startY + 105, 90, 25, "👁 Visuals", selectedCategory == 2, mouseX, mouseY);

            if (selectedCategory == 0) {
                drawModuleToggle(context, startX + 125, startY + 45, 200, 32, "KillAura", killAuraEnabled, mouseX, mouseY);
                drawModuleToggle(context, startX + 125, startY + 85, 200, 32, "Silent Aim (Bypass)", silentAim, mouseX, mouseY);
            } else if (selectedCategory == 1) {
                drawModuleToggle(context, startX + 125, startY + 45, 200, 32, "Velocity (Anti-KB)", velocityEnabled, mouseX, mouseY);
            } else if (selectedCategory == 2) {
                drawModuleToggle(context, startX + 125, startY + 45, 200, 32, "Player ESP", espEnabled, mouseX, mouseY);
            }

            super.render(context, mouseX, mouseY, delta);
        }

        private void drawCategoryButton(DrawContext context, int x, int y, int w, int h, String text, boolean selected, int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            int color = selected ? 0xFF6B21A8 : (hovered ? 0xFF2A2A38 : 0xFF1B1B24);
            context.fill(x, y, x + w, y + h, color);
            context.drawTextWithShadow(this.textRenderer, Text.literal(text), x + 10, y + 8, selected ? 0xFFFFFFFF : 0xFFA1A1AA);
        }

        private void drawModuleToggle(DrawContext context, int x, int y, int w, int h, String name, boolean state, int mouseX, int mouseY) {
            boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
            int bgColor = hovered ? 0xFF252533 : 0xFF1C1C26;
            context.fill(x, y, x + w, y + h, bgColor);
            
            context.drawTextWithShadow(this.textRenderer, Text.literal(name), x + 12, y + 11, 0xFFFFFFFF);
            String status = state ? "§aON" : "§cOFF";
            context.drawTextWithShadow(this.textRenderer, Text.literal(status), x + w - 35, y + 11, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int baseWidth = 340;
            int baseHeight = 230;
            int startX = this.width / 2 - baseWidth / 2;
            int startY = this.height / 2 - baseHeight / 2;

            if (mouseX >= startX + 10 && mouseX <= startX + 100) {
                if (mouseY >= startY + 45 && mouseY <= startY + 70) selectedCategory = 0;
                if (mouseY >= startY + 75 && mouseY <= startY + 100) selectedCategory = 1;
                if (mouseY >= startY + 105 && mouseY <= startY + 130) selectedCategory = 2;
            }

            if (mouseX >= startX + 125 && mouseX <= startX + 325) {
                if (selectedCategory == 0) {
                    if (mouseY >= startY + 45 && mouseY <= startY + 77) killAuraEnabled = !killAuraEnabled;
                    if (mouseY >= startY + 85 && mouseY <= startY + 117) silentAim = !silentAim;
                } else if (selectedCategory == 1) {
                    if (mouseY >= startY + 45 && mouseY <= startY + 77) velocityEnabled = !velocityEnabled;
                } else if (selectedCategory == 2) {
                    if (mouseY >= startY + 45 && mouseY <= startY + 77) espEnabled = !espEnabled;
                }
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }
}
