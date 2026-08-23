package com.cosmere;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side knobs.
 *
 * <p>Kept deliberately small: these are the settings that change how a world plays rather than
 * how one client looks. Balance numbers that belong to a mechanic live with the mechanic.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("The mists: night atmosphere, mistwraiths, and Snapping.").push("mists");
    }

    public static final ModConfigSpec.BooleanValue MISTS_ENABLED = BUILDER
            .comment("Whether the mists roll out at night in the Overworld.")
            .define("mistsEnabled", true);

    public static final ModConfigSpec.BooleanValue SNAPPING_ENABLED = BUILDER
            .comment("Whether nearly dying under the mists can Snap a player into a Misting.")
            .define("snappingEnabled", true);

    public static final ModConfigSpec.DoubleValue MISTWRAITH_SPAWN_CHANCE = BUILDER
            .comment("Chance per ten-second window that a mistwraith crawls out near a player.")
            .defineInRange("mistwraithSpawnChance", 0.08D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue MISTWRAITH_CAP = BUILDER
            .comment("Maximum mistwraiths allowed near one player before the mists stop producing more.")
            .defineInRange("mistwraithCap", 4, 0, 64);

    static {
        BUILDER.pop();
        BUILDER.comment("Allomancy.").push("allomancy");
    }

    public static final ModConfigSpec.DoubleValue PUSH_STRENGTH = BUILDER
            .comment("Global multiplier on Steelpush and Ironpull force.")
            .defineInRange("pushStrength", 1.0D, 0.1D, 10.0D);

    public static final ModConfigSpec.IntValue BLUE_LINE_RANGE = BUILDER
            .comment("How far iron and steel show blue lines to nearby metal, in blocks.")
            .defineInRange("blueLineRange", 16, 4, 48);

    public static final ModConfigSpec.BooleanValue ALLOW_LEECHING_PLAYERS = BUILDER
            .comment("Whether chromium can strip another player's reserves.")
            .define("allowLeechingPlayers", true);

    static {
        BUILDER.pop();
        BUILDER.comment("Hemalurgy.").push("hemalurgy");
    }

    public static final ModConfigSpec.BooleanValue SPIKES_KILL_OVER_LIMIT = BUILDER
            .comment("Whether exceeding the spike limit tears the spiritweb apart and clears the respawn point.")
            .define("spikesKillOverLimit", true);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
