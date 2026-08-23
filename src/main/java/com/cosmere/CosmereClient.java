package com.cosmere;

import com.cosmere.client.ModKeyMappings;
import com.cosmere.client.hud.AllomancyHud;
import com.cosmere.client.hud.SeekerOverlay;
import com.cosmere.client.render.ModRenderers;
import com.cosmere.client.screen.MetallurgyScreen;
import com.cosmere.client.screen.SpikeJarScreen;
import com.cosmere.registry.ModMenus;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only setup: keys, screens, HUD layers and entity renderers.
 *
 * <p>Nothing here decides anything about gameplay. The client sends what the player pressed and
 * draws what the server told it; every rule lives on the other side.
 */
@Mod(value = Cosmere.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Cosmere.MODID, value = Dist.CLIENT)
public class CosmereClient {
    public CosmereClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.TOGGLE_ARMED);
        event.register(ModKeyMappings.BURN_WINDOW);
        event.register(ModKeyMappings.FLARE);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.METALLURGY.get(), MetallurgyScreen::new);
        event.register(ModMenus.SPIKE_JAR.get(), SpikeJarScreen::new);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModRenderers.register(event);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "allomancy"), new AllomancyHud());
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "seeker"), new SeekerOverlay());
    }
}
