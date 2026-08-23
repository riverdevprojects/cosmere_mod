package com.cosmere.network;

import com.cosmere.Cosmere;
import com.cosmere.network.c2s.AllomanticActionPayload;
import com.cosmere.network.c2s.SetFeruchemyModePayload;
import com.cosmere.network.c2s.SetFlarePayload;
import com.cosmere.network.c2s.SetPushPullArmedPayload;
import com.cosmere.network.c2s.ToggleBurnPayload;
import com.cosmere.network.s2c.SeekerPulsePayload;
import com.cosmere.network.s2c.SyncInvestiturePayload;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Packet registration.
 *
 * <p>Client to server carries intent only -- what the player pressed -- and the server decides
 * whether it means anything. Server to client carries the authoritative Investiture state and
 * the Seeker's readings, both of which the client cannot work out for itself.
 */
@EventBusSubscriber(modid = Cosmere.MODID)
public final class CosmereNetwork {
    public static final String VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToServer(ToggleBurnPayload.TYPE, ToggleBurnPayload.STREAM_CODEC, ToggleBurnPayload::handle);
        registrar.playToServer(SetFlarePayload.TYPE, SetFlarePayload.STREAM_CODEC, SetFlarePayload::handle);
        registrar.playToServer(SetPushPullArmedPayload.TYPE, SetPushPullArmedPayload.STREAM_CODEC, SetPushPullArmedPayload::handle);
        registrar.playToServer(SetFeruchemyModePayload.TYPE, SetFeruchemyModePayload.STREAM_CODEC, SetFeruchemyModePayload::handle);
        registrar.playToServer(AllomanticActionPayload.TYPE, AllomanticActionPayload.STREAM_CODEC, AllomanticActionPayload::handle);

        registrar.playToClient(SyncInvestiturePayload.TYPE, SyncInvestiturePayload.STREAM_CODEC,
                com.cosmere.client.ClientPayloadHandler::handleSync);
        registrar.playToClient(SeekerPulsePayload.TYPE, SeekerPulsePayload.STREAM_CODEC,
                com.cosmere.client.ClientPayloadHandler::handleSeekerPulse);
    }

    private CosmereNetwork() {
    }
}
