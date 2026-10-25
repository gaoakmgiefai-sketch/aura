package com.example.aura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.lwjgl.glfw.GLFW;

public class AuraMod implements ClientModInitializer {
    public static boolean auraEnabled = false;
    private static KeyBinding menuKey;
    
    // Настройки киллауры для максимального обхода
    public static double reachDistance = 3.8; // Дистанция удара
    public static int fovCheck = 90;          // Угол обзора (градусы)

    @Override
    public void onInitializeClient() {
        // Регистрация клавиши открытия меню (Right Shift)
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aura.menu",
                InputUtil.Type.KEY_SYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.aura.mod"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Открытие меню по нажатию клавиши
            while (menuKey.wasPressed()) {
                client.setScreen(new AuraScreen(null));
            }

            // Логика киллауры, если она включена в меню
            if (auraEnabled) {
                runLegitAura(client);
            }
        });
    }

    private void runLegitAura(net.minecraft.client.MinecraftClient client) {
        Entity target = null;
        double closestDistance = reachDistance;

        // Поиск целей в радиусе (проверяем только живых сущностей)
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof LivingEntity && entity != client.player) {
                if (entity instanceof PlayerEntity && ((PlayerEntity) entity).isCreative()) continue;

                double distance = client.player.distanceTo(entity);
                if (distance <= closestDistance) {
                    // Проверка FOV (чтобы не бить врагов сзади)
                    if (isInFov(client.player, entity, fovCheck)) {
                        closestDistance = distance;
                        target = entity;
                    }
                }
            }
        }

        // Если цель найдена — наносим удар с учётом кулдауна
        if (target != null && client.interactionManager != null) {
            if (client.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            }
        }
    }

    // Метод проверки угла обзора (FOV)
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
