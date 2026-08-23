package com.cosmere.util;

import com.cosmere.InvestitureData;
import com.cosmere.network.s2c.SyncInvestiturePayload;
import com.cosmere.registry.ModAttachments;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** Shorthand for reaching a living entity's {@link InvestitureData} and pushing it to its owner. */
public final class Investiture {
    public static InvestitureData of(LivingEntity entity) {
        return entity.getData(ModAttachments.INVESTITURE);
    }

    /** Server -> owning client. Call after any change the HUD or input gating needs to see. */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, SyncInvestiturePayload.of(of(player)));
    }

    private Investiture() {
    }
}
