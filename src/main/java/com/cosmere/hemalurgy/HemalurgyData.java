package com.cosmere.hemalurgy;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.cosmere.metal.Metal;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Everything Hemalurgy has done to one spiritweb.
 *
 * <p>Mutable on purpose: it lives inside the player's data attachment and is edited in place
 * as spikes go in and come out.
 */
public class HemalurgyData {
    /**
     * Past this many spikes a spiritweb cannot hold together. The victim dies and, because
     * their Connection to their bed is shredded along with everything else, respawns at world
     * spawn. Forty-seven is the count the Lord Ruler's inquisitors were said to top out near.
     */
    public static final int LETHAL_SPIKE_COUNT = 47;

    public static final Codec<HemalurgyData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            PlacedSpike.CODEC.listOf().optionalFieldOf("spikes", List.of()).forGetter(d -> List.copyOf(d.spikes))
    ).apply(inst, HemalurgyData::new));

    private final List<PlacedSpike> spikes = new ArrayList<>();

    public HemalurgyData() {
    }

    public HemalurgyData(List<PlacedSpike> initial) {
        this.spikes.addAll(initial);
    }

    public List<PlacedSpike> spikes() {
        return List.copyOf(this.spikes);
    }

    public int count() {
        return this.spikes.size();
    }

    /** How much Investiture is bleeding off this body; the mists shy away from high values. */
    public float hemalurgicWeight() {
        float weight = 0.0F;
        for (PlacedSpike spike : this.spikes) {
            weight += spike.metal().isGodMetal() ? 3.0F : 1.0F;
        }
        return weight;
    }

    public int usedCapacity(SpikeSlot slot) {
        int used = 0;
        for (PlacedSpike spike : this.spikes) {
            if (spike.slot() == slot) {
                used++;
            }
        }
        return used;
    }

    public boolean hasRoom(SpikeSlot slot) {
        return usedCapacity(slot) < slot.capacity();
    }

    public boolean canAccept(Metal metal, SpikeSlot slot) {
        return slot.accepts(metal) && hasRoom(slot);
    }

    public void add(PlacedSpike spike) {
        this.spikes.add(spike);
    }

    /** Removes the last spike placed in {@code slot}, returning it, or null if the slot is empty. */
    public PlacedSpike removeLast(SpikeSlot slot) {
        for (int i = this.spikes.size() - 1; i >= 0; i--) {
            if (this.spikes.get(i).slot() == slot) {
                return this.spikes.remove(i);
            }
        }
        return null;
    }

    public void clear() {
        this.spikes.clear();
    }

    /** True once the spiritweb can no longer hold the load. */
    public boolean isOverSpiked() {
        return this.spikes.size() > LETHAL_SPIKE_COUNT;
    }

    /** Counts spikes by metal, used by the Koloss and Kandra transformation checks. */
    public Map<Metal, Integer> countsByMetal() {
        Map<Metal, Integer> counts = new EnumMap<>(Metal.class);
        for (PlacedSpike spike : this.spikes) {
            counts.merge(spike.metal(), 1, Integer::sum);
        }
        return counts;
    }

    public int countOf(Metal metal) {
        return countsByMetal().getOrDefault(metal, 0);
    }

    /** True when any spike carries aluminum, which voids the recipient's own powers. */
    public boolean isVoided() {
        return this.spikes.stream().anyMatch(s -> s.metal() == Metal.ALUMINUM);
    }
}
