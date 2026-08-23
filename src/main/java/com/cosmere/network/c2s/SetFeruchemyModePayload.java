package com.cosmere.network.c2s;

import com.cosmere.Cosmere;
import com.cosmere.feruchemy.FeruchemyMode;
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

/** Sets whether a metalmind is being filled, drained, or left alone. */
public record SetFeruchemyModePayload(Metal metal, FeruchemyMode mode) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SetFeruchemyModePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "set_feruchemy_mode"));

    private static final StreamCodec<ByteBuf, Metal> METAL_CODEC =
            ByteBufCodecs.idMapper(i -> Metal.values()[i], Metal::ordinal);
    private static final StreamCodec<ByteBuf, FeruchemyMode> MODE_CODEC =
            ByteBufCodecs.idMapper(i -> FeruchemyMode.values()[i], FeruchemyMode::ordinal);

    public static final StreamCodec<RegistryFriendlyByteBuf, SetFeruchemyModePayload> STREAM_CODEC =
            StreamCodec.composite(
                    METAL_CODEC, SetFeruchemyModePayload::metal,
                    MODE_CODEC, SetFeruchemyModePayload::mode,
                    SetFeruchemyModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetFeruchemyModePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Investiture.of(player).setFeruchemyMode(payload.metal(), payload.mode());
            Investiture.sync(player);
        }
    }
}
