package com.cosmere.metal;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.util.StringRepresentable;

/**
 * Every Invested metal the mod knows about.
 *
 * <p>A metal is the single source of truth for its three arts: which Allomantic power it
 * fuels, which attribute it stores Feruchemically, and what a Hemalurgic spike of it steals.
 * Registration ({@code ModItems}, {@code ModBlocks}) and every gameplay handler iterate this
 * enum rather than hard-coding names, so adding a metal is a one-line change here plus its
 * effect implementation.
 *
 * <p>Ordering is deliberate: base sixteen first, grouped in Allomantic quadrants and always
 * pure-then-alloy, followed by the god metals. UI that lists metals relies on this order.
 */
public enum Metal implements StringRepresentable {
    // --- Physical ---
    IRON("iron", MetalCategory.PHYSICAL, MetalCategory.PHYSICAL, MetalCategory.PHYSICAL, 0xD8D8D8, Source.VANILLA),
    STEEL("steel", MetalCategory.PHYSICAL, MetalCategory.PHYSICAL, MetalCategory.PHYSICAL, 0x8C8C8C, Source.ALLOY),
    TIN("tin", MetalCategory.PHYSICAL, MetalCategory.PHYSICAL, MetalCategory.PHYSICAL, 0xE4EEF2, Source.ORE),
    PEWTER("pewter", MetalCategory.PHYSICAL, MetalCategory.PHYSICAL, MetalCategory.PHYSICAL, 0xA9A9B4, Source.ALLOY),

    // --- Mental ---
    ZINC("zinc", MetalCategory.MENTAL, MetalCategory.MENTAL, MetalCategory.MENTAL, 0xBFC9CC, Source.ORE),
    BRASS("brass", MetalCategory.MENTAL, MetalCategory.MENTAL, MetalCategory.MENTAL, 0xD6B44A, Source.ALLOY),
    COPPER("copper", MetalCategory.MENTAL, MetalCategory.MENTAL, MetalCategory.MENTAL, 0xC17E4C, Source.VANILLA),
    BRONZE("bronze", MetalCategory.MENTAL, MetalCategory.MENTAL, MetalCategory.MENTAL, 0xB07B3A, Source.ALLOY),

    // --- Temporal ---
    CADMIUM("cadmium", MetalCategory.TEMPORAL, MetalCategory.TEMPORAL, MetalCategory.TEMPORAL, 0xB9C4B0, Source.ORE),
    BENDALLOY("bendalloy", MetalCategory.TEMPORAL, MetalCategory.TEMPORAL, MetalCategory.SPIRITUAL, 0xE0D9A8, Source.ALLOY),
    GOLD("gold", MetalCategory.TEMPORAL, MetalCategory.TEMPORAL, MetalCategory.HYBRID, 0xF5D14A, Source.VANILLA),
    ELECTRUM("electrum", MetalCategory.TEMPORAL, MetalCategory.TEMPORAL, MetalCategory.ENHANCEMENT, 0xF0E2A0, Source.ALLOY),

    // --- Enhancement (Allomancy) / Spiritual (Feruchemy) ---
    ALUMINUM("aluminum", MetalCategory.ENHANCEMENT, MetalCategory.SPIRITUAL, MetalCategory.ENHANCEMENT, 0xDCE2E5, Source.ORE),
    DURALUMIN("duralumin", MetalCategory.ENHANCEMENT, MetalCategory.SPIRITUAL, MetalCategory.ENHANCEMENT, 0xC9CED2, Source.ALLOY),
    CHROMIUM("chromium", MetalCategory.ENHANCEMENT, MetalCategory.SPIRITUAL, MetalCategory.ENHANCEMENT, 0xB6C6CE, Source.ORE),
    NICROSIL("nicrosil", MetalCategory.ENHANCEMENT, MetalCategory.SPIRITUAL, MetalCategory.ENHANCEMENT, 0xA8B8B0, Source.ALLOY),

