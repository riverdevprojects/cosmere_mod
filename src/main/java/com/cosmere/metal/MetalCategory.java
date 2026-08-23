package com.cosmere.metal;

import net.minecraft.util.StringRepresentable;

/**
 * Groupings used by every metallic art. Allomancy and Feruchemy share the first four
 * groups; Feruchemy replaces Enhancement with Spiritual, so both names live here and
 * {@link Metal} records the category that matters for each art.
 */
public enum MetalCategory implements StringRepresentable {
    PHYSICAL("physical"),
    MENTAL("mental"),
    TEMPORAL("temporal"),
    ENHANCEMENT("enhancement"),
    SPIRITUAL("spiritual"),
    HYBRID("hybrid"),
    GOD("god");

    public static final StringRepresentable.EnumCodec<MetalCategory> CODEC = StringRepresentable.fromEnum(MetalCategory::values);

    private final String name;

    MetalCategory(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public String translationKey() {
        return "cosmere.metal_category." + this.name;
    }
}
