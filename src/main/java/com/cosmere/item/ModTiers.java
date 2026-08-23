package com.cosmere.item;

import java.util.function.Supplier;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Tool tiers for the mod's non-vanilla weapon materials.
 *
 * <p>Scadrian weapons are made of what Allomancy cannot touch. Glass and obsidian both cut
 * well and neither can be Pushed or Pulled, which is exactly why an assassin carries them.
 * They pay for that with durability.
 */
public final class ModTiers {
    /** Sharp, brittle, and utterly invisible to a Coinshot. */
    public static final Tier GLASS = new SimpleTier(
            BlockTags.INCORRECT_FOR_STONE_TOOL, 120, 7.0F, 1.0F, 12,
            (Supplier<Ingredient>) () -> Ingredient.of(Blocks.GLASS));

    /** Heavier than glass, and the koloss favourite. */
    public static final Tier OBSIDIAN = new SimpleTier(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 900, 7.5F, 3.0F, 10,
            (Supplier<Ingredient>) () -> Ingredient.of(Blocks.OBSIDIAN));

    /** Alloyed steel, for the weapons a Coinshot is happy to be holding. */
    public static final Tier COSMERE_STEEL = new SimpleTier(
            BlockTags.INCORRECT_FOR_IRON_TOOL, 400, 6.5F, 2.5F, 14,
            (Supplier<Ingredient>) () -> Ingredient.of(Items.IRON_INGOT));

    private ModTiers() {
    }
}
