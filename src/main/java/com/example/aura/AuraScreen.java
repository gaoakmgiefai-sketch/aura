package com.example.aura;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AuraScreen extends Screen {
    private final Screen parent;

    public AuraScreen(Screen parent) {
        super(Text.literal("Aura Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        // Кнопка включения/выключения киллауры
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Killaura: " + (AuraMod.auraEnabled ? "§aВКЛ" : "§cВЫКЛ")),
                button -> {
                    AuraMod.auraEnabled = !AuraMod.auraEnabled;
                    button.setMessage(Text.literal("Killaura: " + (AuraMod.auraEnabled ? "§aВКЛ" : "§cВЫКЛ")));
                }
        ).dimensions(this.width / 2 - 100, this.height / 2 - 20, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // В Minecraft 1.21+ метод renderBackground принимает только context
        this.renderBackground(context);
        
        super.render(context, mouseX, mouseY, delta);
        
        // Исправленный метод отрисовки текста по центру для версии 1.21.8
        context.drawCenteredText(this.textRenderer, this.title, this.width / 2, 30, 0xFFFFFF);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
