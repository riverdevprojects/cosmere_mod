package com.cosmere;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cosmere.feruchemy.FeruchemyMode;
import com.cosmere.hemalurgy.HemalurgyData;
import com.cosmere.hemalurgy.PlacedSpike;
import com.cosmere.hemalurgy.StolenAttribute;
import com.cosmere.metal.Metal;
import com.cosmere.metal.MetalCategory;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One player's whole relationship with Investiture: which of the metallic arts they can use,
 * what they have swallowed, what they are burning, and what is spiked into them.
 *
 * <p>Stored as a single data attachment rather than one per art, because the arts read each
 * other constantly -- tapping a nicrosilmind amplifies Allomancy, an aluminum spike voids
 * everything, and Hemalurgy is how most players gain powers in the first place. Keeping it in
 * one object also means one packet to sync it.
 *
 * <p>Mutable and server-authoritative. The client holds a copy refreshed by
 * {@code SyncInvestiturePayload} and uses it only for HUD and input gating.
 */
public class InvestitureData {
    /** Reserve units in a full vial of a single metal. One unit burns for roughly one second. */
    public static final float VIAL_UNITS = 60.0F;
    /** A stomach only holds so much of any one metal. */
    public static final float MAX_RESERVE = 240.0F;
    /** Ticks of continuous burning before a Misting starts down the road to savanthood. */
    public static final int SAVANT_THRESHOLD = 20 * 60 * 20;

