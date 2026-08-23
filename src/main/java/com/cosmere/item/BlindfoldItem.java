package com.cosmere.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;

/**
 * Cloth worn over the eyes.
 *
 * <p>A Tineye burning tin in daylight is blinded by it -- the world is simply too bright. The
 * blindfold cuts that back to something usable while leaving the enhanced hearing, smell and
 * touch intact. Anyone else who puts one on just cannot see.
 *
 * <p>It occupies the head slot but gives no armour, so wearing one is a real trade.
 */
public class BlindfoldItem extends Item implements Equipable {
    public BlindfoldItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return this.swapWithEquipmentSlot(this, level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("cosmere.tooltip.blindfold").withStyle(ChatFormatting.DARK_GRAY));
    }
}
