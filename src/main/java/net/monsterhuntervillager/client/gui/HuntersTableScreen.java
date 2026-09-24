package net.monsterhuntervillager.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.entity.TrapKind;
import net.monsterhuntervillager.menu.HuntersTableMenu;
import net.monsterhuntervillager.registry.ModEntities;
import net.monsterhuntervillager.registry.ModItems;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class HuntersTableScreen extends AbstractContainerScreen<HuntersTableMenu> {
    private static final ResourceLocation BACKGROUND = MonsterHunterVillager.id("textures/screens/hunters_table_gui.png");
    private static final ResourceLocation PROTOTYPE_HINT = MonsterHunterVillager.id("textures/screens/empty_slot_trap_prototype.png");
    private static final ResourceLocation ARROW = MonsterHunterVillager.id("textures/screens/gui_arrow.png");
    private static final ResourceLocation PLUS = MonsterHunterVillager.id("textures/screens/plus.png");
    private static final ResourceLocation MAP = MonsterHunterVillager.id("textures/screens/map_background.png");
    private static final int LABEL_COLOR = -12829636;

    /**
     * Preview entities, created once per trap type. The original constructed a new entity on
     * every frame the screen was drawn.
     */
    private final Map<EntityType<?>, LivingEntity> previews = new HashMap<>();

    public HuntersTableScreen(HuntersTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        LivingEntity preview = preview();
        if (preview != null) {
            renderPreview(graphics, preview, mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    /** The trap the current inputs make, or the bare trap frame when the addition matches nothing. */
    @Nullable
    private LivingEntity preview() {
        if (!menu.prototype().is(ModItems.TRAP_PROTOTYPE.get()) || minecraft == null || minecraft.level == null) {
            return null;
        }
        ItemStack result = menu.result();
        TrapKind kind = result.isEmpty() ? null : TrapKind.byItem(result.getItem());
        EntityType<?> type = kind != null ? kind.trapType() : ModEntities.EMPTY_TRAP.get();
        return previews.computeIfAbsent(type, t -> (LivingEntity) t.create(minecraft.level));
    }

    /** Draws the trap turning to follow the mouse, framed the same way as the original screen. */
    private void renderPreview(GuiGraphics graphics, LivingEntity entity, int mouseX, int mouseY) {
        float yaw = 2.25F + (float) Math.atan((leftPos + 139 - mouseX) / 40.0);
        float pitch = (float) Math.atan((topPos + 8 - mouseY) / 40.0);
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(pitch * 20.0F * Mth.DEG_TO_RAD);
        pose.mul(camera);

        float bodyRot = entity.yBodyRot;
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();
        float headRotO = entity.yHeadRotO;
        float headRot = entity.yHeadRot;
        entity.yBodyRot = 180.0F + yaw * 20.0F;
        entity.setYRot(180.0F + yaw * 40.0F);
        entity.setXRot(-pitch * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        InventoryScreen.renderEntityInInventory(graphics, leftPos + 139, topPos + 57, 49, new Vector3f(), pose, camera, entity);
        entity.yBodyRot = bodyRot;
        entity.setYRot(yRot);
        entity.setXRot(xRot);
        entity.yHeadRotO = headRotO;
        entity.yHeadRot = headRot;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        graphics.blit(PROTOTYPE_HINT, leftPos + 13, topPos + 56, 0, 0, 16, 16, 16, 16);
        graphics.blit(ARROW, leftPos + 41, topPos + 37, 0, 0, 22, 15, 22, 15);
        graphics.blit(PLUS, leftPos + 13, topPos + 37, 0, 0, 16, 16, 16, 16);
        graphics.blit(MAP, leftPos + 106, topPos + 12, 0, 0, 64, 64, 64, 64);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("gui.monster_hunter_villager.hunters_table_gui.label_hunters_table"), 6, 4, LABEL_COLOR, false);
    }
}
