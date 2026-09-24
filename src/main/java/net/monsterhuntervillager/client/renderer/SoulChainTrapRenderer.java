package net.monsterhuntervillager.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.client.model.SoulChainTrapModel;
import net.monsterhuntervillager.entity.SoulChainTrapEntity;

/** The soul chain trap, with its full-bright soul-fire glow layer. */
public class SoulChainTrapRenderer extends TrapRenderer<SoulChainTrapEntity, SoulChainTrapModel<SoulChainTrapEntity>> {
    private static final ResourceLocation TEXTURE = MonsterHunterVillager.id("textures/entities/soul_chain_trap.png");
    private static final ResourceLocation GLOW = MonsterHunterVillager.id("textures/entities/soul_chain_trap_glow.png");
    private static final int FULL_BRIGHT = 0xF00000;

    public SoulChainTrapRenderer(EntityRendererProvider.Context context) {
        super(context, new SoulChainTrapModel<>(context.bakeLayer(SoulChainTrapModel.LAYER_LOCATION)), TEXTURE);
        addLayer(new GlowLayer(this));
    }

    /**
     * The original baked a brand-new copy of the model from its layer definition every frame, for
     * every soul chain trap on screen. The model has no animation, so the parent model is reused.
     */
    private static final class GlowLayer extends RenderLayer<SoulChainTrapEntity, SoulChainTrapModel<SoulChainTrapEntity>> {
        GlowLayer(RenderLayerParent<SoulChainTrapEntity, SoulChainTrapModel<SoulChainTrapEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, SoulChainTrapEntity entity,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(GLOW)), FULL_BRIGHT,
                    LivingEntityRenderer.getOverlayCoords(entity, 0.0F), -1);
        }
    }
}
