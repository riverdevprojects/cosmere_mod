package com.cosmere.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * The three keys Allomancy needs.
 *
 * <p>F arms and disarms; it does not light anything. R opens the burn window. Caps Lock flares
 * whatever is already alight, for as long as it is held.
 *
 * <p>F collides with vanilla's swap-hands, which is intentional -- the brief asked for F -- and
 * {@code ClientEvents} suppresses the vanilla binding only while the player actually has
 * Allomancy to use.
 */
public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories.cosmere";

    /** Arms Steelpush, Ironpull, Leeching and Nicrobursting. */
    public static final KeyMapping TOGGLE_ARMED = new KeyMapping(
            "key.cosmere.toggle_armed", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY);

    /** Opens the burn window: one switch per metal you can burn. */
    public static final KeyMapping BURN_WINDOW = new KeyMapping(
            "key.cosmere.burn_window", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** Held to flare: hotter, stronger, and far more expensive. */
    public static final KeyMapping FLARE = new KeyMapping(
            "key.cosmere.flare", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_CAPS_LOCK, CATEGORY);

    private ModKeyMappings() {
    }
}
