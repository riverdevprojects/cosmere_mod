package com.cosmere.dimension;

import java.util.List;

import com.cosmere.Cosmere;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Where the other worlds will go.
 *
 * <p>Nothing here is registered yet, and that is deliberate: Scadrial's systems are built and
 * playable in the Overworld first, and adding a dimension is a content decision rather than an
 * architectural one. What this class does is fix the shape of that decision now, so the rest of
 * the mod can already ask "which planet am I on?" and get a real answer.
 *
 * <p>Scadrial currently <em>is</em> the Overworld. When a dedicated Scadrial dimension is
 * added, only {@link #SCADRIAL}'s key changes and everything that consults {@link #of} follows.
 *
 * <h2>Adding a planet</h2>
 * <ol>
 *   <li>Add a constant to {@link Planet} with its dimension id.</li>
 *   <li>Add the JSON under {@code data/cosmere/dimension/} and {@code dimension_type/} --
 *       worldgen is datapack-driven in 1.21, so no code is needed for the dimension itself.</li>
 *   <li>Gate any planet-specific mechanic on {@link #of}. Allomancy already works anywhere,
 *       which is correct: a Mistborn on Roshar is still a Mistborn.</li>
 * </ol>
 */
public final class CosmerePlanets {
    /** A world in the Cosmere, and the Minecraft dimension that stands in for it. */
    public enum Planet {
        /** Home of the metallic arts. Presently the Overworld itself. */
        SCADRIAL("scadrial", Level.OVERWORLD),
        /** Not yet present. Listed so the shape of the enum is honest about the intent. */
        ROSHAR("roshar", null),
        /** Not yet present. */
        NALTHIS("nalthis", null),
        /** Not yet present. */
        SEL("sel", null),
        /** Not yet present. */
        TALDAIN("taldain", null);

        private final String id;
        private final ResourceKey<Level> dimension;

        Planet(String id, ResourceKey<Level> dimension) {
            this.id = id;
            this.dimension = dimension;
        }

        public String id() {
            return this.id;
        }

        /** The dimension this planet lives in, or null while the planet is not implemented. */
        public ResourceKey<Level> dimension() {
            return this.dimension;
        }

        public boolean isImplemented() {
            return this.dimension != null;
        }

        public String translationKey() {
            return "cosmere.planet." + this.id;
        }
    }

    public static final Planet SCADRIAL = Planet.SCADRIAL;

    /** Planets that actually exist in-game right now. */
    public static List<Planet> implemented() {
        return java.util.Arrays.stream(Planet.values()).filter(Planet::isImplemented).toList();
    }

    /** Which planet a dimension belongs to. Unknown dimensions are treated as Scadrial. */
    public static Planet of(ResourceKey<Level> dimension) {
        for (Planet planet : Planet.values()) {
            if (dimension.equals(planet.dimension())) {
                return planet;
            }
        }
        return Planet.SCADRIAL;
    }

    /**
     * The key a future Scadrial dimension will use. Not registered; kept here so the id is
     * decided once rather than guessed at in five places later.
     */
    public static final ResourceKey<Level> FUTURE_SCADRIAL_KEY =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "scadrial"));

    private CosmerePlanets() {
    }
}
