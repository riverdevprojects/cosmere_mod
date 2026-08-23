package com.cosmere.world;

import java.util.List;

import com.cosmere.Config;
import com.cosmere.InvestitureData;
import com.cosmere.metal.Metal;
import com.cosmere.registry.ModEntities;
import com.cosmere.util.Investiture;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;

/**
 * The mists.
 *
 * <p>They come out after dark, they are thickest where nobody has been spiked, and they are
 * alive in a way nobody on Scadrial likes to think about. Mechanically they are three things:
 * an atmosphere, a spawn condition for mistwraiths, and the only way an ordinary person ever
 * becomes an Allomancer.
 *
 * <p>Hemalurgy repels them. A player carrying spikes walks in a clear bubble, which is a
 * visible tell that something has been done to them.
 */
public final class MistManager {
    /** Radius around a player that the mists are simulated in. */
    public static final double MIST_RADIUS = 24.0D;
    /** Mist is pushed this far back per unit of Hemalurgic weight. */
    public static final double REPULSION_PER_SPIKE = 1.5D;
    /** Chance per eligible tick that a near-death moment in the mists Snaps someone. */
    public static final double SNAP_CHANCE = 0.35D;

    /** True when the mists are out where this player is standing. */
    public static boolean isMisty(ServerLevel level, ServerPlayer player) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        if (level.isDay()) {
            return false;
        }
        return level.canSeeSky(player.blockPosition());
    }

    /** How far the mists stay away from this player, in blocks. Zero for the unspiked. */
    public static double repulsionRadius(InvestitureData data) {
        return Math.min(MIST_RADIUS, data.hemalurgy().hemalurgicWeight() * REPULSION_PER_SPIKE);
    }

    public static void tick(ServerLevel level, ServerPlayer player) {
        if (!Config.MISTS_ENABLED.getAsBoolean() || !isMisty(level, player)) {
            return;
        }
        InvestitureData data = Investiture.of(player);
        double clearance = repulsionRadius(data);
        RandomSource random = level.random;

        // Atmosphere. Sent as server particles so every nearby client sees the same weather.
        for (int i = 0; i < 6; i++) {
            double dx = (random.nextDouble() - 0.5D) * MIST_RADIUS;
            double dz = (random.nextDouble() - 0.5D) * MIST_RADIUS;
            if (Math.sqrt(dx * dx + dz * dz) < clearance) {
                continue;
            }
            double x = player.getX() + dx;
            double z = player.getZ() + dz;
            double y = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    BlockPos.containing(x, player.getY(), z)).getY() + random.nextDouble() * 3.0D;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.4D, 0.1D, 0.4D, 0.0D);
        }

        maybeSpawnMistwraith(level, player, random);
        maybeSnap(level, player, data, random);
    }

    /** Mistwraiths slug their way out of the mists a good way from anyone watching. */
    private static void maybeSpawnMistwraith(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (player.tickCount % 200 != 0 || random.nextFloat() > Config.MISTWRAITH_SPAWN_CHANCE.get().floatValue()) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = 20.0D + random.nextDouble() * 12.0D;
        BlockPos target = BlockPos.containing(
                player.getX() + Math.cos(angle) * distance,
                player.getY(),
                player.getZ() + Math.sin(angle) * distance);
        BlockPos surface = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target);
        if (!level.isLoaded(surface) || !level.getBlockState(surface).isAir()) {
            return;
        }
        List<?> existing = level.getEntities(ModEntities.MISTWRAITH.get(),
                player.getBoundingBox().inflate(48.0D), e -> true);
        if (existing.size() >= Config.MISTWRAITH_CAP.getAsInt()) {
            return;
        }
        var mistwraith = ModEntities.MISTWRAITH.get().spawn(level, surface, MobSpawnType.NATURAL);
        if (mistwraith != null) {
            level.sendParticles(ParticleTypes.CLOUD, surface.getX() + 0.5D, surface.getY() + 1.0D, surface.getZ() + 0.5D,
                    20, 0.6D, 0.6D, 0.6D, 0.02D);
        }
    }

    /**
     * Snapping. Someone who nearly dies out under the mists sometimes comes back with a power
     * they did not have. It is the only way to gain Allomancy without lerasium or a spike.
     */
    private static void maybeSnap(ServerLevel level, ServerPlayer player, InvestitureData data, RandomSource random) {
        if (!Config.SNAPPING_ENABLED.getAsBoolean()) {
            return;
        }
        if (player.getHealth() > 4.0F || !data.allomanticPowers().isEmpty()) {
            return;
        }
        if (random.nextDouble() > SNAP_CHANCE / 20.0D) {
            return;
        }
        Metal snapped = Metal.BASE_SIXTEEN.get(random.nextInt(Metal.BASE_SIXTEEN.size()));
        data.grantAllomancy(snapped);
        Investiture.sync(player);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
                60, 0.5D, 1.0D, 0.5D, 0.1D);
        player.displayClientMessage(Component.translatable("cosmere.message.snapped",
                Component.translatable(snapped.translationKey())).withStyle(ChatFormatting.AQUA), false);
    }

    private MistManager() {
    }
}
