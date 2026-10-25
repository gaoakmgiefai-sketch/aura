package com.example.aura;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class AuraScreen extends Screen {
    private ButtonWidget toggleBtn;
    private ButtonWidget rangeBtn;

    public AuraScreen() {
        super(Text.literal("Aura"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        toggleBtn = ButtonWidget.builder(label(), b -> {
            AuraMod.enabled = !AuraMod.enabled;
            b.setMessage(label());
        }).dimensions(cx - 75, cy - 25, 150, 20).build();

        rangeBtn = ButtonWidget.builder(rangeLabel(), b -> {
            AuraMod.range += 0.5;
            if (AuraMod.range > 6.0) AuraMod.range = 3.0;
            b.setMessage(rangeLabel());
        }).dimensions(cx - 75, cy + 5, 150, 20).build();

        addDrawableChild(toggleBtn);
        addDrawableChild(rangeBtn);
    }

    private Text label() {
        return Text.literal("KillAura: " + (AuraMod.enabled ? "ON" : "OFF"));
    }

    private Text rangeLabel() {
        return Text.literal("Range: " + AuraMod.range);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 45, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
