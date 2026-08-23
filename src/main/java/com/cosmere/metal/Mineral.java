package com.cosmere.metal;

/**
 * Non-Invested metals that exist only as alloy ingredients. They get an ore, a raw drop,
 * an ingot and a storage block, but no Allomantic, Feruchemic or Hemalurgic behaviour.
 */
public enum Mineral {
    LEAD("lead", 0x6E6E7A),
    SILVER("silver", 0xE0E6EA),
    NICKEL("nickel", 0xC6C4A8),
    BISMUTH("bismuth", 0xC0A0C8);

    private final String id;
    private final int color;

    Mineral(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return this.id;
    }

    public int color() {
        return this.color;
    }

    public String displayName() {
        return this.id.substring(0, 1).toUpperCase() + this.id.substring(1);
    }
}
