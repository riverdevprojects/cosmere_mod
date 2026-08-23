package com.cosmere.client.hud;

import java.util.List;
import java.util.Map;

import com.cosmere.InvestitureData;
import com.cosmere.client.ClientEvents;
import com.cosmere.client.ClientInvestitureCache;
import com.cosmere.metal.Metal;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * What the Allomancer can see about their own state.
 *
 * <p>A stack of reserve bars for whatever is burning, the armed indicator, and -- while pewter
 * is alight -- a second health bar showing what the body actually has left underneath the
 * damage pewter is refusing to acknowledge.
 */
public class AllomancyHud implements LayeredDraw.Layer {
    private static final int BAR_WIDTH = 60;
    private static final int BAR_HEIGHT = 4;
    private static final int BAR_SPACING = 6;
    private static final int MARGIN = 6;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        InvestitureData data = ClientInvestitureCache.local();
        Map<Metal, Float> reserves = data.allReserves();

        int x = MARGIN;
        int y = MARGIN;

        if (ClientEvents.isArmed()) {
            graphics.drawString(minecraft.font, Component.translatable("cosmere.hud.armed"), x, y, 0xFFB030, true);
            y += 10;
        }

        // One bar per metal in the stomach, brightened while it is burning.
        List<Metal> ordered = Metal.BASE_SIXTEEN;
        for (Metal metal : ordered) {
            Float reserve = reserves.get(metal);
            if (reserve == null || reserve <= 0.0F) {
                continue;
            }
            renderReserveBar(graphics, minecraft, data, metal, x, y, reserve);
            y += BAR_SPACING + 4;
        }
        for (Metal metal : Metal.GOD_METALS) {
            Float reserve = reserves.get(metal);
            if (reserve != null && reserve > 0.0F) {
                renderReserveBar(graphics, minecraft, data, metal, x, y, reserve);
                y += BAR_SPACING + 4;
            }
        }

        renderPewterDrag(graphics, minecraft, data);
    }

    private void renderReserveBar(GuiGraphics graphics, Minecraft minecraft, InvestitureData data,
                                  Metal metal, int x, int y, float reserve) {
        boolean burning = data.isBurning(metal);
        int filled = Math.round(BAR_WIDTH * Math.min(1.0F, reserve / InvestitureData.MAX_RESERVE));

        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x80000000);
        int colour = 0xFF000000 | metal.color();
        graphics.fill(x, y, x + filled, y + BAR_HEIGHT, burning ? colour : dim(colour));
        if (data.isFlaring(metal)) {
            graphics.fill(x, y - 1, x + filled, y, 0xFFFFC040);
        }
        graphics.drawString(minecraft.font, Component.translatable(metal.translationKey()),
                x + BAR_WIDTH + 4, y - 2, burning ? 0xFFFFFF : 0x808080, false);
    }

    /**
     * Pewter's second bar. The red hearts show what the body believes; this shows what is
     * actually left, and the gap is what lands the moment pewter runs out.
     */
    private void renderPewterDrag(GuiGraphics graphics, Minecraft minecraft, InvestitureData data) {
        if (!data.isBurning(Metal.PEWTER) || minecraft.player == null) {
            return;
        }
        float debt = data.pewterDebt();
        if (debt <= 0.0F) {
            return;
        }
        float real = Math.max(0.0F, minecraft.player.getHealth() - debt);
        float max = minecraft.player.getMaxHealth();

        int width = 80;
        int x = (graphics.guiWidth() - width) / 2;
        int y = graphics.guiHeight() - 60;

        graphics.fill(x, y, x + width, y + 5, 0x80000000);
        graphics.fill(x, y, x + Math.round(width * (real / max)), y + 5, 0xFF8B1A1A);
        graphics.drawString(minecraft.font, Component.translatable("cosmere.hud.pewter_drag"),
                x, y - 10, 0xFF7070, true);
    }

    private static int dim(int colour) {
        int r = ((colour >> 16) & 0xFF) / 3;
        int g = ((colour >> 8) & 0xFF) / 3;
        int b = (colour & 0xFF) / 3;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
