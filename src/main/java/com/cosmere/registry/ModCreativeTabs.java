package com.cosmere.registry;

import java.util.function.Supplier;

import com.cosmere.Cosmere;
import com.cosmere.metal.Metal;
import com.cosmere.metal.Mineral;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative tabs, split by art so the sixteen-times-everything does not become one wall of
 * grey ingots.
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Cosmere.MODID);

    /** Ores, ingots, nuggets, blocks and the tables. */
    public static final Supplier<CreativeModeTab> MATERIALS = TABS.register("materials", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cosmere.materials"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.INGOTS.get(Metal.STEEL).get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                for (Metal metal : Metal.values()) {
                    accept(output, ModBlocks.METAL_ORES.get(metal));
                    accept(output, ModBlocks.DEEPSLATE_METAL_ORES.get(metal));
                    accept(output, ModItems.RAW_METALS.get(metal));
                    accept(output, ModItems.INGOTS.get(metal));
                    accept(output, ModItems.NUGGETS.get(metal));
                    accept(output, ModBlocks.METAL_BLOCKS.get(metal));
                }
                for (Mineral mineral : Mineral.values()) {
                    accept(output, ModBlocks.MINERAL_ORES.get(mineral));
                    accept(output, ModBlocks.DEEPSLATE_MINERAL_ORES.get(mineral));
                    accept(output, ModItems.RAW_MINERALS.get(mineral));
                    accept(output, ModItems.MINERAL_INGOTS.get(mineral));
                    accept(output, ModItems.MINERAL_NUGGETS.get(mineral));
                    accept(output, ModBlocks.MINERAL_BLOCKS.get(mineral));
                }
                output.accept(ModBlocks.METALLURGY_TABLE.get());
                output.accept(ModBlocks.HEMALURGIC_TABLE.get());
                output.accept(ModItems.CLIP.get());
                output.accept(ModItems.BOXING.get());
            })
            .build());

    /** Vials, metalminds, spikes and the god metals. */
    public static final Supplier<CreativeModeTab> ARTS = TABS.register("arts", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cosmere.arts"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.LERASIUM_BEAD.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                for (Metal metal : Metal.values()) {
                    accept(output, ModItems.VIALS.get(metal));
                }
                for (Metal metal : Metal.values()) {
                    accept(output, ModItems.RINGS.get(metal));
                    accept(output, ModItems.BRACERS.get(metal));
                }
                for (Metal metal : Metal.values()) {
                    accept(output, ModItems.SPIKES.get(metal));
                }
                output.accept(ModItems.SPIKE_JAR.get());
                output.accept(ModItems.LERASIUM_BEAD.get());
                output.accept(ModItems.ATIUM_BEAD.get());
                output.accept(ModItems.KOLOSS_SKIN.get());
            })
            .build());

    /** Weapons, the blindfold, and the mob eggs. */
    public static final Supplier<CreativeModeTab> GEAR = TABS.register("gear", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cosmere.gear"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.OBSIDIAN_AXE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.BLINDFOLD.get());
                output.accept(ModItems.GLASS_DAGGER.get());
                output.accept(ModItems.GLASS_SWORD.get());
                output.accept(ModItems.OBSIDIAN_DAGGER.get());
                output.accept(ModItems.OBSIDIAN_SWORD.get());
                output.accept(ModItems.OBSIDIAN_AXE.get());
                output.accept(ModItems.STEEL_DAGGER.get());
                output.accept(ModItems.DUELING_CANE.get());
                output.accept(ModItems.MISTWRAITH_SPAWN_EGG.get());
                output.accept(ModItems.KANDRA_SPAWN_EGG.get());
                output.accept(ModItems.KOLOSS_SPAWN_EGG.get());
                output.accept(ModItems.WOLFHOUND_SPAWN_EGG.get());
            })
            .build());

    private static void accept(CreativeModeTab.Output output, Supplier<? extends net.minecraft.world.level.ItemLike> supplier) {
        if (supplier != null) {
            output.accept(supplier.get());
        }
    }

    private ModCreativeTabs() {
    }
}
