package com.cosmere.allomancy;

import java.util.ArrayList;
import java.util.List;

import com.cosmere.InvestitureData;
import com.cosmere.metal.Metal;
import com.cosmere.network.s2c.SeekerPulsePayload;
import com.cosmere.registry.ModItems;
import com.cosmere.util.Investiture;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * One server tick of everything that is currently on fire.
 *
 * <p>Runs per player, per tick: spend reserves, apply what each burning metal does, then let
 * held Pushes resolve. Metals whose effect is purely visual -- gold, electrum, malatium, and
 * the blue lines of iron and steel -- do nothing here; the client draws them from its synced
 * copy of {@link InvestitureData}.
 */
public final class AllomancyTicker {
    /** Radius of a Soothing, a Rioting, or a copper cloud. */
    public static final double EMOTIONAL_RANGE = 24.0D;
    /** How far a Seeker hears. */
    public static final double SEEKER_RANGE = 48.0D;
    /** Radius of a cadmium or bendalloy speed bubble. */
    public static final double BUBBLE_RANGE = 6.0D;

    private static final int EFFECT_REFRESH = 40;

    public static void tick(ServerPlayer player) {
        InvestitureData data = Investiture.of(player);

        boolean duraluminBurning = data.isBurning(Metal.DURALUMIN);
        data.setDuraluminFlash(duraluminBurning);

        // Take a snapshot: effects can stop burns, and we must not mutate while iterating.
        List<Metal> burning = new ArrayList<>(data.burning());
        for (Metal metal : burning) {
            if (!data.isBurning(metal)) {
                continue;
            }
            applyEffect(player, data, metal);
            data.addSavantTick(metal);
            if (!data.consume(metal, data.burnRate(metal))) {
                // Ran dry mid-tick. consume() already snuffed it.
                continue;
            }
        }

        if (duraluminBurning) {
            // Duralumin does not merely amplify -- it spends everything, then goes out itself.
            for (Metal metal : burning) {
                if (metal != Metal.DURALUMIN) {
                    data.setReserve(metal, 0.0F);
                }
            }
            data.setBurning(Metal.DURALUMIN, false);
        }

        applyPewterDrag(player, data);
        AllomanticActions.tickHeld(player);

        if (player.tickCount % 20 == 0) {
            Investiture.sync(player);
        }
    }