    // --- God metals ---
    ATIUM("atium", MetalCategory.GOD, MetalCategory.GOD, MetalCategory.GOD, 0x6E6E86, Source.ORE),
    LERASIUM("lerasium", MetalCategory.GOD, MetalCategory.GOD, MetalCategory.GOD, 0xE8E4D0, Source.NONE),
    MALATIUM("malatium", MetalCategory.GOD, MetalCategory.GOD, MetalCategory.GOD, 0x9A8E7A, Source.ALLOY),
    HARMONIUM("harmonium", MetalCategory.GOD, MetalCategory.GOD, MetalCategory.GOD, 0xD0D6E4, Source.ALLOY),
    TRELLIUM("trellium", MetalCategory.GOD, MetalCategory.GOD, MetalCategory.GOD, 0x8C4A4A, Source.NONE);

    /** How the raw metal enters the world, which decides whether it gets an ore and a raw item. */
    public enum Source {
        /** Minecraft already provides the ore, ingot and nugget; we add only the block/nugget gaps. */
        VANILLA,
        /** We generate an ore block and a raw drop for it. */
        ORE,
        /** Only obtainable through the Metallurgy Table. */
        ALLOY,
        /** Not obtainable by ordinary means -- loot, bosses, or creative only. */
        NONE
    }

    public static final StringRepresentable.EnumCodec<Metal> CODEC = StringRepresentable.fromEnum(Metal::values);

    /** The base sixteen, in Allomantic table order. Excludes god metals. */
    public static final List<Metal> BASE_SIXTEEN = List.of(
            IRON, STEEL, TIN, PEWTER,
            ZINC, BRASS, COPPER, BRONZE,
            CADMIUM, BENDALLOY, GOLD, ELECTRUM,
            ALUMINUM, DURALUMIN, CHROMIUM, NICROSIL);

    public static final List<Metal> GOD_METALS = List.of(ATIUM, LERASIUM, MALATIUM, HARMONIUM, TRELLIUM);

    private final String id;
    private final MetalCategory allomanticCategory;
    private final MetalCategory feruchemicCategory;
    private final MetalCategory hemalurgicCategory;
    private final int color;
    private final Source source;

    Metal(String id, MetalCategory allomantic, MetalCategory feruchemic, MetalCategory hemalurgic, int color, Source source) {
        this.id = id;
        this.allomanticCategory = allomantic;
        this.feruchemicCategory = feruchemic;
        this.hemalurgicCategory = hemalurgic;
        this.color = color;
        this.source = source;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public String id() {
        return this.id;
    }

    public MetalCategory allomanticCategory() {
        return this.allomanticCategory;
    }

    public MetalCategory feruchemicCategory() {
        return this.feruchemicCategory;
    }

    public MetalCategory hemalurgicCategory() {
        return this.hemalurgicCategory;
    }

    /** Packed 0xRRGGBB, used for tooltips, blue lines, particles and item tints. */
    public int color() {
        return this.color;
    }

    public Source source() {
        return this.source;
    }

    public boolean isGodMetal() {
        return this.allomanticCategory == MetalCategory.GOD;
    }

    /** True when the mod (not vanilla) supplies the ore block and raw drop. */
    public boolean hasOwnOre() {
        return this.source == Source.ORE;
    }

    /** True when the mod supplies ingot/nugget/block items; vanilla metals reuse vanilla items. */
    public boolean hasOwnIngot() {
        return this.source != Source.VANILLA;
    }

    /**
     * True when the mod has to supply the nugget. Vanilla ships iron and gold nuggets but not
     * copper ones, and Feruchemy needs a copper nugget for copperminds.
     */
    public boolean hasOwnNugget() {
        return hasOwnIngot() || this == COPPER;
    }

    /** Human-readable name used when building lang entries. */
    public String displayName() {
        return this.id.substring(0, 1).toUpperCase(Locale.ROOT) + this.id.substring(1);
    }

    public String translationKey() {
        return "cosmere.metal." + this.id;
    }

    public static Optional<Metal> byId(String id) {
        for (Metal metal : values()) {
            if (metal.id.equals(id)) {
                return Optional.of(metal);
            }
        }
        return Optional.empty();
    }
}