    public static final Codec<InvestitureData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Metal.CODEC.listOf().optionalFieldOf("allomantic_powers", List.of()).forGetter(d -> List.copyOf(d.allomanticPowers)),
            Metal.CODEC.listOf().optionalFieldOf("feruchemic_powers", List.of()).forGetter(d -> List.copyOf(d.feruchemicPowers)),
            Codec.unboundedMap(Metal.CODEC, Codec.FLOAT).optionalFieldOf("reserves", Map.of()).forGetter(d -> Map.copyOf(d.reserves)),
            Metal.CODEC.listOf().optionalFieldOf("burning", List.of()).forGetter(d -> List.copyOf(d.burning)),
            Codec.unboundedMap(Metal.CODEC, Codec.INT).optionalFieldOf("savant_ticks", Map.of()).forGetter(d -> Map.copyOf(d.savantTicks)),
            Codec.unboundedMap(Metal.CODEC, FeruchemyMode.CODEC).optionalFieldOf("feruchemy", Map.of()).forGetter(d -> Map.copyOf(d.feruchemyModes)),
            PlacedSpike.CODEC.listOf().optionalFieldOf("spikes", List.of()).forGetter(d -> d.hemalurgy.spikes())
    ).apply(inst, InvestitureData::new));

    private final EnumSet<Metal> allomanticPowers = EnumSet.noneOf(Metal.class);
    private final EnumSet<Metal> feruchemicPowers = EnumSet.noneOf(Metal.class);
    private final EnumMap<Metal, Float> reserves = new EnumMap<>(Metal.class);
    private final EnumSet<Metal> burning = EnumSet.noneOf(Metal.class);
    private final EnumMap<Metal, Integer> savantTicks = new EnumMap<>(Metal.class);
    private final EnumMap<Metal, FeruchemyMode> feruchemyModes = new EnumMap<>(Metal.class);
    private final HemalurgyData hemalurgy = new HemalurgyData();

    /** Flaring is a held key, never saved. */
    private final transient EnumSet<Metal> flaring = EnumSet.noneOf(Metal.class);
    /** The F toggle that arms Steelpushing, Ironpulling, Leeching and Nicrobursting. */
    private transient boolean pushPullArmed;
    /** Set for the tick after duralumin is burned, so effects can read the flashburn. */
    private transient boolean duraluminFlash;
    /**
     * Damage pewter is holding back. A Thug does not stop being hurt while burning pewter --
     * the body simply refuses to notice. Stop burning and the whole bill comes due at once,
     * which is pewter drag. Not saved: logging out counts as the crash.
     */
    private transient float pewterDebt;
    /** Set while storing nicrosil or lerasium: the power is in the metalmind, not in you. */
    private transient boolean investitureSuppressed;
    /** Powers on loan from a metalmind being tapped this tick. Never saved. */
    private final transient EnumSet<Metal> loanedPowers = EnumSet.noneOf(Metal.class);

    public InvestitureData() {
    }

    private InvestitureData(List<Metal> allomantic, List<Metal> feruchemic, Map<Metal, Float> reserves,
                            List<Metal> burning, Map<Metal, Integer> savantTicks,
                            Map<Metal, FeruchemyMode> feruchemyModes, List<PlacedSpike> spikes) {
        this.allomanticPowers.addAll(allomantic);
        this.feruchemicPowers.addAll(feruchemic);
        this.reserves.putAll(reserves);
        this.burning.addAll(burning);
        this.savantTicks.putAll(savantTicks);
        this.feruchemyModes.putAll(feruchemyModes);
        spikes.forEach(this.hemalurgy::add);
    }

    // ---------------------------------------------------------------- powers

    public Set<Metal> allomanticPowers() {
        return EnumSet.copyOf(this.allomanticPowers);
    }

    public Set<Metal> feruchemicPowers() {
        return EnumSet.copyOf(this.feruchemicPowers);
    }

    public HemalurgyData hemalurgy() {
        return this.hemalurgy;
    }

    /**
     * Whether this player can burn {@code metal} at all. Innate powers, powers stolen with
     * Hemalurgy, and the blanket grant a Mistborn has all count -- unless an aluminum spike
     * has hollowed them out.
     */
    public boolean canBurn(Metal metal) {
        if (this.loanedPowers.contains(metal)) {
            return true;
        }
        if (this.hemalurgy.isVoided() || this.investitureSuppressed) {
            return false;
        }
        return this.allomanticPowers.contains(metal) || spikeGrantsAllomancy(metal);
    }

    /** True while a nicrosil- or lerasiummind is drinking this player's Investiture. */
    public boolean isInvestitureSuppressed() {
        return this.investitureSuppressed;
    }

    public void setInvestitureSuppressed(boolean suppressed) {
        this.investitureSuppressed = suppressed;
    }

    /** Grants a power for as long as a metalmind keeps paying for it. Cleared every tick. */
    public void loanPower(Metal metal) {
        this.loanedPowers.add(metal);
    }

    public void clearLoanedPowers() {
        this.loanedPowers.clear();
    }

    public boolean canStore(Metal metal) {
        if (this.hemalurgy.isVoided()) {
            return false;
        }
        return this.feruchemicPowers.contains(metal) || spikeGrantsFeruchemy(metal);
    }

    private boolean spikeGrantsAllomancy(Metal metal) {
        for (PlacedSpike spike : this.hemalurgy.spikes()) {
            StolenAttribute charge = spike.charge().orElse(null);
            if (charge == null) {
                continue;
            }
            if (charge.kind() == StolenAttribute.Kind.EVERYTHING) {
                return true;
            }
            if (charge.kind() == StolenAttribute.Kind.ALLOMANTIC_POWER && charge.metal().orElse(null) == metal) {
                return true;
            }
        }
        return false;
    }

    private boolean spikeGrantsFeruchemy(Metal metal) {
        for (PlacedSpike spike : this.hemalurgy.spikes()) {
            StolenAttribute charge = spike.charge().orElse(null);
            if (charge == null) {
                continue;
            }
            if (charge.kind() == StolenAttribute.Kind.EVERYTHING) {
                return true;
            }
            if (charge.kind() == StolenAttribute.Kind.FERUCHEMIC_POWER && charge.metal().orElse(null) == metal) {
                return true;
            }
        }
        return false;
    }

    /** True when the player can burn every one of the base sixteen. */
    public boolean isMistborn() {
        for (Metal metal : Metal.BASE_SIXTEEN) {
            if (!canBurn(metal)) {
                return false;
            }
        }
        return true;
    }

    /** A Misting burns exactly one metal. Returns null for Mistborn, the mundane, and Twinborn. */
    public Metal mistingMetal() {
        Metal only = null;
        for (Metal metal : Metal.BASE_SIXTEEN) {
            if (canBurn(metal)) {
                if (only != null) {
                    return null;
                }
                only = metal;
            }
        }
        return only;
    }

    public void grantAllomancy(Metal metal) {
        this.allomanticPowers.add(metal);
    }

    public void grantFeruchemy(Metal metal) {
        this.feruchemicPowers.add(metal);
    }

    /** Snaps the player as a full Mistborn. Burning a bead of lerasium does this. */
    public void makeMistborn() {
        this.allomanticPowers.addAll(Metal.BASE_SIXTEEN);
        this.allomanticPowers.add(Metal.ATIUM);
        this.allomanticPowers.add(Metal.MALATIUM);
    }

    /** Makes the player a Full Feruchemist -- the Terris Keeper package. */
    public void makeFullFeruchemist() {
        this.feruchemicPowers.addAll(Metal.BASE_SIXTEEN);
    }

    public void revokeAllPowers() {
        this.allomanticPowers.clear();
        this.feruchemicPowers.clear();
        this.burning.clear();
        this.flaring.clear();
    }

    // -------------------------------------------------------------- reserves

    public float reserve(Metal metal) {
        return this.reserves.getOrDefault(metal, 0.0F);
    }

    public void setReserve(Metal metal, float amount) {
        float clamped = Math.max(0.0F, Math.min(MAX_RESERVE, amount));
        if (clamped <= 0.0F) {
            this.reserves.remove(metal);
        } else {
            this.reserves.put(metal, clamped);
        }
    }

    /** Swallowing a vial. Returns the amount actually ingested. */
    public float addReserve(Metal metal, float amount) {
        float before = reserve(metal);
        setReserve(metal, before + amount);
        return reserve(metal) - before;
    }

    /** Burns reserve away. Returns false and stops the burn when the metal runs out. */
    public boolean consume(Metal metal, float amount) {
        float remaining = reserve(metal) - amount;
        if (remaining <= 0.0F) {
            setReserve(metal, 0.0F);
            this.burning.remove(metal);
            this.flaring.remove(metal);
            return false;
        }
        setReserve(metal, remaining);
        return true;
    }

    public Map<Metal, Float> allReserves() {
        return Map.copyOf(this.reserves);
    }

    // --------------------------------------------------------------- burning

    public Set<Metal> burning() {
        return EnumSet.copyOf(this.burning);
    }

    public boolean isBurning(Metal metal) {
        return this.burning.contains(metal);
    }

    public boolean isFlaring(Metal metal) {
        return this.flaring.contains(metal);
    }

    public void setFlaring(boolean flaring) {
        this.flaring.clear();
        if (flaring) {
            this.flaring.addAll(this.burning);
        }
    }

    /**
     * Turns a metal on or off. Fails silently when the player lacks the power or the metal --
     * a Misting who tries to burn pewter simply does nothing.
     */
    public boolean setBurning(Metal metal, boolean burn) {
        if (!burn) {
            this.flaring.remove(metal);
            return this.burning.remove(metal);
        }
        if (!canBurn(metal) || reserve(metal) <= 0.0F) {
            return false;
        }
        return this.burning.add(metal);
    }

    public boolean toggleBurning(Metal metal) {
        return setBurning(metal, !isBurning(metal));
    }

    public void stopBurningAll() {
        this.burning.clear();
        this.flaring.clear();
    }

    /**
     * How hard a metal is being burned this tick, as a multiplier on its effect. Flaring is a
     * modest boost; duralumin dumps the whole reserve at once for an enormous one.
     */
    public float burnStrength(Metal metal) {
        if (!isBurning(metal)) {
            return 0.0F;
        }
        float strength = 1.0F;
        if (isFlaring(metal)) {
            strength *= 1.75F;
        }
        if (this.duraluminFlash && metal != Metal.DURALUMIN) {
            strength *= 8.0F;
        }
        if (isSavant(metal)) {
            strength *= 1.5F;
        }
        return strength;
    }

    /** Metal burns faster when flared, and duralumin consumes everything in one tick. */
    public float burnRate(Metal metal) {
        float rate = 1.0F / 20.0F;
        if (isFlaring(metal)) {
            rate *= 3.0F;
        }
        return rate;
    }

    /** Damage pewter is currently deferring. The HUD draws this as the second health bar. */
    public float pewterDebt() {
        return this.pewterDebt;
    }

    public void addPewterDebt(float amount) {
        this.pewterDebt = Math.max(0.0F, this.pewterDebt + amount);
    }

    public void clearPewterDebt() {
        this.pewterDebt = 0.0F;
    }

    public boolean isDuraluminFlash() {
        return this.duraluminFlash;
    }

    public void setDuraluminFlash(boolean flash) {
        this.duraluminFlash = flash;
    }

    // ---------------------------------------------------------------- savant

    public int savantTicks(Metal metal) {
        return this.savantTicks.getOrDefault(metal, 0);
    }

    public void addSavantTick(Metal metal) {
        this.savantTicks.merge(metal, 1, Integer::sum);
    }

    /** A savant has burned one metal so long it has warped them; the power is stronger and always partly on. */
    public boolean isSavant(Metal metal) {
        return savantTicks(metal) >= SAVANT_THRESHOLD;
    }

    // ------------------------------------------------------------- feruchemy

    public FeruchemyMode feruchemyMode(Metal metal) {
        return this.feruchemyModes.getOrDefault(metal, FeruchemyMode.OFF);
    }

    public void setFeruchemyMode(Metal metal, FeruchemyMode mode) {
        if (mode == FeruchemyMode.OFF) {
            this.feruchemyModes.remove(metal);
        } else {
            this.feruchemyModes.put(metal, mode);
        }
    }

    public Map<Metal, FeruchemyMode> feruchemyModes() {
        return Map.copyOf(this.feruchemyModes);
    }

    /**
     * Storing aluminum suppresses Identity, which is what lets a Feruchemist fill metalminds
     * anyone can tap.
     */
    public boolean isIdentitySuppressed() {
        return feruchemyMode(Metal.ALUMINUM) == FeruchemyMode.STORING && canStore(Metal.ALUMINUM);
    }

    /**
     * Tapping aluminum armours the spiritweb: no Soothing, no Rioting, and no new spikes.
     * A zinc spike grants the same protection permanently.
     */
    public boolean isSpirituallyShielded() {
        if (feruchemyMode(Metal.ALUMINUM) == FeruchemyMode.TAPPING && canStore(Metal.ALUMINUM)) {
            return true;
        }
        for (PlacedSpike spike : this.hemalurgy.spikes()) {
            StolenAttribute charge = spike.charge().orElse(null);
            if (charge != null && charge.kind() == StolenAttribute.Kind.EMOTIONAL_FORTITUDE) {
                return true;
            }
            if (spike.metal() == Metal.TRELLIUM) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------------------- helpers

    /** All metals in one Allomantic quadrant that this player can burn. */
    public Set<Metal> burnableIn(MetalCategory category) {
        EnumSet<Metal> result = EnumSet.noneOf(Metal.class);
        for (Metal metal : Metal.values()) {
            if (metal.allomanticCategory() == category && canBurn(metal)) {
                result.add(metal);
            }
        }
        return result;
    }

    public boolean isPushPullArmed() {
        return this.pushPullArmed;
    }

    public void setPushPullArmed(boolean armed) {
        this.pushPullArmed = armed;
    }

    /** Deep copy, used when a player dies and the attachment is carried to the new entity. */
    public InvestitureData copy() {
        InvestitureData copy = new InvestitureData();
        copy.allomanticPowers.addAll(this.allomanticPowers);
        copy.feruchemicPowers.addAll(this.feruchemicPowers);
        copy.reserves.putAll(this.reserves);
        copy.savantTicks.putAll(this.savantTicks);
        copy.feruchemyModes.putAll(this.feruchemyModes);
        this.hemalurgy.spikes().forEach(copy.hemalurgy::add);
        return copy;
    }
}
