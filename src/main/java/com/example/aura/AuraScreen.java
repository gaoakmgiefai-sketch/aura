package com.example.aura;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AuraScreen extends Screen {
    private final Screen parent;

    public AuraScreen(Screen parent) {
        super(Text.literal("Aura Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(ButtonWidget.builder(
                getButtonText(),
                button -> {
                    AuraMod.auraEnabled = !AuraMod.auraEnabled;
                    button.setMessage(getButtonText());
                }
        ).dimensions(this.width / 2 - 100, this.height / 2 - 20, 200, 20).build());
    }

    private Text getButtonText() {
        Text status = AuraMod.auraEnabled 
            ? Text.literal("ВКЛ").formatted(Formatting.GREEN) 
            : Text.literal("ВЫКЛ").formatted(Formatting.RED);
            
        return Text.literal("Killaura: ").append(status);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        
        String titleText = this.title.getString();
        int textX = (this.width - this.textRenderer.getWidth(titleText)) / 2;
        context.drawText(this.textRenderer, titleText, textX, 30, 0xFFFFFF, true);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
