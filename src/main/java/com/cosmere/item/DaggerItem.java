package com.cosmere.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

/**
 * A short blade: fast and weak in a straight fight, brutal from behind.
 *
 * <p>Daggers cost a single stick and a single ingot or glass block, so they are the first
 * real weapon most players make. The backstab bonus is applied in {@code CombatEvents}.
 */
public class DaggerItem extends SwordItem {
    /** Damage multiplier when striking a target that is facing away from you. */
    public static final float BACKSTAB_MULTIPLIER = 2.5F;

    public DaggerItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    /** Daggers swing quickly and hit lighter than a sword of the same material. */
    public static Item.Properties properties(Tier tier) {
        return new Item.Properties().attributes(SwordItem.createAttributes(tier, 1, -1.6F));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("cosmere.tooltip.dagger_backstab").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static boolean isDagger(ItemStack stack) {
        return stack.getItem() instanceof DaggerItem;
    }
}
