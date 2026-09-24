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
public class BaitArrowModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(MonsterHunterVillager.id("modelbait_arrow"), "main");

    private final ModelPart body;

    public BaitArrowModel(ModelPart root) {
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition body = partdefinition.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-8.0F, -2.5F, 0.0F, 16.0F, 5.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0)
                .addBox(-7.0F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(-5, 0)
                .addBox(-8.0F, 0.0F, -2.5F, 16.0F, 0.0F, 5.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 21.5F, 0.0F, 0.7854F, 0.0F, -1.5708F)
        );
        body.addOrReplaceChild(
            "body_r1",
            CubeListBuilder.create().texOffs(11, 0).addBox(-3.5F, -2.5F, -1.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(5.5F, -1.0F, 0.0F, -1.5708F, 0.0F, -0.7854F)
        );
        body.addOrReplaceChild(
            "body_r2",
            CubeListBuilder.create().texOffs(11, 0).addBox(-3.5F, -2.5F, 1.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(5.5F, 1.0F, 0.0F, -1.5708F, 0.0F, 0.7854F)
        );
        body.addOrReplaceChild(
            "body_r3",
            CubeListBuilder.create()
                .texOffs(11, 0)
                .mirror()
                .addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F))
                .mirror(false),
            PartPose.offsetAndRotation(4.5F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F)
        );
        body.addOrReplaceChild(
            "body_r4",
            CubeListBuilder.create().texOffs(11, 0).addBox(-3.5F, -2.5F, 1.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(5.5F, 0.0F, 1.0F, 0.0F, -0.7854F, 0.0F)
        );
        body.addOrReplaceChild(
            "body_r5",
            CubeListBuilder.create().texOffs(11, 0).addBox(-3.5F, -2.5F, -1.0F, 5.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(5.5F, 0.0F, -1.0F, 0.0F, 0.7854F, 0.0F)
        );
        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
