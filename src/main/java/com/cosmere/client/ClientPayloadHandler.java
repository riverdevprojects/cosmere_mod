package com.cosmere.client;

import com.cosmere.network.s2c.SeekerPulsePayload;
import com.cosmere.network.s2c.SyncInvestiturePayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Where server-to-client packets land.
 *
 * <p>Everything here writes into {@link ClientInvestitureCache} and nothing else, which keeps
 * the class free of client-only types -- {@code CosmereNetwork} names these methods from common
 * code, and a dedicated server must be able to load that class.
 */
public final class ClientPayloadHandler {
    public static void handleSync(SyncInvestiturePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientInvestitureCache.setLocal(payload.data()));
    }

    public static void handleSeekerPulse(SeekerPulsePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientInvestitureCache.putPulse(
                payload.entityId(), payload.metals(), payload.muffled(), System.currentTimeMillis() / 50L));
    }

    private ClientPayloadHandler() {
    }
}
