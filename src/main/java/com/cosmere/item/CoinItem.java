package com.cosmere.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Scadrian money, and the ammunition of choice for a Coinshot.
 *
 * <p>A clip is a copper coin worth very little; a boxing is gold and worth a great deal more.
 * Both are metal, which means both can be Pushed -- flicking a coin into the air and Pushing
 * it is the cheapest lethal attack in the mod, and the reason Coinshots carry pouches.
 *
 * <p>{@link #value()} is what a villager will accept the coin as, in emeralds.
 */
public class CoinItem extends Item {
    private final int value;

    public CoinItem(int value, Properties properties) {
        super(properties);
        this.value = value;
    }

    /** Worth in emeralds when trading. */
    public int value() {
        return this.value;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("cosmere.tooltip.coin_value", this.value).withStyle(ChatFormatting.GREEN));
    }
}
