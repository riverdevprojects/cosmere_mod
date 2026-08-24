package com.cosmere.client;

import java.util.List;

import com.cosmere.Config;
import com.cosmere.allomancy.MetalScanner;
import com.cosmere.allomancy.MetalTarget;
import com.cosmere.metal.Metal;

import net.minecraft.client.Minecraft;

/**
 * The metal an Allomancer can currently see, refreshed on an interval rather than per frame.
 *
 * <p>Scanning a cube of blocks is far too expensive to do sixty times a second, and the lines
 * do not need to be that fresh -- a quarter of a second of lag on a wall of iron is invisible.
 */
public final class AllomanticLineCache {
    /** Ticks between rescans. */
    public static final int RESCAN_INTERVAL = 5;

    private static List<MetalTarget> targets = List.of();
    private static int cooldown;

    public static List<MetalTarget> targets() {
        return targets;
    }

    /** Called from the client tick. Clears the cache when neither iron nor steel is alight. */
    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            targets = List.of();
            return;
        }
        var data = ClientInvestitureCache.local();
        if (!data.isBurning(Metal.IRON) && !data.isBurning(Metal.STEEL)) {
            targets = List.of();
            return;
        }
        if (--cooldown > 0) {
            return;
        }
        cooldown = RESCAN_INTERVAL;
        targets = MetalScanner.scan(minecraft.level, minecraft.player.getEyePosition(),
                Config.BLUE_LINE_RANGE.getAsInt(), true, minecraft.player);
    }

    private AllomanticLineCache() {
    }
}
