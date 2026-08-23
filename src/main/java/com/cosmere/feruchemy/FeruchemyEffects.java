package com.cosmere.feruchemy;

import com.cosmere.InvestitureData;
import com.cosmere.metal.Metal;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodData;

/**
 * What each metalmind costs to fill and pays out to drain.
 *
 * <p>Feruchemy is a trade, never a gift: everything here comes in pairs, and the storing half
 * is always the unpleasant one. Tapping is stronger than storing by design -- an hour of
 * limping buys a few minutes of running -- which is the compression that makes the art worth
 * practising.
 */
public final class FeruchemyEffects {
    /** How long each applied effect lasts before the next tick refreshes it. */
    private static final int REFRESH = 40;

    /** Applies the penalty for filling a metalmind of {@code metal}. */
    public static void applyStoring(ServerPlayer player, InvestitureData data, Metal metal, int amplifier) {
        switch (metal) {
            // --- Physical ---
            case IRON -> {
                // Weight goes into the metalmind; what is left drifts.
                effect(player, MobEffects.SLOW_FALLING, REFRESH, 0);
                effect(player, MobEffects.JUMP, REFRESH, amplifier);
            }
            case STEEL -> effect(player, MobEffects.MOVEMENT_SLOWDOWN, REFRESH, amplifier);
            case TIN -> {
                effect(player, MobEffects.BLINDNESS, REFRESH, 0);
                effect(player, MobEffects.CONFUSION, REFRESH, 0);
            }
            case PEWTER -> effect(player, MobEffects.WEAKNESS, REFRESH, amplifier);

            // --- Mental ---
            case ZINC -> effect(player, MobEffects.DIG_SLOWDOWN, REFRESH, amplifier);
            case BRASS -> {
                // Body warmth going into the metalmind is slow-burn frostbite.
                player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze() + 60, player.getTicksFrozen() + 4));
            }
            case COPPER -> effect(player, MobEffects.CONFUSION, REFRESH, 0);
            case BRONZE -> effect(player, MobEffects.CONFUSION, REFRESH, amplifier);

            // --- Temporal ---
            case CADMIUM -> player.setAirSupply(Math.max(-20, player.getAirSupply() - 8));
            case BENDALLOY -> player.causeFoodExhaustion(0.6F);
            case GOLD -> {
                // Health drains towards three hearts and no further; a goldmind will not kill you.
                if (player.getHealth() > 6.0F) {
                    player.setHealth(Math.max(6.0F, player.getHealth() - 0.2F));
                }
            }
            case ELECTRUM -> {
                effect(player, MobEffects.MOVEMENT_SLOWDOWN, REFRESH, 0);
                effect(player, MobEffects.DIG_SLOWDOWN, REFRESH, 0);
            }

            // --- Spiritual ---
            case CHROMIUM -> effect(player, MobEffects.UNLUCK, REFRESH, amplifier);
            case NICROSIL, LERASIUM, HARMONIUM -> data.setInvestitureSuppressed(true);
            case ALUMINUM -> {
                // Identity is in the metalmind; anything filled now comes out unkeyed.
            }
            case DURALUMIN -> effect(player, MobEffects.BAD_OMEN, REFRESH, 0);

            case ATIUM, MALATIUM, TRELLIUM -> effect(player, MobEffects.MOVEMENT_SLOWDOWN, REFRESH, amplifier);
        }
    }

    /** Applies the benefit for draining a metalmind of {@code metal}. */
    public static void applyTapping(ServerPlayer player, InvestitureData data, Metal metal, int amplifier) {
        switch (metal) {
            // --- Physical ---
            case IRON -> {
                // Many times heavier: nothing shifts you, and you come down hard.
                effect(player, MobEffects.DAMAGE_RESISTANCE, REFRESH, 0);
                player.push(0.0D, -0.08D * (amplifier + 1), 0.0D);
            }
            case STEEL -> effect(player, MobEffects.MOVEMENT_SPEED, REFRESH, amplifier + 1);
            case TIN -> effect(player, MobEffects.NIGHT_VISION, REFRESH + 200, 0);
            case PEWTER -> {
                effect(player, MobEffects.DAMAGE_BOOST, REFRESH, amplifier + 1);
                effect(player, MobEffects.DAMAGE_RESISTANCE, REFRESH, amplifier);
            }

            // --- Mental ---
            case ZINC -> effect(player, MobEffects.DIG_SPEED, REFRESH, amplifier + 1);
            case BRASS -> {
                player.setTicksFrozen(0);
                effect(player, MobEffects.FIRE_RESISTANCE, REFRESH, 0);
            }
            case COPPER -> effect(player, MobEffects.GLOWING, REFRESH, 0);
            case BRONZE -> {
                // Perfect wakefulness: the phantoms have nothing to find.
                player.resetStat(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.TIME_SINCE_REST));
            }

            // --- Temporal ---
            case CADMIUM -> player.setAirSupply(player.getMaxAirSupply());
            case BENDALLOY -> {
                FoodData food = player.getFoodData();
                if (food.getFoodLevel() < 20) {
                    food.eat(1, 0.4F);
                }
            }
            case GOLD -> effect(player, MobEffects.REGENERATION, REFRESH, amplifier);
            case ELECTRUM -> {
                effect(player, MobEffects.DIG_SPEED, REFRESH, amplifier);
                effect(player, MobEffects.DAMAGE_BOOST, REFRESH, 0);
            }

            // --- Spiritual ---
            case CHROMIUM -> effect(player, MobEffects.LUCK, REFRESH, amplifier);
            case NICROSIL -> data.setDuraluminFlash(true);
            case ALUMINUM -> {
                // Identity armoured; see InvestitureData#isSpirituallyShielded.
            }
            case DURALUMIN -> effect(player, MobEffects.HERO_OF_THE_VILLAGE, REFRESH, amplifier);

            // --- God metals ---
            case ATIUM, MALATIUM -> effect(player, MobEffects.MOVEMENT_SPEED, REFRESH, amplifier + 1);
            case LERASIUM, HARMONIUM -> {
                // A lerasiummind hands back the whole of Allomancy for as long as it lasts.
                for (Metal loaned : Metal.BASE_SIXTEEN) {
                    data.loanPower(loaned);
                }
            }
            case TRELLIUM -> effect(player, MobEffects.FIRE_RESISTANCE, REFRESH, 0);
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance existing = player.getEffect(effect);
        if (existing == null || existing.getDuration() < 10 || existing.getAmplifier() < amplifier) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, false));
        }
    }

    private FeruchemyEffects() {
    }
}
