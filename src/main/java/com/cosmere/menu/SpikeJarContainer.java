package com.cosmere.menu;

import java.util.List;

import com.cosmere.item.SpikeItem;
import com.cosmere.item.SpikeJarItem;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A view of a Jar of Spikes as a container.
 *
 * <p>The jar's contents live on the item stack as a data component, so every mutation writes
 * straight back to the held jar. Only spikes go in -- the blood is not good for anything else.
 */
public class SpikeJarContainer implements Container {
    private final ItemStack jar;
    private final NonNullList<ItemStack> items;

    public SpikeJarContainer(ItemStack jar) {
        this.jar = jar;
        this.items = NonNullList.withSize(SpikeJarItem.CAPACITY, ItemStack.EMPTY);
        List<ItemStack> stored = SpikeJarItem.contentsOf(jar);
        for (int i = 0; i < Math.min(stored.size(), this.items.size()); i++) {
            this.items.set(i, stored.get(i));
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(this.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(this.items, slot);
        setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return SpikeItem.isSpike(stack);
    }

    @Override
    public void setChanged() {
        SpikeJarItem.setContents(this.jar, this.items);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == this.jar || player.getOffhandItem() == this.jar;
    }

    @Override
    public void clearContent() {
        this.items.clear();
        setChanged();
    }
}
