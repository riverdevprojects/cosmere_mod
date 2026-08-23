package com.cosmere.client.hud;

import java.util.Map;

import com.cosmere.client.ClientInvestitureCache;
import com.cosmere.metal.Metal;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * What a Seeker sees when they hear something.
 *
 * <p>Each Allomancer within earshot gets a Steel Alphabet glyph floating over them, one per
 * metal they are burning. Someone under a copper cloud shows a smeared marker instead: bronze
 * still knows a person is there, it simply cannot make out the rhythm.
 */
public class SeekerOverlay implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
            return;
        }
        if (!ClientInvestitureCache.local().isBurning(Metal.BRONZE)) {
            return;
        }

        Map<Integer, ClientInvestitureCache.Pulse> pulses = ClientInvestitureCache.pulses();
        if (pulses.isEmpty()) {
            return;
        }

        Matrix4f projection = new Matrix4f(minecraft.gameRenderer.getProjectionMatrix(
                minecraft.options.fov().get().doubleValue()));
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().getPosition();

        for (Map.Entry<Integer, ClientInvestitureCache.Pulse> entry : pulses.entrySet()) {
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            Vec3 head = entity.position().add(0.0D, entity.getBbHeight() + 0.6D, 0.0D).subtract(cameraPos);
            Vector4f screen = project(minecraft, projection, head);
            if (screen == null) {
                continue;
            }
            drawGlyphs(graphics, minecraft, entry.getValue(), (int) screen.x, (int) screen.y);
        }
    }

    private void drawGlyphs(GuiGraphics graphics, Minecraft minecraft,
                            ClientInvestitureCache.Pulse pulse, int x, int y) {
        if (pulse.muffled()) {
            graphics.drawCenteredString(minecraft.font, "~", x, y, 0x60A0FF);
            return;
        }
        StringBuilder glyphs = new StringBuilder();
        for (Metal metal : pulse.metals()) {
            glyphs.append(SteelAlphabet.glyphFor(metal));
        }
        graphics.drawCenteredString(minecraft.font, glyphs.toString(), x, y, 0x80D0FF);
    }

    /**
     * World point to screen point. Returns null when the point is behind the camera, where the
     * projection would fold it back onto the screen in the wrong place.
     */
    private static Vector4f project(Minecraft minecraft, Matrix4f projection, Vec3 relative) {
        Matrix4f view = new Matrix4f().rotation(minecraft.gameRenderer.getMainCamera().rotation()).invert();
        Vector4f point = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1.0F);
        view.transform(point);
        if (point.z > 0.0F) {
            return null;
        }
        projection.transform(point);
        if (Math.abs(point.w) < 1.0E-4F) {
            return null;
        }
        point.mul(1.0F / point.w);
        float screenX = (point.x * 0.5F + 0.5F) * graphicsWidth(minecraft);
        float screenY = (1.0F - (point.y * 0.5F + 0.5F)) * graphicsHeight(minecraft);
        return new Vector4f(screenX, screenY, 0.0F, 1.0F);
    }

    private static float graphicsWidth(Minecraft minecraft) {
        return minecraft.getWindow().getGuiScaledWidth();
    }

    private static float graphicsHeight(Minecraft minecraft) {
        return minecraft.getWindow().getGuiScaledHeight();
    }
}
