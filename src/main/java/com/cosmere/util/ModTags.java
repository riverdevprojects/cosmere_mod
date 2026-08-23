package com.cosmere.util;

import com.cosmere.Cosmere;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tags that decide what Allomancy can grab.
 *
 * <p>Using tags rather than a hard-coded list means other mods' metal fits into the system by
 * adding themselves to the tag, and pack authors can decide whether, say, netherite counts.
 */
public final class ModTags {
    public static final class Blocks {
        /** Anything an Ironpull or Steelpush can catch hold of. */
        public static final TagKey<Block> ALLOMANTICALLY_MOVABLE = tag("allomantically_movable");
        /** Metal fixtures that never move: the Allomancer moves instead. */
        public static final TagKey<Block> ALLOMANTIC_ANCHOR = tag("allomantic_anchor");

        private static TagKey<Block> tag(String path) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, path));
        }

        private Blocks() {
        }
    }

    public static final class Items {
        /** Items with enough metal in them to Push. Coins, ingots, nuggets, tools. */
        public static final TagKey<Item> ALLOMANTICALLY_MOVABLE = tag("allomantically_movable");
        /** Glass, obsidian, and anything else an Allomancer cannot feel at all. */
        public static final TagKey<Item> ALLOMANTICALLY_INERT = tag("allomantically_inert");

        private static TagKey<Item> tag(String path) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, path));
        }

        private Items() {
        }
    }

    private ModTags() {
    }
}
