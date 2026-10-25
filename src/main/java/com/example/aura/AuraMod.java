package com.example.aura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

public class AuraMod implements ClientModInitializer {
    public static boolean auraEnabled = false;
    private static KeyBinding menuKey;
    
    // Настройки киллауры
    public static double reachDistance = 3.8; 
    public static int fovCheck = 90;          

    @Override
    public void onInitializeClient() {
        // Открытие меню по нажатию Правого Shift (теперь без крашей)
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aura.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.aura.mod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Обработка открытия меню
            while (menuKey.wasPressed()) {
                client.setScreen(new AuraScreen(null));
            }

            // Работа киллауры, если она включена
            if (auraEnabled) {
                runLegitAura(client);
            }
        });
    }

    private void runLegitAura(net.minecraft.client.MinecraftClient client) {
        Entity target = null;
        double closestDistance = reachDistance;

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof LivingEntity && entity != client.player) {
                if (entity instanceof PlayerEntity && ((PlayerEntity) entity).isCreative()) continue;
                if (entity.isSpectator()) continue;

                double distance = client.player.distanceTo(entity);
                if (distance <= closestDistance) {
                    if (isInFov(client.player, entity, fovCheck)) {
                        closestDistance = distance;
                        target = entity;
                    }
                }
            }
        }

        // Нанесение удара с учетом кулдауна оружия
        if (target != null && client.interactionManager != null) {
            if (client.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    private boolean isInFov(PlayerEntity player, Entity target, float angle) {
        double diffX = target.getX() - player.getX();
        double diffZ = target.getZ() - player.getZ();
        double yaw = Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0D;
        double normalizedAngle = Math.abs((player.getYaw() - yaw) % 360.0D);
        if (normalizedAngle > 180.0D) {
            normalizedAngle = 360.0D - normalizedAngle;
        }
        return normalizedAngle <= angle / 2.0D;
    }
}
