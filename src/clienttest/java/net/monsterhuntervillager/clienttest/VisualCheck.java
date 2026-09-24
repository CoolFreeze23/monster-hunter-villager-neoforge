package net.monsterhuntervillager.clienttest;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.menu.HuntersTableMenu;
import net.monsterhuntervillager.registry.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Dev-client visual check (never shipped). With {@code -Dmhv.clientTest=true} it creates a flat
 * creative world, builds each scene with commands, and saves a screenshot of each to
 * {@code run/clienttest/screenshots}, then quits. Every capture is logged with a {@code MHV_VISUAL} tag.
 */
@EventBusSubscriber(modid = MonsterHunterVillager.MODID, value = Dist.CLIENT)
public final class VisualCheck {
    private static final boolean ENABLED = Boolean.getBoolean("mhv.clientTest");
    private static final Logger LOG = LogUtils.getLogger();
    private static final String MOD = MonsterHunterVillager.MODID + ":";
    private static final String HUNTER_DATA = "VillagerData:{profession:\"" + MOD + "monster_hunter\",level:2,type:\"minecraft:plains\"}";
    private static final BlockPos TABLE = new BlockPos(41, -60, 0);

    /** One scripted step: run {@code action}, wait {@code ticks}, then (optionally) capture {@code shot}. */
    private record Step(Consumer<Minecraft> action, int ticks, String shot) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static int stage;
    private static Step current;
    private static int wait;
    private static String pendingShot;
    private static int shots;

    private VisualCheck() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            stage = 1;
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(8);
            mc.getTutorial().setStep(TutorialSteps.NONE);
            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            mc.createWorldOpenFlows().createFreshLevel("mhv-visual-" + System.currentTimeMillis(),
                    new LevelSettings("MHV visual check", GameType.CREATIVE, false, Difficulty.EASY, true, rules, WorldDataConfiguration.DEFAULT),
                    new WorldOptions(1L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    mc.screen);
            return;
        }
        if (stage == 1 && mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
            stage = 2;
            mc.setScreen(null);
            script();
            wait = 100; // let the spawn area finish meshing
            return;
        }
        if (stage != 2 || pendingShot != null) {
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (current != null) {
            if (current.shot() != null) {
                pendingShot = current.shot();
            }
            current = null;
            return;
        }
        current = STEPS.poll();
        if (current == null) {
            stage = 3;
            LOG.info("MHV_VISUAL: finished, {} screenshots", shots);
            mc.stop();
            return;
        }
        try {
            current.action().accept(mc);
        } catch (RuntimeException error) {
            LOG.error("MHV_VISUAL_FAIL: step before '{}' threw", current.shot(), error);
        }
        wait = current.ticks();
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (pendingShot == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String name = pendingShot;
        pendingShot = null;
        shots++;
        LOG.info("MHV_VISUAL: capture {} screen={} {}", name,
                mc.screen == null ? "none" : mc.screen.getClass().getSimpleName(), describeScene(mc));
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                message -> LOG.info("MHV_VISUAL: {}", message.getString()));
    }

