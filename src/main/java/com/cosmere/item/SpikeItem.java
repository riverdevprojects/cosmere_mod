package com.cosmere.item;

import java.util.List;

import com.cosmere.entity.MistwraithEntity;
import com.cosmere.hemalurgy.StolenAttribute;
import com.cosmere.metal.Metal;
import com.cosmere.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A length of metal sharpened at one end.
 *
 * <p>Blank, it is a poor weapon. Driven through a living heart on a Hemalurgic Table it tears
 * an attribute out of the victim and holds it -- see {@link com.cosmere.hemalurgy.HemalurgyTransfer}.
 * Charged spikes bleed their charge away when carried loose, which is what a Jar of Spikes is for.
 *
 * <p>Right-clicking a mistwraith with a charged spike is how kandra are made.
 */
public class SpikeItem extends Item {
    private final Metal metal;

    public SpikeItem(Metal metal, Properties properties) {
        super(properties);
        this.metal = metal;
    }

    public Metal metal() {
        return this.metal;
    }

    public static SpikeData dataOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.SPIKE.get(), SpikeData.BLANK);
    }

    public static void setData(ItemStack stack, SpikeData data) {
        stack.set(ModDataComponents.SPIKE.get(), data);
    }

    public static boolean isSpike(ItemStack stack) {
        return stack.getItem() instanceof SpikeItem;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof MistwraithEntity mistwraith) {
            return mistwraith.receiveSpike(player, stack, hand);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SpikeData data = dataOf(stack);
        if (data.isCharged()) {
            StolenAttribute charge = data.charge().orElseThrow();
            Component what = charge.metal()
                    .map(m -> (Component) Component.translatable(charge.kind().translationKey() + ".of",
                            Component.translatable(m.translationKey())))
                    .orElseGet(() -> Component.translatable(charge.kind().translationKey()));
            tooltip.add(Component.translatable("cosmere.tooltip.spike_charged", what).withStyle(ChatFormatting.DARK_RED));
        } else {
            tooltip.add(Component.translatable("cosmere.tooltip.spike_blank").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("cosmere.hemalurgy." + this.metal.id()).withStyle(ChatFormatting.RED));
        }
    }
}
