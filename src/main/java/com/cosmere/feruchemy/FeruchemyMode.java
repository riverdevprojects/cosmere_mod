package com.cosmere.feruchemy;

import net.minecraft.util.StringRepresentable;

/** What a Feruchemist is doing with a given metalmind right now. */
public enum FeruchemyMode implements StringRepresentable {
    /** Not touching the metalmind. */
    OFF("off"),
    /** Paying an attribute in, taking the penalty now for a benefit later. */
    STORING("storing"),
    /** Drawing the attribute back out, compressed. */
    TAPPING("tapping");

    public static final StringRepresentable.EnumCodec<FeruchemyMode> CODEC = StringRepresentable.fromEnum(FeruchemyMode::values);

    private final String name;

    FeruchemyMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public FeruchemyMode next() {
        return switch (this) {
            case OFF -> STORING;
            case STORING -> TAPPING;
            case TAPPING -> OFF;
        };
    }

    public String translationKey() {
        return "cosmere.feruchemy.mode." + this.name;
    }
}
