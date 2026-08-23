package com.cosmere.item;

import java.util.List;

import com.cosmere.InvestitureData;
import com.cosmere.util.Investiture;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A bead of the body of Preservation.
 *
 * <p>Swallow it and you are a Mistborn -- not a Misting, not Twinborn, the whole sixteen and
 * the god metals besides. There is no way to undo it and no second bead.
 */
public class LerasiumBeadItem extends Item {
    public LerasiumBeadItem(Properties properties) {
        super(properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant());
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 40;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            InvestitureData data = Investiture.of(player);
            data.makeMistborn();
            Investiture.sync(player);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.displayClientMessage(Component.translatable("cosmere.message.became_mistborn").withStyle(ChatFormatting.AQUA), false);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("cosmere.tooltip.lerasium").withStyle(ChatFormatting.AQUA));
    }
}
