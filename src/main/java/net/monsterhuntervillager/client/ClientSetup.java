package net.monsterhuntervillager.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.client.gui.HuntersTableScreen;
import net.monsterhuntervillager.client.model.BaitArrowModel;
import net.monsterhuntervillager.client.model.EmptyTrapModel;
import net.monsterhuntervillager.client.model.SharpenedTrapModel;
import net.monsterhuntervillager.client.model.SoulChainTrapModel;
import net.monsterhuntervillager.client.model.StickyTrapModel;
import net.monsterhuntervillager.client.renderer.SoulChainTrapRenderer;
import net.monsterhuntervillager.client.renderer.TrapRenderer;
import net.monsterhuntervillager.registry.ModEntities;
import net.monsterhuntervillager.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-side entry point: renderers, model layers and the workbench screen. */
@Mod(value = MonsterHunterVillager.MODID, dist = Dist.CLIENT)
public final class ClientSetup {
    private static final ResourceLocation STICKY_TEXTURE = MonsterHunterVillager.id("textures/entities/sticky_trap.png");
    // The original drew the sharpened trap with the plain trap texture too; kept as-is.
    private static final ResourceLocation EMPTY_TEXTURE = MonsterHunterVillager.id("textures/entities/empty_trap.png");

    public ClientSetup(IEventBus modBus) {
        modBus.addListener(ClientSetup::registerRenderers);
        modBus.addListener(ClientSetup::registerLayers);
        modBus.addListener(ClientSetup::registerScreens);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.STICKY_TRAP.get(), context ->
                new TrapRenderer<>(context, new StickyTrapModel<>(context.bakeLayer(StickyTrapModel.LAYER_LOCATION)), STICKY_TEXTURE));
        event.registerEntityRenderer(ModEntities.SHARPENED_TRAP.get(), context ->
                new TrapRenderer<>(context, new SharpenedTrapModel<>(context.bakeLayer(SharpenedTrapModel.LAYER_LOCATION)), EMPTY_TEXTURE));
        event.registerEntityRenderer(ModEntities.EMPTY_TRAP.get(), context ->
                new TrapRenderer<>(context, new EmptyTrapModel<>(context.bakeLayer(EmptyTrapModel.LAYER_LOCATION)), EMPTY_TEXTURE));
        event.registerEntityRenderer(ModEntities.SOUL_CHAIN_TRAP.get(), SoulChainTrapRenderer::new);

        event.registerEntityRenderer(ModEntities.STICKY_TRAP_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SHARPENED_TRAP_PROJECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.SOUL_CHAIN_TRAP_PROJECTILE.get(), ThrownItemRenderer::new);
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(StickyTrapModel.LAYER_LOCATION, StickyTrapModel::createBodyLayer);
        event.registerLayerDefinition(SharpenedTrapModel.LAYER_LOCATION, SharpenedTrapModel::createBodyLayer);
        event.registerLayerDefinition(EmptyTrapModel.LAYER_LOCATION, EmptyTrapModel::createBodyLayer);
        event.registerLayerDefinition(SoulChainTrapModel.LAYER_LOCATION, SoulChainTrapModel::createBodyLayer);
        // Registered but unused, as in the original (its texture ships for resource packs).
        event.registerLayerDefinition(BaitArrowModel.LAYER_LOCATION, BaitArrowModel::createBodyLayer);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.HUNTERS_TABLE.get(), HuntersTableScreen::new);
    }
}
