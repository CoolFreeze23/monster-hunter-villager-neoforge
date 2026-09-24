package net.monsterhuntervillager.client.renderer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.monsterhuntervillager.entity.AbstractTrapEntity;

/** Renders a placed trap with a fixed model and texture and no shadow. */
public class TrapRenderer<T extends AbstractTrapEntity, M extends EntityModel<T>> extends MobRenderer<T, M> {
    private final ResourceLocation texture;

    public TrapRenderer(EntityRendererProvider.Context context, M model, ResourceLocation texture) {
        super(context, model, 0.0F);
        this.texture = texture;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
