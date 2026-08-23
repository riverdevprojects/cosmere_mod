package com.cosmere.network.c2s;

import java.util.Optional;

import com.cosmere.Cosmere;
import com.cosmere.allomancy.AllomanticActions;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A click, while the F toggle is armed.
 *
 * <p>The client picks the target because it is the side that knows where the crosshair is; the
 * server re-validates range and power before doing anything with it.
 *
 * @param action   which of the four armed abilities was used
 * @param pressed  true on press, false on release -- Pushes are held, not tapped
 * @param entityId target entity, or -1
 * @param blockPos target block, if the crosshair was on one
 */
public record AllomanticActionPayload(Action action, boolean pressed, int entityId, Optional<BlockPos> blockPos)
        implements CustomPacketPayload {

    /** The four things the F toggle arms. */
    public enum Action {
        /** Steel: shove the metal away, or yourself away from it. */
        PUSH,
        /** Iron: haul it in, or yourself towards it. */
        PULL,
        /** Chromium: wipe another Allomancer's metals. */
        LEECH,
        /** Nicrosil: dump everything they are burning at once. */
        NICROBURST
    }

    public static final CustomPacketPayload.Type<AllomanticActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "allomantic_action"));

    private static final StreamCodec<ByteBuf, Action> ACTION_CODEC =
            ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal);

    public static final StreamCodec<RegistryFriendlyByteBuf, AllomanticActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ACTION_CODEC, AllomanticActionPayload::action,
                    ByteBufCodecs.BOOL, AllomanticActionPayload::pressed,
                    ByteBufCodecs.VAR_INT, AllomanticActionPayload::entityId,
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC), AllomanticActionPayload::blockPos,
                    AllomanticActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AllomanticActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            AllomanticActions.handle(player, payload);
        }
    }
}
