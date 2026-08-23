package com.cosmere.item;

import java.util.List;
import java.util.Optional;

import com.cosmere.feruchemy.FeruchemyMode;
import com.cosmere.metal.Metal;
import com.cosmere.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A ring, bracer or spike of metal a Feruchemist can pour an attribute into.
 *
 * <p>Form decides capacity: a ring is what you wear every day and holds little, a bracer holds
 * a great deal but is obvious under a sleeve. Which metal decides <em>what</em> it stores --
 * see {@link com.cosmere.feruchemy.FeruchemyEffects}.
 *
 * <p>The item itself is inert. All the behaviour lives in the tick handler, which reads the
 * player's {@link FeruchemyMode} for this metal and moves charge in or out.
 */
public class MetalmindItem extends Item {
    /** Physical size of a metalmind, which is all that separates one from another. */
    public enum Form {
        RING("ring", 600.0F),
        BRACER("bracer", 3600.0F);

        private final String id;
        private final float capacity;

        Form(String id, float capacity) {
            this.id = id;
            this.capacity = capacity;
        }

        public String id() {
            return this.id;
        }

        /** Capacity in charge-seconds of stored attribute. */
        public float capacity() {
            return this.capacity;
        }
    }

    private final Metal metal;
    private final Form form;

    public MetalmindItem(Metal metal, Form form, Properties properties) {
        super(properties.stacksTo(1));
        this.metal = metal;
        this.form = form;
    }

    public Metal metal() {
        return this.metal;
    }

    public Form form() {
        return this.form;
    }

    public float capacity() {
        return MetalmindData.defaultCapacityFor(this.metal, this.form.capacity());
    }

    public MetalmindData dataOf(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.METALMIND.get(), MetalmindData.empty(capacity()));
    }

    public void setData(ItemStack stack, MetalmindData data) {
        stack.set(ModDataComponents.METALMIND.get(), data);
    }

    /** Metalminds show a fill bar rather than a durability bar. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !dataOf(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(dataOf(stack).fillFraction() * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return this.metal.color();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MetalmindData data = dataOf(stack);
        tooltip.add(Component.translatable("cosmere.tooltip.metalmind_charge",
                        String.format("%.0f", data.charge()), String.format("%.0f", data.capacity()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("cosmere.feruchemy." + this.metal.id()).withStyle(ChatFormatting.GOLD));
        Optional<java.util.UUID> owner = data.owner();
        tooltip.add(owner.isEmpty()
                ? Component.translatable("cosmere.tooltip.metalmind_unkeyed").withStyle(ChatFormatting.DARK_PURPLE)
                : Component.translatable("cosmere.tooltip.metalmind_keyed").withStyle(ChatFormatting.DARK_GRAY));
    }
}
