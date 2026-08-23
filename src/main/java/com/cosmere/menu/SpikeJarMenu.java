package com.cosmere.menu;

import com.cosmere.hemalurgy.HemalurgyTransfer;
import com.cosmere.hemalurgy.SpikeSlot;
import com.cosmere.item.SpikeItem;
import com.cosmere.item.SpikeJarItem;
import com.cosmere.registry.ModMenus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * The jar screen: nine-by-three of spikes on the left, a diagram of your own body on the right.
 *
 * <p>Picking a spike up onto the cursor and clicking a place on the diagram drives it in.
 * Places that will not take the spike you are holding grey out, which is
 * {@link SpikeSlot#accepts} showing its work.
 */
public class SpikeJarMenu extends AbstractContainerMenu {
    public static final int JAR_ROWS = 3;
    public static final int JAR_COLS = 9;
    private static final int JAR_SLOTS = JAR_ROWS * JAR_COLS;

    private final SpikeJarContainer container;
    private final ItemStack jar;
    private final InteractionHand hand;

    public SpikeJarMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readEnum(InteractionHand.class));
    }

    public SpikeJarMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
        super(ModMenus.SPIKE_JAR.get(), containerId);
        this.hand = hand;
        this.jar = playerInventory.player.getItemInHand(hand);
        this.container = new SpikeJarContainer(this.jar);

        for (int row = 0; row < JAR_ROWS; row++) {
            for (int col = 0; col < JAR_COLS; col++) {
                this.addSlot(new SpikeOnlySlot(this.container, col + row * JAR_COLS, 8 + col * 18, 20 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 106 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 164));
        }
    }

    public ItemStack jar() {
        return this.jar;
    }

    public InteractionHand hand() {
        return this.hand;
    }

    /**
     * Body-diagram clicks arrive here. {@code id} is a {@link SpikeSlot} ordinal; the spike is
     * whatever the player is holding on the cursor.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= SpikeSlot.values().length) {
            return false;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        ItemStack carried = this.getCarried();
        if (!SpikeItem.isSpike(carried)) {
            return false;
        }
        SpikeSlot slot = SpikeSlot.values()[id];
        ItemStack single = carried.copyWithCount(1);
        if (!HemalurgyTransfer.implant(serverPlayer, single, slot)) {
            return false;
        }
        carried.shrink(1);
        this.setCarried(carried);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(this.hand) == this.jar && this.jar.getItem() instanceof SpikeJarItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < JAR_SLOTS) {
            if (!this.moveItemStackTo(stack, JAR_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, JAR_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** Blood keeps a charge; a stray shovel does not. */
    private static class SpikeOnlySlot extends Slot {
        SpikeOnlySlot(SpikeJarContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return SpikeItem.isSpike(stack);
        }
    }
}
