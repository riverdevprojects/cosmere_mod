package com.cosmere.hemalurgy;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import com.cosmere.metal.Metal;
import com.cosmere.metal.MetalCategory;

import net.minecraft.util.StringRepresentable;

/**
 * A place on the body a spike can be driven, its capacity, and where it sits on the body
 * diagram in the Jar of Spikes screen.
 *
 * <p>Placement is not cosmetic: a spike only takes hold where the body can carry that kind of
 * power, which is why the screen greys out slots that reject the spike you are dragging.
 * Total capacity across all slots is 52, comfortably above the
 * {@link HemalurgyData#LETHAL_SPIKE_COUNT} at which a spiritweb tears apart.
 */
public enum SpikeSlot implements StringRepresentable {
    LEFT_EAR("left_ear", 2, 30, 22, MetalCategory.MENTAL),
    RIGHT_EAR("right_ear", 2, 58, 22, MetalCategory.MENTAL),
    LEFT_EYE("left_eye", 1, 38, 16, MetalCategory.MENTAL, MetalCategory.GOD),
    RIGHT_EYE("right_eye", 1, 50, 16, MetalCategory.MENTAL, MetalCategory.GOD),
    LEFT_SHOULDER("left_shoulder", 3, 26, 38, MetalCategory.ENHANCEMENT, MetalCategory.GOD),
    RIGHT_SHOULDER("right_shoulder", 3, 62, 38, MetalCategory.ENHANCEMENT, MetalCategory.GOD),
    LEFT_ARM("left_arm", 3, 18, 54, MetalCategory.PHYSICAL),
    RIGHT_ARM("right_arm", 3, 70, 54, MetalCategory.PHYSICAL),
    CHEST("chest", 4, 44, 44),
    LEFT_RIBS("left_ribs", 6, 34, 56, MetalCategory.PHYSICAL, MetalCategory.TEMPORAL),
    RIGHT_RIBS("right_ribs", 6, 54, 56, MetalCategory.PHYSICAL, MetalCategory.TEMPORAL),
    ABDOMEN("abdomen", 4, 44, 68, MetalCategory.TEMPORAL, MetalCategory.SPIRITUAL),
    SPINE("spine", 6, 44, 80),
    LEFT_LEG("left_leg", 4, 36, 96, MetalCategory.PHYSICAL),
    RIGHT_LEG("right_leg", 4, 52, 96, MetalCategory.PHYSICAL);

    public static final StringRepresentable.EnumCodec<SpikeSlot> CODEC = StringRepresentable.fromEnum(SpikeSlot::values);

    private final String name;
    private final int capacity;
    private final int diagramX;
    private final int diagramY;
    private final Set<MetalCategory> accepts;

    SpikeSlot(String name, int capacity, int diagramX, int diagramY, MetalCategory... accepts) {
        this.name = name;
        this.capacity = capacity;
        this.diagramX = diagramX;
        this.diagramY = diagramY;
        // An empty list means the slot is a wildcard: chest and spine take anything.
        this.accepts = accepts.length == 0 ? EnumSet.allOf(MetalCategory.class) : EnumSet.copyOf(Arrays.asList(accepts));
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int capacity() {
        return this.capacity;
    }

    /** X pixel of this slot on the 88x120 body diagram drawn in the spike jar screen. */
    public int diagramX() {
        return this.diagramX;
    }

    public int diagramY() {
        return this.diagramY;
    }

    public String translationKey() {
        return "cosmere.spike_slot." + this.name;
    }

    /** Whether a spike of this metal can take hold here. */
    public boolean accepts(Metal spikeMetal) {
        if (spikeMetal.isGodMetal()) {
            return this.accepts.contains(MetalCategory.GOD);
        }
        return this.accepts.contains(spikeMetal.hemalurgicCategory());
    }

    public static int totalCapacity() {
        int total = 0;
        for (SpikeSlot slot : values()) {
            total += slot.capacity;
        }
        return total;
    }
}
