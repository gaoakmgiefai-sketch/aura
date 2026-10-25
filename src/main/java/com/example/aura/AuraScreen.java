package com.example.aura;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AuraScreen extends Screen {
    private final Screen parent;

    protected AuraScreen(Screen parent) {
        super(Text.literal("Aura Mini-Cheat Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Кнопка включения/выключения Киллауры
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Killaura: " + (AuraMod.auraEnabled ? "§aВКЛ" : "§cВЫКЛ")),
                button -> {
                    AuraMod.auraEnabled = !AuraMod.auraEnabled;
                    button.setMessage(Text.literal("Killaura: " + (AuraMod.auraEnabled ? "§aВКЛ" : "§cВЫКЛ")));
                }
        ).dimensions(centerX - 100, centerY - 25, 200, 20).build());

        // Кнопка закрытия меню
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Закрыть меню"),
                button -> this.close()
        ).dimensions(centerX - 100, centerY + 5, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Затемнение фона игры под меню (выглядит красиво и стильно)
        this.renderBackground(context, mouseX, mouseY, delta);

        // Красивый заголовок с тенью
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 60, 0xFF5555);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(this.parent);
    }
}
