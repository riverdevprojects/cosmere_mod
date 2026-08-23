package com.cosmere.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * A renderer for the mod's humanoid-ish mobs.
 *
 * <p>Mistwraiths, kandra and koloss are all roughly person-shaped, so they share the vanilla
 * humanoid geometry and differ only by texture and scale. Custom geometry is a later job; a
 * bespoke model per mob would triple the size of the client code for very little play value
 * right now.
 */
public class SimpleHumanoidRenderer<T extends Mob> extends MobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;

    public SimpleHumanoidRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer,
                                  ResourceLocation texture, float shadowRadius) {
        super(context, new HumanoidModel<>(context.bakeLayer(layer)), shadowRadius);
        this.texture = texture;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
