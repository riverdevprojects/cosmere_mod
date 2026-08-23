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

/** Caps Lock held: burn everything harder and faster. */
public record SetFlarePayload(boolean flaring) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetFlarePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "set_flare"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFlarePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, SetFlarePayload::flaring, SetFlarePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetFlarePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Investiture.of(player).setFlaring(payload.flaring());
        }
    }
}
