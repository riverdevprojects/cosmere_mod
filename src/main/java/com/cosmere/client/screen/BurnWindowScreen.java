package com.cosmere.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.cosmere.InvestitureData;
import com.cosmere.client.ClientInvestitureCache;
import com.cosmere.feruchemy.FeruchemyMode;
import com.cosmere.metal.Metal;
import com.cosmere.network.c2s.SetFeruchemyModePayload;
import com.cosmere.network.c2s.ToggleBurnPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The burn window: one row per metal you have any claim to.
 *
 * <p>Left half is Allomancy -- click to light or snuff, with the bar showing what is left in
 * your stomach. Right half is Feruchemy, cycling off, storing and tapping. Metals you cannot
 * use at all are not listed; there is no value in showing a Coinshot fifteen switches that do
 * nothing.
 */
public class BurnWindowScreen extends Screen {
    private static final int ROW_HEIGHT = 16;
    private static final int PANEL_WIDTH = 260;
    private static final int BURN_BUTTON_WIDTH = 120;
    private static final int FERU_BUTTON_WIDTH = 96;

    private static final int PANEL_BACKGROUND = 0xC0101018;
    private static final int ROW_IDLE = 0x40FFFFFF;
    private static final int ROW_HOVER = 0x60FFFFFF;
    private static final int RESERVE_BAR = 0x8033A0FF;

    private final List<Metal> rows = new ArrayList<>();
    private int left;
    private int top;

    public BurnWindowScreen() {
        super(Component.translatable("cosmere.screen.burn_window"));
    }

    @Override
    protected void init() {
        this.rows.clear();
        InvestitureData data = ClientInvestitureCache.local();
        for (Metal metal : Metal.values()) {
            if (data.canBurn(metal) || data.canStore(metal)) {
                this.rows.add(metal);
            }
        }
        int height = Math.max(1, this.rows.size()) * ROW_HEIGHT + 28;
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - height) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        InvestitureData data = ClientInvestitureCache.local();
        int panelHeight = Math.max(1, this.rows.size()) * ROW_HEIGHT + 28;

        graphics.fill(this.left - 4, this.top - 4, this.left + PANEL_WIDTH + 4, this.top + panelHeight, PANEL_BACKGROUND);
        graphics.drawString(this.font, this.title, this.left, this.top, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("cosmere.screen.burn_window.hint"),
                this.left, this.top + 10, 0xA0A0A0, false);

        if (this.rows.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("cosmere.screen.burn_window.no_powers"),
                    this.left, this.top + 28, 0xFF7070, false);
            return;
        }

        for (int i = 0; i < this.rows.size(); i++) {
            Metal metal = this.rows.get(i);
            int y = this.top + 24 + i * ROW_HEIGHT;
            renderBurnCell(graphics, data, metal, y, mouseX, mouseY);
            renderFeruchemyCell(graphics, data, metal, y, mouseX, mouseY);
        }
    }

    private void renderBurnCell(GuiGraphics graphics, InvestitureData data, Metal metal, int y, int mouseX, int mouseY) {
        int x = this.left;
        boolean hovered = inside(mouseX, mouseY, x, y, BURN_BUTTON_WIDTH, ROW_HEIGHT - 2);
        graphics.fill(x, y, x + BURN_BUTTON_WIDTH, y + ROW_HEIGHT - 2, hovered ? ROW_HOVER : ROW_IDLE);

        if (data.canBurn(metal)) {
            // The bar behind the name is what is left in the stomach.
            float fill = data.reserve(metal) / InvestitureData.MAX_RESERVE;
            int width = Math.round(BURN_BUTTON_WIDTH * Math.min(1.0F, fill));
            graphics.fill(x, y, x + width, y + ROW_HEIGHT - 2, RESERVE_BAR);
        }

        int colour = data.isBurning(metal) ? 0xFF8040 : (data.canBurn(metal) ? 0xFFFFFF : 0x606060);
        graphics.drawString(this.font, Component.translatable(metal.translationKey()), x + 4, y + 4, colour, false);
        if (data.isFlaring(metal)) {
            graphics.drawString(this.font, "▲", x + BURN_BUTTON_WIDTH - 12, y + 4, 0xFFC040, false);
        }
    }

    private void renderFeruchemyCell(GuiGraphics graphics, InvestitureData data, Metal metal, int y, int mouseX, int mouseY) {
        int x = this.left + BURN_BUTTON_WIDTH + 8;
        boolean hovered = inside(mouseX, mouseY, x, y, FERU_BUTTON_WIDTH, ROW_HEIGHT - 2);
        graphics.fill(x, y, x + FERU_BUTTON_WIDTH, y + ROW_HEIGHT - 2, hovered ? ROW_HOVER : ROW_IDLE);

        FeruchemyMode mode = data.feruchemyMode(metal);
        int colour = switch (mode) {
            case OFF -> data.canStore(metal) ? 0xFFFFFF : 0x606060;
            case STORING -> 0x70C0FF;
            case TAPPING -> 0xFFD070;
        };
        graphics.drawString(this.font, Component.translatable(mode.translationKey()), x + 4, y + 4, colour, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        InvestitureData data = ClientInvestitureCache.local();
        for (int i = 0; i < this.rows.size(); i++) {
            Metal metal = this.rows.get(i);
            int y = this.top + 24 + i * ROW_HEIGHT;

            if (inside(mouseX, mouseY, this.left, y, BURN_BUTTON_WIDTH, ROW_HEIGHT - 2) && data.canBurn(metal)) {
                boolean nowBurning = !data.isBurning(metal);
                data.setBurning(metal, nowBurning);
                PacketDistributor.sendToServer(new ToggleBurnPayload(metal, nowBurning));
                return true;
            }

            int feruX = this.left + BURN_BUTTON_WIDTH + 8;
            if (inside(mouseX, mouseY, feruX, y, FERU_BUTTON_WIDTH, ROW_HEIGHT - 2) && data.canStore(metal)) {
                FeruchemyMode next = data.feruchemyMode(metal).next();
                data.setFeruchemyMode(metal, next);
                PacketDistributor.sendToServer(new SetFeruchemyModePayload(metal, next));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