    private static void applyEffect(ServerPlayer player, InvestitureData data, Metal metal) {
        float strength = data.burnStrength(metal);
        int amplifier = Math.max(0, Math.round(strength) - 1);
        ServerLevel level = player.serverLevel();

        switch (metal) {
            // Iron and steel do their work through AllomanticActions; the lines are client-side.
            case IRON, STEEL -> {
            }

            case TIN -> {
                // Tin opens the senses all the way up. In daylight that is not a gift.
                effect(player, MobEffects.NIGHT_VISION, EFFECT_REFRESH + 200, 0);
                boolean blindfolded = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BLINDFOLD.get());
                boolean glare = level.isDay() && level.canSeeSky(player.blockPosition());
                if (glare && !blindfolded) {
                    effect(player, MobEffects.BLINDNESS, EFFECT_REFRESH, 0);
                } else {
                    player.removeEffect(MobEffects.BLINDNESS);
                }
                if (data.isSavant(Metal.TIN) && !blindfolded) {
                    // A tin savant never fully closes those senses again.
                    effect(player, MobEffects.CONFUSION, EFFECT_REFRESH, 0);
                }
            }

            case PEWTER -> {
                effect(player, MobEffects.DAMAGE_BOOST, EFFECT_REFRESH, amplifier);
                effect(player, MobEffects.MOVEMENT_SPEED, EFFECT_REFRESH, amplifier);
                effect(player, MobEffects.DAMAGE_RESISTANCE, EFFECT_REFRESH, amplifier);
                effect(player, MobEffects.JUMP, EFFECT_REFRESH, amplifier);
            }

            case BRASS -> soothe(player, level, strength);
            case ZINC -> riot(player, level, strength);

            case COPPER -> {
                // A copper cloud does not look like anything. That is the point.
                if (player.tickCount % 10 == 0) {
                    level.sendParticles(ParticleTypes.MYCELIUM, player.getX(), player.getY() + 1.0D, player.getZ(),
                            3, 1.5D, 1.0D, 1.5D, 0.0D);
                }
            }

            case BRONZE -> seek(player, level);

            case CADMIUM -> {
                // Time drags inside the bubble, the Allomancer worst of all.
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, EFFECT_REFRESH, 3 + amplifier);
                effect(player, MobEffects.DIG_SLOWDOWN, EFFECT_REFRESH, 2 + amplifier);
                forEachInBubble(player, level, other ->
                        other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_REFRESH, 2, true, false, false)));
            }

            case BENDALLOY -> {
                effect(player, MobEffects.MOVEMENT_SPEED, EFFECT_REFRESH, 3 + amplifier);
                effect(player, MobEffects.DIG_SPEED, EFFECT_REFRESH, 2 + amplifier);
                // Everything outside the Allomancer is what actually slows down.
                forEachInBubble(player, level, other ->
                        other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_REFRESH, 1, true, false, false)));
            }

            // Gold, electrum and malatium are all sight. The client renders the shades.
            case GOLD, ELECTRUM, MALATIUM -> {
            }

            case ALUMINUM -> {
                // Aluminum eats itself and everything else with it.
                for (Metal other : Metal.values()) {
                    if (other != Metal.ALUMINUM) {
                        data.setReserve(other, 0.0F);
                        data.setBurning(other, false);
                    }
                }
                data.setBurning(Metal.ALUMINUM, false);
                data.setReserve(Metal.ALUMINUM, 0.0F);
            }

            case DURALUMIN -> {
                // Handled as a flashburn in tick(); nothing to do per-metal.
            }

            // Chromium and nicrosil only ever fire through the F toggle.
            case CHROMIUM, NICROSIL -> {
            }

            case ATIUM -> {
                // Seeing a second ahead is indistinguishable from not being hittable.
                effect(player, MobEffects.DAMAGE_RESISTANCE, EFFECT_REFRESH, 4);
                effect(player, MobEffects.MOVEMENT_SPEED, EFFECT_REFRESH, 1);
            }

            case LERASIUM -> {
                data.makeMistborn();
                data.setBurning(Metal.LERASIUM, false);
                data.setReserve(Metal.LERASIUM, 0.0F);
            }

            case HARMONIUM -> {
                // Harmonium is raw Investiture: it stands in for whatever metal you lack.
                for (Metal other : Metal.BASE_SIXTEEN) {
                    if (data.isBurning(other) && data.reserve(other) <= 0.0F) {
                        data.setReserve(other, 1.0F);
                    }
                }
            }

            case TRELLIUM -> {
                // Trellium shields; the check lives in InvestitureData#isSpirituallyShielded.
                effect(player, MobEffects.FIRE_RESISTANCE, EFFECT_REFRESH, 0);
            }
        }
    }

    /** Brass. Everything hostile nearby loses interest. */
    private static void soothe(ServerPlayer player, ServerLevel level, float strength) {
        double range = EMOTIONAL_RANGE * Math.min(3.0F, strength);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(range))) {
            if (Investiture.of(mob).isSpirituallyShielded()) {
                continue;
            }
            if (mob instanceof Enemy || mob.getTarget() != null) {
                mob.setTarget(null);
                mob.setLastHurtByMob(null);
            }
            if (mob instanceof com.cosmere.entity.KolossEntity koloss) {
                koloss.soothe(player);
            }
        }
    }

    /** Zinc. Everything nearby remembers it was angry about something. */
    private static void riot(ServerPlayer player, ServerLevel level, float strength) {
        double range = EMOTIONAL_RANGE * Math.min(3.0F, strength);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(player.blockPosition()).inflate(range))) {
            if (Investiture.of(mob).isSpirituallyShielded()) {
                continue;
            }
            if (mob instanceof com.cosmere.entity.KolossEntity koloss) {
                koloss.riot();
            }
            if (mob instanceof PathfinderMob && mob.getTarget() == null) {
                LivingEntity victim = level.getNearestPlayer(mob, 16.0D);
                if (victim != null && victim != player) {
                    mob.setTarget(victim);
                }
            }
        }
    }

    /** Bronze. Reports every burning Allomancer in earshot to the Seeker's client. */
    private static void seek(ServerPlayer player, ServerLevel level) {
        if (player.tickCount % 10 != 0) {
            return;
        }
        for (Player other : level.players()) {
            if (other == player || other.distanceTo(player) > SEEKER_RANGE) {
                continue;
            }
            InvestitureData otherData = Investiture.of(other);
            List<Metal> theirBurning = new ArrayList<>(otherData.burning());
            if (theirBurning.isEmpty()) {
                continue;
            }
            // Smokers do not vanish from a Seeker so much as smear.
            boolean muffled = otherData.isBurning(Metal.COPPER);
            PacketDistributor.sendToPlayer(player,
                    new SeekerPulsePayload(other.getId(), muffled ? List.of() : theirBurning, muffled));
        }
    }

    /**
     * The bill for burning pewter. While it burns, damage is deferred; the moment it stops,
     * everything the body was ignoring lands at once.
     */
    private static void applyPewterDrag(ServerPlayer player, InvestitureData data) {
        if (data.isBurning(Metal.PEWTER) || data.pewterDebt() <= 0.0F) {
            return;
        }
        float debt = data.pewterDebt();
        data.clearPewterDebt();
        player.hurt(player.damageSources().magic(), debt);
        effect(player, MobEffects.WEAKNESS, 20 * 15, 1);
        effect(player, MobEffects.MOVEMENT_SLOWDOWN, 20 * 15, 1);
    }

    private static void forEachInBubble(ServerPlayer player, ServerLevel level, java.util.function.Consumer<LivingEntity> action) {
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(BUBBLE_RANGE))) {
            if (other != player) {
                action.accept(other);
            }
        }
    }

    private static void effect(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                               int duration, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing == null || existing.getDuration() < 10 || existing.getAmplifier() < amplifier) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, false));
        }
    }

    private AllomancyTicker() {
    }
}
