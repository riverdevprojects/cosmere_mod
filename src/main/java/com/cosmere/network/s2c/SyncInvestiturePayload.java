package com.cosmere.network.s2c;

import com.cosmere.Cosmere;
import com.cosmere.InvestitureData;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The server's copy of a player's Investiture, pushed to that player.
 *
 * <p>The client needs it to decide what the burn window can show, whether an input is allowed,
 * and what the HUD draws. The server never trusts the copy back.
 */
public record SyncInvestiturePayload(InvestitureData data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncInvestiturePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "sync_investiture"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInvestiturePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.fromCodec(InvestitureData.CODEC), SyncInvestiturePayload::data,
                    SyncInvestiturePayload::new);

    public static SyncInvestiturePayload of(InvestitureData data) {
        return new SyncInvestiturePayload(data);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
