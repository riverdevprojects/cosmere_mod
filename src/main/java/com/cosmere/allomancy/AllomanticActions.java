package com.cosmere.allomancy;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cosmere.InvestitureData;
import com.cosmere.metal.Metal;
import com.cosmere.network.c2s.AllomanticActionPayload;
import com.cosmere.util.Investiture;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The armed abilities: what happens on the click, and what keeps happening while it is held.
 *
 * <p>Pushes and Pulls are sustained, so the press is recorded here and re-resolved every tick
 * by {@link AllomancyTicker} until the release arrives or the player logs out. Leeching and
 * Nicrobursting fire once and are done.
 */
public final class AllomanticActions {
    /** How far an armed Push or Pull can reach. */
    public static final double REACH = 24.0D;
    /** Leeching and Nicrobursting are touch range: you have to actually be on them. */
    public static final double TOUCH_RANGE = 4.5D;

    /** A press that has not been released yet. */
    public record Held(AllomanticActionPayload.Action action, int entityId, Optional<BlockPos> blockPos) {
    }

    private static final Map<UUID, Held> HELD = new ConcurrentHashMap<>();

    public static void handle(ServerPlayer player, AllomanticActionPayload payload) {
        InvestitureData data = Investiture.of(player);
        if (!data.isPushPullArmed()) {
            HELD.remove(player.getUUID());
            return;
        }

        switch (payload.action()) {
            case PUSH, PULL -> {
                if (payload.pressed()) {
                    HELD.put(player.getUUID(), new Held(payload.action(), payload.entityId(), payload.blockPos()));
                    // A Push with nothing in front of you flicks the metal out of your hand instead.
                    if (payload.action() == AllomanticActionPayload.Action.PUSH
                            && payload.entityId() < 0 && payload.blockPos().isEmpty()) {
                        tryLaunchHeld(player, data);
                    }
                } else {
                    HELD.remove(player.getUUID());
                }
            }
            case LEECH -> {
                if (payload.pressed()) {
                    leech(player, data, payload.entityId());
                }
            }
            case NICROBURST -> {
                if (payload.pressed()) {
                    nicroburst(player, data, payload.entityId());
                }
            }
        }
    }

    /** Resolves one tick of every held Push and Pull. Called from the server tick. */
    public static void tickHeld(ServerPlayer player) {
        Held held = HELD.get(player.getUUID());
        if (held == null) {
            return;
        }
        InvestitureData data = Investiture.of(player);
        boolean pull = held.action() == AllomanticActionPayload.Action.PULL;
        Metal metal = pull ? Metal.IRON : Metal.STEEL;

        if (!data.isPushPullArmed() || !data.isBurning(metal)) {
            HELD.remove(player.getUUID());
            return;
        }

        MetalTarget target = resolve(player, held);
        if (target == null) {
            return;
        }
        AllomanticPhysics.apply(player, target, data.burnStrength(metal), pull);
    }

    public static void forget(UUID playerId) {
        HELD.remove(playerId);
    }

    @Nullable
    private static MetalTarget resolve(ServerPlayer player, Held held) {
        if (held.entityId() >= 0) {
            Entity entity = player.level().getEntity(held.entityId());
            if (entity == null || entity.distanceTo(player) > REACH) {
                return null;
            }
            return MetalTarget.ofEntity(entity, false, MetalScanner.entityMetalWeight(entity));
        }
        BlockPos pos = held.blockPos().orElse(null);
        if (pos == null || !player.blockPosition().closerThan(pos, REACH)) {
            return null;
        }
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        return MetalTarget.ofBlock(pos, true, 100.0F);
    }

    private static void tryLaunchHeld(ServerPlayer player, InvestitureData data) {
        if (!data.isBurning(Metal.STEEL)) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        AllomanticPhysics.launchHeldMetal(player, held, data.burnStrength(Metal.STEEL));
    }

    /**
     * Chromium. Wipes the target's metal reserves entirely -- the single most frightening thing
     * one Allomancer can do to another, and the reason Leechers get killed first.
     */
    private static void leech(ServerPlayer player, InvestitureData data, int entityId) {
        if (!data.isBurning(Metal.CHROMIUM)) {
            return;
        }
        LivingEntity target = touchTarget(player, entityId);
        if (target == null) {
            return;
        }
        InvestitureData victim = Investiture.of(target);
        if (victim.isSpirituallyShielded()) {
            player.displayClientMessage(Component.translatable("cosmere.message.leech_blocked")
                    .withStyle(ChatFormatting.AQUA), true);
            return;
        }
        for (Metal metal : Metal.values()) {
            victim.setReserve(metal, 0.0F);
        }
        victim.stopBurningAll();
        if (target instanceof ServerPlayer victimPlayer) {
            Investiture.sync(victimPlayer);
            victimPlayer.displayClientMessage(Component.translatable("cosmere.message.leeched")
                    .withStyle(ChatFormatting.DARK_RED), true);
        }
        data.consume(Metal.CHROMIUM, 20.0F);
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 1.0F, 1.6F);
        Investiture.sync(player);
    }

    /**
     * Nicrosil. Dumps everything the target is burning in a single instant. Wonderful for an
     * ally, catastrophic for them if they were burning something dangerous.
     */
    private static void nicroburst(ServerPlayer player, InvestitureData data, int entityId) {
        if (!data.isBurning(Metal.NICROSIL)) {
            return;
        }
        LivingEntity target = touchTarget(player, entityId);
        if (target == null) {
            return;
        }
        InvestitureData boosted = Investiture.of(target);
        boosted.setDuraluminFlash(true);
        if (target instanceof ServerPlayer boostedPlayer) {
            Investiture.sync(boostedPlayer);
            boostedPlayer.displayClientMessage(Component.translatable("cosmere.message.nicroburst")
                    .withStyle(ChatFormatting.GOLD), true);
        }
        data.consume(Metal.NICROSIL, 20.0F);
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.0F, 1.2F);
        Investiture.sync(player);
    }

    @Nullable
    private static LivingEntity touchTarget(ServerPlayer player, int entityId) {
        if (entityId < 0) {
            return null;
        }
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof LivingEntity living) || living.distanceTo(player) > TOUCH_RANGE) {
            return null;
        }
        return living;
    }

    private AllomanticActions() {
    }
}
