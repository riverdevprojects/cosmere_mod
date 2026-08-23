package com.cosmere;

import com.cosmere.registry.ModAttachments;
import com.cosmere.registry.ModBlocks;
import com.cosmere.registry.ModCreativeTabs;
import com.cosmere.registry.ModDataComponents;
import com.cosmere.registry.ModEntities;
import com.cosmere.registry.ModItems;
import com.cosmere.registry.ModMenus;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * A mod about the worlds of Brandon Sanderson, starting with Scadrial.
 *
 * <p>Everything here is one planet's worth of content: Allomancy, Feruchemy and Hemalurgy,
 * the mists, and the things that live in them. The structure anticipates more planets --
 * see {@link com.cosmere.dimension.CosmerePlanets} for where a second one plugs in -- but no
 * other dimension is registered yet, and nothing in Scadrial's systems assumes it is alone.
 *
 * <p>Content is registered by walking {@link com.cosmere.metal.Metal} rather than by hand, so
 * a new metal is a single enum entry plus its effect in the three tickers.
 */
@Mod(Cosmere.MODID)
public class Cosmere {
    public static final String MODID = "cosmere";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Cosmere(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        com.cosmere.event.ModBusEvents.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