    /** Entities around the camera, so the log records what each screenshot should show. */
    private static String describeScene(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return "";
        }
        var counts = new java.util.TreeMap<String, Integer>();
        for (var entity : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(24))) {
            counts.merge(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath(), 1, Integer::sum);
        }
        return "entities=" + counts;
    }

    // ---- the script --------------------------------------------------------------------------

    private static void step(Consumer<Minecraft> action, int ticks, String shot) {
        STEPS.add(new Step(action, ticks, shot));
    }

    private static void server(Minecraft mc, Consumer<MinecraftServer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> action.accept(server));
    }

    private static void run(Minecraft mc, String... commands) {
        server(mc, server -> {
            for (String command : commands) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
            }
        });
    }

    private static void camera(Minecraft mc, double x, double y, double z, float yaw, float pitch) {
        run(mc, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
    }

    private static void script() {
        // World setup: daylight frozen, nothing spawning, spectator camera with the HUD hidden.
        step(mc -> {
            run(mc, "gamerule doMobSpawning false", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
                    "weather clear", "time set 6000", "gamemode spectator @a");
            mc.options.hideGui = true;
        }, 20, null);

        // 1. The four trap models side by side (sticky, sharpened, soul chain, bare frame).
        step(mc -> {
            run(mc, "summon " + MOD + "sticky_trap 0.5 -60 0.5 {Rotation:[0f,0f]}",
                    "summon " + MOD + "sharpened_trap 2.5 -60 0.5 {Rotation:[0f,0f]}",
                    "summon " + MOD + "soul_chain_trap 4.5 -60 0.5 {Rotation:[0f,0f]}",
                    "summon " + MOD + "empty_trap 6.5 -60 0.5 {Rotation:[0f,0f]}");
            camera(mc, 3.5, -57.2, -5.2, 0, 32);
        }, 40, "01_traps_day");
        step(mc -> run(mc, "time set 18000"), 15, "02_traps_night_soul_glow");
        step(mc -> run(mc, "time set 6000"), 5, null);

        // 2. Monster Hunter villager and its zombie form (under a roof so the zombie doesn't burn).
        step(mc -> {
            run(mc, "fill 19 -57 -1 23 -57 1 minecraft:oak_planks",
                    "summon minecraft:villager 20.5 -60 0.5 {NoAI:1b,Rotation:[180f,0f]," + HUNTER_DATA + "}",
                    "summon minecraft:zombie_villager 22.5 -60 0.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]," + HUNTER_DATA + "}");
            camera(mc, 21.5, -58.6, -3.2, 0, 8);
        }, 40, "03_hunter_and_zombie_hunter");

        // 3. The Hunter's Workbench block from three sides.
        step(mc -> {
            run(mc, "setblock 39 -60 0 " + MOD + "hunters_table[facing=north]",
                    "setblock 41 -60 0 " + MOD + "hunters_table[facing=north]",
                    "setblock 43 -60 0 " + MOD + "hunters_table[facing=east]");
            camera(mc, 41.5, -58.3, -3.0, 20, 25);
        }, 30, "04_workbench_blocks");

        // 4. A hunter tent, placed from the 1.20.1 template.
        step(mc -> server(mc, server -> {
            ServerLevel level = server.overworld();
            var id = MonsterHunterVillager.id("monster_hunter_tent_blue");
            var size = server.getStructureManager().get(id).map(t -> t.getSize()).orElse(new net.minecraft.core.Vec3i(9, 7, 9));
            LOG.info("MHV_VISUAL: tent template size {}", size);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                    "place template " + id + " 60 -61 20");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                    String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f 0 35", 60 + size.getX() / 2.0, -61 + size.getY() + 5.0, 20 - size.getZ() * 0.9));
        }), 60, "05_tent");

        // 5. A soul chain trap dragging a pig: the chain of soul fire and the ring around the victim.
        step(mc -> {
            run(mc, "summon " + MOD + "soul_chain_trap 0.5 -60 30.5 {Rotation:[0f,0f]}",
                    "summon minecraft:pig 4.0 -60 30.5 {PersistenceRequired:1b}");
            camera(mc, 2.2, -58.2, 24.8, 0, 14);
        }, 8, "06_soul_chain_drag_start");
        step(mc -> {
        }, 40, "07_soul_chain_drag_later");
        step(mc -> {
        }, 40, "07b_soul_chain_drag_latest");

        // 6. A thrown trap in flight (drawn as its item).
        step(mc -> {
            run(mc, "summon " + MOD + "projectile_sticky_trap_projectile_projectile 10.5 -58 40.5 {Motion:[0.0,0.45,0.0]}");
            camera(mc, 10.5, -57.0, 37.5, 0, 0);
        }, 4, "08_trap_in_flight");

        // 7. A Monster Hunter (with AI) hunting a husk: a burst of frames to catch throws and strikes.
        step(mc -> {
            run(mc, "summon minecraft:villager 20.5 -60 30.5 {PersistenceRequired:1b," + HUNTER_DATA + "}",
                    "summon minecraft:husk 26.5 -60 30.5 {PersistenceRequired:1b}");
            camera(mc, 23.5, -52.0, 23.0, 0, 42);
        }, 20, "09_hunt_00");
        for (int i = 1; i <= 24; i++) {
            step(mc -> {
            }, 10, String.format("09_hunt_%02d", i));
        }

        // 8. Hotbar item icons and the held trap prototype, HUD on.
        step(mc -> {
            run(mc, "gamemode creative @a", "clear @a", "give @a " + MOD + "trap_prototype 16", "give @a " + MOD + "sticky_trap_item 4",
                    "give @a " + MOD + "sharpened_trap_item 4", "give @a " + MOD + "soul_chain_trap_item 4", "give @a " + MOD + "hunters_knife",
                    "give @a " + MOD + "hunters_table", "give @a minecraft:slime_ball 8", "give @a minecraft:soul_lantern 4");
            camera(mc, 41.5, -60.0, -2.5, 0, 20);
            mc.options.hideGui = false;
        }, 20, "10_hotbar");
        step(mc -> mc.player.getInventory().selected = 4, 10, "11_knife_in_hand");

        // 9. The workbench screen with each recipe, so the preview shows every trap.
        addWorkbenchShot(Items.SLIME_BALL, "12_gui_sticky");
        addWorkbenchShot(Items.SOUL_LANTERN, "13_gui_soul_chain");
        addWorkbenchShot(ModItems.HUNTERS_KNIFE.get(), "14_gui_sharpened");
        addWorkbenchShot(Items.DIRT, "15_gui_no_match");

        // Owner sync: throw a soul chain trap at our own feet and compare owners on both sides.
        step(mc -> {
            mc.player.closeContainer();
            run(mc, "clear @a", "give @a " + MOD + "soul_chain_trap_item 1", "gamemode survival @a");
            camera(mc, 30.5, -60.0, 60.5, 0, 50);
            mc.player.getInventory().selected = 0;
        }, 20, null);
        step(mc -> mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND), 40, "17_thrown_soul_chain");
        step(mc -> {
            for (var trap : mc.level.getEntitiesOfClass(net.monsterhuntervillager.entity.SoulChainTrapEntity.class, mc.player.getBoundingBox().inflate(12))) {
                LOG.info("MHV_VISUAL: client trap {} owner='{}' player={} ownedByPlayer={} playerMotion={}", trap.getId(), trap.ownerId(),
                        mc.player.getStringUUID(), trap.isOwnedBy(mc.player), mc.player.getDeltaMovement());
            }
            server(mc, server -> {
                for (var trap : server.overworld().getEntitiesOfClass(net.monsterhuntervillager.entity.SoulChainTrapEntity.class,
                        server.getPlayerList().getPlayers().get(0).getBoundingBox().inflate(12))) {
                    LOG.info("MHV_VISUAL: server trap {} owner='{}' player={}", trap.getId(), trap.ownerId(),
                            server.getPlayerList().getPlayers().get(0).getStringUUID());
                }
            });
        }, 10, null);

        // 10. The JEI recipe page, if JEI is installed.
        step(mc -> {
            mc.player.closeContainer();
            if (!VisualCheckJei.showWorkbenchRecipes()) {
                LOG.info("MHV_VISUAL: JEI not loaded, skipping recipe page");
            }
        }, 20, "16_jei_recipes");
    }

    private static void addWorkbenchShot(Item addition, String shot) {
        step(mc -> server(mc, server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            ServerLevel level = server.overworld();
            if (!(player.containerMenu instanceof HuntersTableMenu)) {
                player.openMenu(level.getBlockState(TABLE).getMenuProvider(level, TABLE), TABLE);
            }
            if (player.containerMenu instanceof HuntersTableMenu menu) {
                menu.getSlot(HuntersTableMenu.PROTOTYPE_SLOT).set(new ItemStack(ModItems.TRAP_PROTOTYPE.get(), 4));
                menu.getSlot(HuntersTableMenu.ADDITION_SLOT).set(new ItemStack(addition, 2));
                menu.broadcastChanges();
            } else {
                LOG.error("MHV_VISUAL_FAIL: workbench menu did not open");
            }
        }), 15, shot);
    }
}
