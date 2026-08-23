package com.cosmere.item;

import java.util.ArrayList;
import java.util.List;

import com.cosmere.menu.SpikeJarMenu;
import com.cosmere.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A sealed jar of blood holding loose spikes.
 *
 * <p>Blood is what keeps a Hemalurgic charge from bleeding away, so the Ministry kept its
 * spikes in jars and so do you. Opening one shows the spikes it holds beside a diagram of your
 * own body; drag a spike onto a valid place on the diagram to drive it in.
 *
 * <p>Jars turn up in the ruins of Ministry buildings, usually with something unpleasant
 * already in them.
 */
public class SpikeJarItem extends Item {
    public static final int CAPACITY = 27;

    public SpikeJarItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static List<ItemStack> contentsOf(ItemStack jar) {
        return new ArrayList<>(jar.getOrDefault(ModDataComponents.SPIKE_JAR_CONTENTS.get(), List.of()));
    }

    public static void setContents(ItemStack jar, List<ItemStack> contents) {
        List<ItemStack> trimmed = new ArrayList<>();
        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                trimmed.add(stack.copy());
            }
        }
        jar.set(ModDataComponents.SPIKE_JAR_CONTENTS.get(), List.copyOf(trimmed));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack jar = player.getItemInHand(hand);
        if (!level.isClientSide) {
            MenuProvider provider = new SimpleMenuProvider(
                    (id, inventory, owner) -> new SpikeJarMenu(id, inventory, hand),
                    Component.translatable("container.cosmere.spike_jar"));
            player.openMenu(provider, buf -> buf.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(jar, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int count = contentsOf(stack).size();
        tooltip.add(Component.translatable("cosmere.tooltip.spike_jar", count, CAPACITY).withStyle(ChatFormatting.GRAY));
    }
}
