package com.cosmere.client.render;

import com.cosmere.Cosmere;
import com.cosmere.entity.KandraEntity;
import com.cosmere.entity.KolossEntity;
import com.cosmere.entity.MistwraithEntity;
import com.cosmere.registry.ModEntities;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Binds each entity type to how it is drawn. */
public final class ModRenderers {
    private static final ResourceLocation MISTWRAITH_TEXTURE = texture("mistwraith");
    private static final ResourceLocation KANDRA_TEXTURE = texture("kandra");
    private static final ResourceLocation KOLOSS_TEXTURE = texture("koloss");

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MISTWRAITH.get(),
                context -> new SimpleHumanoidRenderer<MistwraithEntity>(context, ModelLayers.ZOMBIE, MISTWRAITH_TEXTURE, 0.6F));
        event.registerEntityRenderer(ModEntities.KANDRA.get(),
                context -> new SimpleHumanoidRenderer<KandraEntity>(context, ModelLayers.PLAYER, KANDRA_TEXTURE, 0.5F));
        event.registerEntityRenderer(ModEntities.KOLOSS.get(),
                context -> new SimpleHumanoidRenderer<KolossEntity>(context, ModelLayers.ZOMBIE, KOLOSS_TEXTURE, 0.9F));
        event.registerEntityRenderer(ModEntities.WOLFHOUND.get(), WolfRenderer::new);
        event.registerEntityRenderer(ModEntities.COIN_PROJECTILE.get(), ThrownItemRenderer::new);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Cosmere.MODID, "textures/entity/" + name + ".png");
    }

    private ModRenderers() {
    }
}
