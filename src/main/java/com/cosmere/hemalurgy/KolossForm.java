package com.cosmere.hemalurgy;

import com.cosmere.Cosmere;
import com.cosmere.registry.ModAttachments;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * What being a koloss does to a player.
 *
 * <p>Applied as transient attribute modifiers rather than baked into the entity, so the whole
 * package goes on and comes off in one place and nothing is left behind if the mechanic ever
 * gains a cure. There is no cure yet.
 */
public final class KolossForm {
    private static final ResourceLocation SCALE_ID = ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "koloss_scale");
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "koloss_health");
    private static final ResourceLocation DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "koloss_damage");
    private static final ResourceLocation STEP_ID = ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "koloss_step");
    private static final ResourceLocation KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "koloss_knockback");
    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "koloss_speed");

    /** Puts the koloss body on. Safe to call repeatedly; modifiers are replaced, not stacked. */
    public static void apply(LivingEntity entity) {
        set(entity, Attributes.SCALE, SCALE_ID, 0.9D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        set(entity, Attributes.MAX_HEALTH, HEALTH_ID, 20.0D, AttributeModifier.Operation.ADD_VALUE);
        set(entity, Attributes.ATTACK_DAMAGE, DAMAGE_ID, 6.0D, AttributeModifier.Operation.ADD_VALUE);
        set(entity, Attributes.STEP_HEIGHT, STEP_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE);
        set(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, 0.6D, AttributeModifier.Operation.ADD_VALUE);
        // Big and heavy: a koloss is not quick on its feet.
        set(entity, Attributes.MOVEMENT_SPEED, SPEED_ID, -0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        entity.setHealth(entity.getMaxHealth());
    }

    public static void remove(LivingEntity entity) {
        clear(entity, Attributes.SCALE, SCALE_ID);
        clear(entity, Attributes.MAX_HEALTH, HEALTH_ID);
        clear(entity, Attributes.ATTACK_DAMAGE, DAMAGE_ID);
        clear(entity, Attributes.STEP_HEIGHT, STEP_ID);
        clear(entity, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID);
        clear(entity, Attributes.MOVEMENT_SPEED, SPEED_ID);
    }

    public static boolean isKoloss(LivingEntity entity) {
        return entity.getData(ModAttachments.KOLOSS_FORM.get());
    }

    /** Re-applies the body after a respawn or a dimension change, where modifiers are lost. */
    public static void refresh(LivingEntity entity) {
        if (isKoloss(entity)) {
            apply(entity);
        }
    }

    private static void set(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id,
                            double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static void clear(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private KolossForm() {
    }
}
