package net.monsterhuntervillager.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;
import net.monsterhuntervillager.MonsterHunterVillager;

/** Static model (no animation). Geometry unchanged from the original mod. */
public class SoulChainTrapModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(MonsterHunterVillager.id("modelsoul_chain_trap"), "main");

    private final ModelPart bone;

    public SoulChainTrapModel(ModelPart root) {
        this.bone = root.getChild("bone");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition bone = partdefinition.addOrReplaceChild(
            "bone",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -0.1F, -4.0F, 8.0F, 0.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0)
                .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 5)
                .addBox(-1.0F, -5.0F, -1.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 5)
                .mirror()
                .addBox(-1.0F, -6.0F, -1.0F, 2.0F, 0.0F, 2.0F, new CubeDeformation(0.0F))
                .mirror(false),
            PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        bone.addOrReplaceChild(
            "cube_r1",
            CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -6.0F, -4.0F, 0.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F)
        );
        bone.addOrReplaceChild(
            "cube_r2",
            CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -6.0F, -4.0F, 0.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.7854F, 0.0F)
        );
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        bone.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
