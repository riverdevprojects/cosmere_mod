package com.cosmere.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cosmere.InvestitureData;
import com.cosmere.metal.Metal;

/**
 * The client's copy of what the server says about this player, plus whatever the Seeker heard.
 *
 * <p>Read-only as far as gameplay is concerned: the HUD and input gating consult it, nothing
 * writes gameplay decisions back through it. Deliberately free of any {@code net.minecraft.client}
 * reference so the packet handler that fills it is safe to name from common code.
 */
public final class ClientInvestitureCache {
    /** How long a Seeker's reading stays on screen after the pulse that produced it. */
    public static final int PULSE_TTL_TICKS = 30;

    private static InvestitureData local = new InvestitureData();
    private static final Map<Integer, Pulse> PULSES = new HashMap<>();

    /** One heard Allomancer: what they are burning and when we last heard it. */
    public record Pulse(List<Metal> metals, boolean muffled, long heardAtTick) {
    }

    public static InvestitureData local() {
        return local;
    }

    public static void setLocal(InvestitureData data) {
        local = data;
    }

    public static void putPulse(int entityId, List<Metal> metals, boolean muffled, long tick) {
        PULSES.put(entityId, new Pulse(metals, muffled, tick));
    }

    public static Map<Integer, Pulse> pulses() {
        return PULSES;
    }

    /** Drops readings the Seeker has not refreshed. Called from the client tick. */
    public static void expirePulses(long now) {
        PULSES.entrySet().removeIf(entry -> now - entry.getValue().heardAtTick() > PULSE_TTL_TICKS);
    }

    public static void clear() {
        local = new InvestitureData();
        PULSES.clear();
    }

    private ClientInvestitureCache() {
    }
}
