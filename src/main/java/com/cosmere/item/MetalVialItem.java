package com.cosmere.item;

import java.util.List;

import com.cosmere.InvestitureData;
import com.cosmere.metal.Metal;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A vial of metal flakes suspended in alcohol.
 *
 * <p>Drinking one puts that metal in your stomach, where Allomancy can reach it. Anyone can
 * swallow a vial; only someone with the matching power gets anything out of it, and everyone
 * else just has metal sitting in their gut. Vials of aluminum are the exception -- swallowing
 * one scours every other metal out.
 */
public class MetalVialItem extends Item {
    private final Metal metal;

    public MetalVialItem(Metal metal, Properties properties) {
        super(properties);
        this.metal = metal;
    }

    public Metal metal() {
        return this.metal;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!level.isClientSide) {
                InvestitureData data = Investiture.of(player);
                if (this.metal == Metal.ALUMINUM) {
                    // Aluminum is the great cleanser: it wipes the stomach and every active burn.
                    for (Metal other : Metal.values()) {
                        data.setReserve(other, 0.0F);
                    }
                    data.stopBurningAll();
                } else {
                    data.addReserve(this.metal, InvestitureData.VIAL_UNITS);
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    Investiture.sync(serverPlayer);
                }
            }
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.6F, 1.4F);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    return new ItemStack(Items.GLASS_BOTTLE);
                }
                player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("cosmere.tooltip.vial", Component.translatable(this.metal.translationKey()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("cosmere.allomancy." + this.metal.id()).withStyle(ChatFormatting.DARK_AQUA));
    }
}
