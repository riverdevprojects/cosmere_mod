package com.cosmere.crafting;

import java.util.ArrayList;
import java.util.List;

import com.cosmere.metal.Metal;
import com.cosmere.metal.Mineral;
import com.cosmere.registry.ModItems;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The alloy table.
 *
 * <p>Ratios follow the books where the books give them and metallurgy where they do not.
 * The recipes are built lazily on first use because they reference registered items.
 */
public final class AlloyRecipes {
    @Nullable
    private static List<AlloyRecipe> recipes;

    public static List<AlloyRecipe> all() {
        if (recipes == null) {
            recipes = build();
        }
        return recipes;
    }

    @Nullable
    public static AlloyRecipe find(Container container, int inputSlots) {
        for (AlloyRecipe recipe : all()) {
            if (recipe.matches(container, inputSlots)) {
                return recipe;
            }
        }
        return null;
    }

    private static List<AlloyRecipe> build() {
        List<AlloyRecipe> list = new ArrayList<>();

        // --- the eight base alloys ---
        list.add(alloy(Metal.STEEL, 3, stack(Items.IRON_INGOT, 3), stack(Items.COAL, 1)));
        list.add(alloy(Metal.PEWTER, 4, metal(Metal.TIN, 3), mineral(Mineral.LEAD, 1)));
        list.add(alloy(Metal.BRASS, 4, stack(Items.COPPER_INGOT, 2), metal(Metal.ZINC, 2)));
        list.add(alloy(Metal.BRONZE, 4, stack(Items.COPPER_INGOT, 3), metal(Metal.TIN, 1)));
        list.add(alloy(Metal.BENDALLOY, 4, mineral(Mineral.BISMUTH, 1), mineral(Mineral.LEAD, 1),
                metal(Metal.TIN, 1), metal(Metal.CADMIUM, 1)));
        list.add(alloy(Metal.ELECTRUM, 4, stack(Items.GOLD_INGOT, 2), mineral(Mineral.SILVER, 2)));
        list.add(alloy(Metal.DURALUMIN, 4, metal(Metal.ALUMINUM, 3), stack(Items.COPPER_INGOT, 1)));
        list.add(alloy(Metal.NICROSIL, 4, mineral(Mineral.NICKEL, 3), metal(Metal.CHROMIUM, 1)));

        // --- god metal alloys ---
        // Malatium is the Eleventh Metal: atium, cut with gold.
        list.add(alloy(Metal.MALATIUM, 2, metal(Metal.ATIUM, 1), stack(Items.GOLD_INGOT, 1)));
        // Harmonium is what you get when the two shards are made to hold together.
        list.add(alloy(Metal.HARMONIUM, 2, metal(Metal.LERASIUM, 1), metal(Metal.ATIUM, 1)));

        return List.copyOf(list);
    }

    private static AlloyRecipe alloy(Metal result, int count, ItemStack... ingredients) {
        return new AlloyRecipe(List.of(ingredients), metal(result, count));
    }

    private static ItemStack metal(Metal metal, int count) {
        return new ItemStack(ModItems.INGOTS.get(metal).get(), count);
    }

    private static ItemStack mineral(Mineral mineral, int count) {
        return new ItemStack(ModItems.MINERAL_INGOTS.get(mineral).get(), count);
    }

    private static ItemStack stack(net.minecraft.world.item.Item item, int count) {
        return new ItemStack(item, count);
    }

    private AlloyRecipes() {
    }
}
