package com.cosmere.crafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * One mixture: a handful of ingredients in any arrangement, and what comes out.
 *
 * <p>Deliberately not a datapack recipe type. Alloy ratios are a fixed part of how the metallic
 * arts work rather than something a pack should be retuning, and keeping them in code means
 * {@link AlloyRecipes} stays readable as a table.
 */
public record AlloyRecipe(List<ItemStack> ingredients, ItemStack result) {
    /**
     * Whether {@code container}'s input slots hold exactly this recipe -- every ingredient
     * present in at least the required amount, and nothing else.
     */
    public boolean matches(Container container, int inputSlots) {
        List<ItemStack> required = new ArrayList<>();
        for (ItemStack ingredient : this.ingredients) {
            required.add(ingredient.copy());
        }

        for (int i = 0; i < inputSlots; i++) {
            ItemStack present = container.getItem(i);
            if (present.isEmpty()) {
                continue;
            }
            int remaining = present.getCount();
            for (ItemStack need : required) {
                if (need.getCount() > 0 && ItemStack.isSameItem(need, present)) {
                    int used = Math.min(need.getCount(), remaining);
                    need.shrink(used);
                    remaining -= used;
                    if (remaining == 0) {
                        break;
                    }
                }
            }
            if (remaining > 0) {
                // Something in the crucible is not part of this alloy.
                return false;
            }
        }

        return required.stream().allMatch(ItemStack::isEmpty);
    }

    /** Removes this recipe's ingredients from the input slots. */
    public void consume(Container container, int inputSlots) {
        for (ItemStack ingredient : this.ingredients) {
            int needed = ingredient.getCount();
            for (int i = 0; i < inputSlots && needed > 0; i++) {
                ItemStack present = container.getItem(i);
                if (!present.isEmpty() && ItemStack.isSameItem(present, ingredient)) {
                    int used = Math.min(needed, present.getCount());
                    present.shrink(used);
                    needed -= used;
                    if (present.isEmpty()) {
                        container.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
        container.setChanged();
    }
}
