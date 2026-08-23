package com.cosmere.network.c2s;

import com.cosmere.Cosmere;
import com.cosmere.util.Investiture;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The F toggle.
 *
 * <p>Arming does not light any metal. It decides whether a click Pushes and Pulls, Leeches and
 * Nicrobursts -- or does the ordinary thing. Chromium and nicrosil do not burn at all unless
 * armed, since there is nothing passive about them.
 */
public record SetPushPullArmedPayload(boolean armed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetPushPullArmedPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "set_push_pull_armed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetPushPullArmedPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, SetPushPullArmedPayload::armed, SetPushPullArmedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetPushPullArmedPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Investiture.of(player).setPushPullArmed(payload.armed());
        }
    }
}
