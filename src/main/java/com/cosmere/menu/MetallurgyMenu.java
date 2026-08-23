package com.cosmere.menu;

import com.cosmere.crafting.AlloyRecipe;
import com.cosmere.crafting.AlloyRecipes;
import com.cosmere.registry.ModBlocks;
import com.cosmere.registry.ModMenus;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Four crucible slots and one output.
 *
 * <p>Arrangement does not matter -- an alloy is a mixture, not a pattern -- so the menu simply
 * asks {@link AlloyRecipes} whether what is in the crucible adds up to something.
 */
public class MetallurgyMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOTS = 4;
    private static final int RESULT_SLOT = 0;
    private static final int FIRST_INPUT = 1;
    private static final int FIRST_PLAYER_SLOT = FIRST_INPUT + INPUT_SLOTS;

    private final Container inputs = new SimpleContainer(INPUT_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            MetallurgyMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    public MetallurgyMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public MetallurgyMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenus.METALLURGY.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;

        this.addSlot(new AlloyResultSlot(this.result, RESULT_SLOT, 124, 35));
        for (int i = 0; i < INPUT_SLOTS; i++) {
            int x = 30 + (i % 2) * 22;
            int y = 24 + (i / 2) * 22;
            this.addSlot(new Slot(this.inputs, i, x, y));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        AlloyRecipe recipe = AlloyRecipes.find(this.inputs, INPUT_SLOTS);
        this.result.setItem(0, recipe == null ? ItemStack.EMPTY : recipe.result().copy());
        super.slotsChanged(container);
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, ModBlocks.METALLURGY_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.inputs));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int playerStart = FIRST_PLAYER_SLOT;
        int playerEnd = this.slots.size();

        if (index == RESULT_SLOT) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index < playerStart) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, FIRST_INPUT, FIRST_INPUT + INPUT_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    /** Taking the result is what actually spends the ingredients. */
    private class AlloyResultSlot extends Slot {
        AlloyResultSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player taker, ItemStack taken) {
            AlloyRecipe recipe = AlloyRecipes.find(MetallurgyMenu.this.inputs, INPUT_SLOTS);
            if (recipe != null) {
                recipe.consume(MetallurgyMenu.this.inputs, INPUT_SLOTS);
            }
            MetallurgyMenu.this.slotsChanged(MetallurgyMenu.this.inputs);
            super.onTake(taker, taken);
        }
    }

    public Player owner() {
        return this.player;
    }
}
