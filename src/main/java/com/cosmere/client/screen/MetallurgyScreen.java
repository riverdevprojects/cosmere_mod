package com.cosmere.client.screen;

import com.cosmere.menu.MetallurgyMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Metallurgy Table screen.
 *
 * <p>Drawn from primitives rather than a background sheet, so the layout stays in one place
 * and adding a fifth crucible slot later does not mean re-cutting a PNG.
 */
public class MetallurgyScreen extends AbstractContainerScreen<MetallurgyMenu> {
    private static final int PANEL = 0xFF3B3B42;
    private static final int PANEL_LIGHT = 0xFF55555E;
    private static final int PANEL_DARK = 0xFF23232A;
    private static final int SLOT = 0xFF16161B;

    public MetallurgyScreen(MetallurgyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, PANEL);
        graphics.fill(x, y, x + this.imageWidth, y + 1, PANEL_LIGHT);
        graphics.fill(x, y, x + 1, y + this.imageHeight, PANEL_LIGHT);
        graphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, PANEL_DARK);
        graphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, PANEL_DARK);

        for (var slot : this.menu.slots) {
            graphics.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT);
        }

        // An arrow from the crucible to the output, so the flow is obvious without a tooltip.
        int arrowY = y + 39;
        graphics.fill(x + 80, arrowY, x + 108, arrowY + 4, PANEL_LIGHT);
        graphics.fill(x + 104, arrowY - 3, x + 108, arrowY + 7, PANEL_LIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
