package com.cosmere.client.screen;

import java.util.List;

import com.cosmere.client.ClientInvestitureCache;
import com.cosmere.hemalurgy.HemalurgyData;
import com.cosmere.hemalurgy.SpikeSlot;
import com.cosmere.item.SpikeItem;
import com.cosmere.menu.SpikeJarMenu;
import com.cosmere.metal.Metal;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Spikes on the left, your own body on the right.
 *
 * <p>Pick a spike up onto the cursor and the diagram lights up: green where it will take,
 * greyed where the body cannot hold that kind of power or has no room left. Clicking a lit
 * spot drives the spike in, which hurts and is not reversible.
 */
public class SpikeJarScreen extends AbstractContainerScreen<SpikeJarMenu> {
    private static final int PANEL = 0xFF3B2B2B;
    private static final int PANEL_LIGHT = 0xFF554040;
    private static final int PANEL_DARK = 0xFF231A1A;
    private static final int SLOT = 0xFF1A1010;

    private static final int DIAGRAM_X = 186;
    private static final int DIAGRAM_Y = 18;

    private static final int SLOT_EMPTY = 0xFF4A4A50;
    private static final int SLOT_FILLED = 0xFFB03030;
    private static final int SLOT_VALID = 0xFF40C060;
    private static final int SLOT_INVALID = 0xFF2A2A2E;

    public SpikeJarScreen(SpikeJarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 284;
        this.imageHeight = 188;
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

        renderBodyDiagram(graphics, mouseX, mouseY);
    }

    /** The body: a crude outline with one marker per {@link SpikeSlot}. */
    private void renderBodyDiagram(GuiGraphics graphics, int mouseX, int mouseY) {
        int originX = this.leftPos + DIAGRAM_X;
        int originY = this.topPos + DIAGRAM_Y;

        graphics.fill(originX, originY, originX + 88, originY + 120, 0xFF2A1E1E);
        // Head, torso, arms, legs -- enough shape to read as a person at this size.
        graphics.fill(originX + 36, originY + 8, originX + 52, originY + 28, 0xFF4A3838);
        graphics.fill(originX + 30, originY + 30, originX + 58, originY + 76, 0xFF4A3838);
        graphics.fill(originX + 18, originY + 32, originX + 30, originY + 70, 0xFF3F3030);
        graphics.fill(originX + 58, originY + 32, originX + 70, originY + 70, 0xFF3F3030);
        graphics.fill(originX + 32, originY + 76, originX + 42, originY + 112, 0xFF3F3030);
        graphics.fill(originX + 46, originY + 76, originX + 56, originY + 112, 0xFF3F3030);

        HemalurgyData hemalurgy = ClientInvestitureCache.local().hemalurgy();
        Metal carried = carriedSpikeMetal();

        for (SpikeSlot slot : SpikeSlot.values()) {
            int sx = originX + slot.diagramX() - 3;
            int sy = originY + slot.diagramY() - 3;
            int used = hemalurgy.usedCapacity(slot);
            int colour;
            if (carried != null) {
                colour = hemalurgy.canAccept(carried, slot) ? SLOT_VALID : SLOT_INVALID;
            } else {
                colour = used > 0 ? SLOT_FILLED : SLOT_EMPTY;
            }
            graphics.fill(sx, sy, sx + 6, sy + 6, colour);
            if (used > 0) {
                graphics.drawString(this.font, String.valueOf(used), sx + 8, sy - 1, 0xFFC0C0, false);
            }
        }
    }

    @Nullable
    private Metal carriedSpikeMetal() {
        ItemStack carried = this.menu.getCarried();
        return carried.getItem() instanceof SpikeItem spike ? spike.metal() : null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        SpikeSlot hovered = hoveredSlot(mouseX, mouseY);
        if (hovered != null && carriedSpikeMetal() != null && this.minecraft != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, hovered.ordinal());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        SpikeSlot hovered = hoveredSlot(mouseX, mouseY);
        if (hovered != null) {
            HemalurgyData hemalurgy = ClientInvestitureCache.local().hemalurgy();
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable(hovered.translationKey()),
                    Component.translatable("cosmere.screen.spike_jar.capacity",
                            hemalurgy.usedCapacity(hovered), hovered.capacity()).withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
        }
    }

    @Nullable
    private SpikeSlot hoveredSlot(double mouseX, double mouseY) {
        int originX = this.leftPos + DIAGRAM_X;
        int originY = this.topPos + DIAGRAM_Y;
        for (SpikeSlot slot : SpikeSlot.values()) {
            int sx = originX + slot.diagramX() - 4;
            int sy = originY + slot.diagramY() - 4;
            if (mouseX >= sx && mouseX < sx + 8 && mouseY >= sy && mouseY < sy + 8) {
                return slot;
            }
        }
        return null;
    }
}
