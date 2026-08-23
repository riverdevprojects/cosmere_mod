package com.cosmere.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Loose blue hide, cut from a koloss.
 *
 * <p>Draped over a body already spiked with four iron spikes through the ribs, on a Hemalurgic
 * Table, it makes a new koloss. The skin never grows to fit; that is the whole horror of them.
 */
public class KolossSkinItem extends Item {
    public KolossSkinItem(Properties properties) {
        super(properties.stacksTo(4));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("cosmere.tooltip.koloss_skin").withStyle(ChatFormatting.DARK_GRAY));
    }
}
