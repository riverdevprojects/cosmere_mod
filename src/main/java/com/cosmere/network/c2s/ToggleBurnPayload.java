package com.cosmere.network.c2s;

import com.cosmere.Cosmere;
import com.cosmere.InvestitureData;
import com.cosmere.metal.Metal;
import com.cosmere.util.Investiture;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The burn window telling the server to light or snuff one metal.
 *
 * <p>The server re-checks the power and the reserve; a client that asks to burn atium it does
 * not have simply gets nothing.
 */
public record ToggleBurnPayload(Metal metal, boolean burning) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ToggleBurnPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "toggle_burn"));

    private static final StreamCodec<ByteBuf, Metal> METAL_CODEC =
            ByteBufCodecs.idMapper(i -> Metal.values()[i], Metal::ordinal);

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleBurnPayload> STREAM_CODEC =
            StreamCodec.composite(
                    METAL_CODEC, ToggleBurnPayload::metal,
                    ByteBufCodecs.BOOL, ToggleBurnPayload::burning,
                    ToggleBurnPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleBurnPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        InvestitureData data = Investiture.of(player);
        data.setBurning(payload.metal(), payload.burning());
        Investiture.sync(player);
    }
}
