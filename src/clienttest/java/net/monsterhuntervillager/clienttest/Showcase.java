package net.monsterhuntervillager.clienttest;

import com.mojang.logging.LogUtils;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.block.HuntersTableBlock;
import net.monsterhuntervillager.entity.AbstractTrapEntity;
import net.monsterhuntervillager.entity.StickyTrapEntity;
import net.monsterhuntervillager.hunter.MonsterHunterAI;
import net.monsterhuntervillager.registry.ModBlocks;
import net.monsterhuntervillager.registry.ModEntities;
import net.monsterhuntervillager.registry.ModVillagers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Records the README showcase (never shipped). With {@code -Dmhv.showcase=true} it builds a plains
 * world, finds a village and a hunter tent, levels the flattest patch near the village into a small
 * arena, stages every feature, and writes stills and per-tick GIF frames to {@code run/showcase/screenshots}.
 *
 * <p>Arena scenes use a local frame: {@code depth} runs from the arena toward the village (the way
 * the camera looks), {@code side} runs to the camera's right.
 */
@EventBusSubscriber(modid = MonsterHunterVillager.MODID, value = Dist.CLIENT)
public final class Showcase {
    private static final boolean ENABLED = Boolean.getBoolean("mhv.showcase");
    private static final Logger LOG = LogUtils.getLogger();
    private static final long SEED = 20240926L;
    private static final String MOD = MonsterHunterVillager.MODID + ":";
    private static final List<String> TENT_COLOURS = List.of("blue", "brown", "cyan", "gray", "green", "light_gray", "purple", "red", "white");
    private static final int ARENA_HALF = 16;

    private enum Kind { ACT, STILL, RECORD, UNTIL }

    private record Event(int at, Consumer<Minecraft> action) {
    }

    private record Op(Kind kind, Consumer<Minecraft> action, int ticks, String name, BooleanSupplier until, List<Event> events) {
    }

    private static final Deque<Op> OPS = new ArrayDeque<>();
    private static int stage;
    private static int wait;
    private static Op current;
    private static int recordTick;
    private static int pendingAge;
    private static String pendingShot;
    private static int tooltipSlot = -1;

    // Landmarks, found on the server thread.
    private static volatile BlockPos village;
    private static volatile BlockPos arena;
    private static volatile Direction look;
    private static volatile BlockPos tent;
    private static volatile BlockPos tentBench;
    private static volatile Direction tentOpen;

    private Showcase() {
    }

    // ---- driver ------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            stage = 1;
            configureOptions(mc);
            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
            rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_DOINSOMNIA).set(false, null);
            mc.createWorldOpenFlows().createFreshLevel("mhv-showcase-" + System.currentTimeMillis(),
                    new LevelSettings("MHV showcase", GameType.CREATIVE, false, Difficulty.EASY, true, rules, WorldDataConfiguration.DEFAULT),
                    new WorldOptions(SEED, true, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.SINGLE_BIOME_SURFACE).value().createWorldDimensions(),
                    mc.screen);
            return;
        }
        if (stage == 1 && mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
            stage = 2;
            mc.setScreen(null);
            script();
            wait = 60;
            return;
        }
        if (stage != 2) {
            return;
        }
        mc.getToasts().clear();
        if (pendingShot != null) {
            if (++pendingAge > 40) {
                LOG.warn("MHV_SHOWCASE: frame {} never rendered, skipping", pendingShot);
                pendingShot = null;
            }
            return;
        }
        if (current != null && current.kind() == Kind.RECORD) {
            if (recordTick < current.ticks()) {
                for (Event e : current.events()) {
                    if (e.at() == recordTick) {
                        safely(mc, e.action(), current.name() + "@" + recordTick);
                    }
                }
                requestShot(current.name() + "/f_" + String.format(Locale.ROOT, "%04d", recordTick));
                recordTick++;
            } else {
                LOG.info("MHV_SHOWCASE: recorded {} ({} frames)", current.name(), current.ticks());
                current = null;
            }
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (current != null && current.kind() == Kind.UNTIL) {
            if (!current.until().getAsBoolean() && recordTick++ < current.ticks()) {
                return;
            }
            current = null;
            return;
        }
        current = OPS.poll();
        if (current == null) {
            stage = 3;
            LOG.info("MHV_SHOWCASE: finished");
            mc.stop();
            return;
        }
        switch (current.kind()) {
            case ACT -> {
                safely(mc, current.action(), "act, " + OPS.size() + " ops left");
                wait = current.ticks();
                current = null;
            }
            case STILL -> {
                requestShot(current.name());
                current = null;
            }
            case RECORD -> {
                recordTick = 0;
                new File(mc.gameDirectory, "screenshots/" + current.name()).mkdirs();
            }
            case UNTIL -> recordTick = 0;
        }
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Post event) {
        if (!ENABLED || pendingShot == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String name = pendingShot;
        pendingShot = null;
        if (!name.contains("/")) {
            LOG.info("MHV_SHOWCASE: still {}", name);
        }
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> {
        });
    }

    /** Draws a real item tooltip (and the hover highlight) over a hotbar slot for the inventory still. */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!ENABLED || tooltipSlot < 0 || !(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int x = screen.getGuiLeft() + 8 + tooltipSlot * 18;
        int y = screen.getGuiTop() + 142;
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
        graphics.renderTooltip(mc.font, mc.player.getInventory().getItem(tooltipSlot), x + 10, y + 4);
    }

    private static void requestShot(String name) {
        pendingShot = name;
        pendingAge = 0;
    }

    private static void safely(Minecraft mc, Consumer<Minecraft> action, String where) {
        try {
            action.accept(mc);
        } catch (RuntimeException error) {
            LOG.error("MHV_SHOWCASE_FAIL: {}", where, error);
        }
    }

    private static void configureOptions(Minecraft mc) {
        var o = mc.options;
        o.pauseOnLostFocus = false;
        o.renderDistance().set(12);
        o.graphicsMode().set(GraphicsStatus.FANCY);
        o.cloudStatus().set(CloudStatus.FANCY);
        o.ambientOcclusion().set(true);
        o.chatVisibility().set(ChatVisiblity.HIDDEN);
        o.bobView().set(false);
        o.fov().set(70);
        o.hideGui = true;
        mc.getTutorial().setStep(TutorialSteps.NONE);
    }

    // ---- script building blocks --------------------------------------------------------------

    private static void act(Consumer<Minecraft> action, int ticks) {
        OPS.add(new Op(Kind.ACT, action, ticks, null, null, List.of()));
    }

    private static void still(String name) {
        OPS.add(new Op(Kind.STILL, null, 0, name, null, List.of()));
    }

    private static void record(String name, int frames, Event... events) {
        OPS.add(new Op(Kind.RECORD, null, frames, name, null, List.of(events)));
    }

    private static void until(BooleanSupplier condition, int timeout) {
        OPS.add(new Op(Kind.UNTIL, null, timeout, null, condition, List.of()));
    }

    private static Event at(int tick, Consumer<Minecraft> action) {
        return new Event(tick, action);
    }

    private static void server(Minecraft mc, Consumer<ServerLevel> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            try {
                action.accept(server.overworld());
            } catch (RuntimeException error) {
                LOG.error("MHV_SHOWCASE_FAIL: server task", error);
            }
        });
    }

    private static void cmd(ServerLevel level, String command) {
        MinecraftServer server = level.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    private static void cmds(Minecraft mc, String... commands) {
        server(mc, level -> {
            for (String command : commands) {
                cmd(level, command);
            }
        });
    }

    // ---- the arena frame ---------------------------------------------------------------------

    /** World point for a local (side, up, depth) offset from the arena centre. */
    private static Vec3 w(double side, double up, double depth) {
        Direction right = look.getClockWise();
        return new Vec3(
                arena.getX() + 0.5 + depth * look.getStepX() + side * right.getStepX(),
                arena.getY() + up,
                arena.getZ() + 0.5 + depth * look.getStepZ() + side * right.getStepZ());
    }

    private static BlockPos wb(double side, double depth) {
        return BlockPos.containing(w(side, 0, depth));
    }

    private static String p(Vec3 v) {
        return String.format(Locale.ROOT, "%.2f %.2f %.2f", v.x, v.y, v.z);
    }

    /** Yaw that faces the camera (i.e. away from the village). */
    private static float towardCamera() {
        return look.getOpposite().toYRot();
    }

    private static void camera(Minecraft mc, double side, double up, double depth, float yawOffset, float pitch) {
        cmds(mc, String.format(Locale.ROOT, "tp @a %s %.1f %.1f", p(w(side, up, depth)), look.toYRot() + yawOffset, pitch));
    }

    private static void lookAt(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        float yaw = (float) (-Math.atan2(d.x, d.z) * 180 / Math.PI);
        float pitch = (float) (-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI);
        cmd(level, String.format(Locale.ROOT, "tp @a %s %.1f %.1f", p(from), yaw, pitch));
    }

    /** Removes everything but the player from the arena at once (no death animations; traps can't be /killed). */
    private static void clearArena(Minecraft mc) {
        server(mc, level -> {
            AABB box = new AABB(arena).inflate(40);
            for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof Player))) {
                e.discard();
            }
        });
    }

    private static ServerPlayer player(ServerLevel level) {
        return level.getServer().getPlayerList().getPlayers().get(0);
    }

    private static AbstractTrapEntity trap(ServerLevel level, EntityType<? extends AbstractTrapEntity> type, Vec3 at, float yaw, Entity owner) {
        AbstractTrapEntity trap = type.create(level);
        trap.moveTo(at.x, at.y, at.z, yaw, 0);
        trap.setYBodyRot(yaw);
        trap.setYHeadRot(yaw);
        trap.setOwner(owner);
        level.addFreshEntity(trap);
        return trap;
    }

    /** A Monster Hunter posed facing {@code yaw} (body too, which a summoned mob without AI never turns). */
    private static Villager hunter(ServerLevel level, Vec3 at, float yaw, int villagerLevel, boolean ai) {
        Villager villager = EntityType.VILLAGER.create(level);
        villager.moveTo(at.x, at.y, at.z, yaw, 0);
        villager.setYBodyRot(yaw);
        villager.setYHeadRot(yaw);
        villager.yBodyRotO = yaw;
        villager.yHeadRotO = yaw;
        villager.setVillagerData(villager.getVillagerData()
                .setProfession(ModVillagers.MONSTER_HUNTER.get()).setLevel(villagerLevel).setType(VillagerType.PLAINS));
        villager.setNoAi(!ai);
        villager.setPersistenceRequired();
        level.addFreshEntity(villager);
        return villager;
    }

    private static <T extends Entity> T spawn(ServerLevel level, EntityType<T> type, Vec3 at, float yaw, boolean ai) {
        T entity = type.create(level);
        entity.moveTo(at.x, at.y, at.z, yaw, 0);
        entity.setYHeadRot(yaw);
        if (entity instanceof net.minecraft.world.entity.Mob mob) {
            mob.setYBodyRot(yaw);
            mob.setNoAi(!ai);
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(entity);
        return entity;
    }

    private static <T extends Entity> T nearest(ServerLevel level, Class<T> type, Vec3 at) {
        return level.getEntitiesOfClass(type, new AABB(at, at).inflate(12)).stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(at))).orElse(null);
    }

    // ---- the script --------------------------------------------------------------------------

    /** Sections to record; {@code -Dmhv.showcase.only=firstPerson,night} re-records just those (the world setup always runs). */
    private static final java.util.Set<String> ONLY = java.util.Set.of(System.getProperty("mhv.showcase.only", "").split(","));

    private static void section(String name, Runnable script) {
        if (ONLY.contains("") || ONLY.contains(name)) {
            script.run();
        }
    }

    private static void script() {
        setUpWorld();
        section("arenaStills", Showcase::arenaStills);
        section("trapGifs", Showcase::trapGifs);
        section("hunterGifs", Showcase::hunterGifs);
        section("naturalTent", Showcase::naturalTent);
        section("tentRow", Showcase::tentRow);
        section("firstPerson", Showcase::firstPerson);
        section("screens", Showcase::screens);
        section("night", Showcase::night);
        section("fixes", Showcase::fixes);
    }

    private static void setUpWorld() {
        act(mc -> cmds(mc, "gamemode spectator @a", "time set 2000", "weather clear"), 20);
        act(mc -> server(mc, level -> {
            var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level,
                    HolderSet.direct(structures.getHolderOrThrow(BuiltinStructures.VILLAGE_PLAINS)), level.getSharedSpawnPos(), 100, false);
            village = found != null ? found.getFirst() : level.getSharedSpawnPos();
            LOG.info("MHV_SHOWCASE: village at {}", village);
        }), 5);
        until(() -> village != null, 1200);
        act(mc -> cmds(mc, String.format(Locale.ROOT, "tp @a %d 150 %d 0 60", village.getX(), village.getZ())), 200);
        // The flattest tree-free patch 56-88 blocks from the village becomes the arena.
        act(mc -> server(mc, level -> {
            int bestScore = Integer.MAX_VALUE;
            BlockPos best = null;
            for (int r : new int[]{56, 72, 88}) {
                for (int k = 0; k < 12; k++) {
                    double angle = k * Math.PI / 6;
                    int cx = village.getX() + (int) Math.round(Math.cos(angle) * r);
                    int cz = village.getZ() + (int) Math.round(Math.sin(angle) * r);
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            level.getChunk((cx >> 4) + dx, (cz >> 4) + dz);
                        }
                    }
                    List<Integer> heights = new ArrayList<>();
                    int penalty = 0;
                    for (int sx = -ARENA_HALF; sx <= ARENA_HALF; sx += 4) {
                        for (int sz = -ARENA_HALF; sz <= ARENA_HALF; sz += 4) {
                            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + sx, cz + sz);
                            BlockState ground = level.getBlockState(new BlockPos(cx + sx, h - 1, cz + sz));
                            if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.DIRT)) {
                                penalty += 3;
                            }
                            if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx + sx, cz + sz) != h) {
                                penalty += 2;
                            }
                            heights.add(h);
                        }
                    }
                    Collections.sort(heights);
                    int score = (heights.get(heights.size() - 1) - heights.get(0)) * 4 + penalty;
                    if (score < bestScore) {
                        bestScore = score;
                        best = new BlockPos(cx, heights.get(heights.size() / 2), cz);
                    }
                }
            }
            int x = best.getX();
            int y = best.getY();
            int z = best.getZ();
            int h = ARENA_HALF;
            cmd(level, String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:air", x - h, y, z - h, x + h, y + 14, z + h));
            cmd(level, String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:dirt", x - h, y - 4, z - h, x + h, y - 2, z + h));
            cmd(level, String.format(Locale.ROOT, "fill %d %d %d %d %d %d minecraft:grass_block", x - h, y - 1, z - h, x + h, y - 1, z + h));
            cmd(level, String.format(Locale.ROOT, "execute positioned %d %d %d run kill @e[type=minecraft:iron_golem,distance=..200]", x, y, z));
            int dx = village.getX() - x;
            int dz = village.getZ() - z;
            look = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
            arena = best;
            LOG.info("MHV_SHOWCASE: arena at {} (flatness score {}), camera looks {}", best, bestScore, look);
        }), 10);
        until(() -> arena != null, 600);
        act(mc -> camera(mc, 0, 5, -12, 0, 20), 160);
    }

    private static void arenaStills() {
        // The three traps side by side, low angle.
        act(mc -> {
            clearArena(mc);
            server(mc, level -> {
                trap(level, ModEntities.STICKY_TRAP.get(), w(-3, 0, 0), 30, player(level));
                trap(level, ModEntities.SHARPENED_TRAP.get(), w(0, 0, 0), -20, player(level));
                trap(level, ModEntities.SOUL_CHAIN_TRAP.get(), w(3, 0, 0), 0, player(level));
            });
            camera(mc, 0, 1.3, -4.4, 0, 14);
        }, 40);
        still("traps");

        // A Monster Hunter by its workbench, village behind.
        act(mc -> {
            clearArena(mc);
            server(mc, level -> {
                BlockPos bench = wb(1.2, 3);
                level.setBlockAndUpdate(bench, ModBlocks.HUNTERS_TABLE.get().defaultBlockState().setValue(HuntersTableBlock.FACING, look.getOpposite()));
                hunter(level, w(-0.9, 0, 2.3), towardCamera() + 12, 2, false);
            });
            camera(mc, 0.2, 1.62, -2.4, 0, 5);
        }, 40);
        still("hunter_workbench");
        act(mc -> server(mc, level -> level.setBlockAndUpdate(wb(1.2, 3), Blocks.AIR.defaultBlockState())), 5);
    }

    private static void trapGifs() {
        // Sticky: a husk heading for a villager walks into the trap and sticks.
        act(mc -> {
            clearArena(mc);
            server(mc, level -> {
                trap(level, ModEntities.STICKY_TRAP.get(), w(0, 0, 0), 0, player(level));
                Villager bait = spawn(level, EntityType.VILLAGER, w(-6.5, 0, 0), look.getClockWise().toYRot(), false);
                Husk husk = spawn(level, EntityType.HUSK, w(6.5, 0, 0), look.getCounterClockWise().toYRot(), true);
                husk.setTarget(bait);
            });
            camera(mc, 0, 2.0, -6.5, 0, 9);
        }, 12);
        record("gif_sticky", 110);

        // Sharpened: the husk goes for a villager just past the trap and is cut down standing in it.
        act(mc -> {
            clearArena(mc);
            server(mc, level -> {
                trap(level, ModEntities.SHARPENED_TRAP.get(), w(0, 0, 0), 0, player(level));
                Villager bait = spawn(level, EntityType.VILLAGER, w(-2.2, 0, 0), look.getClockWise().toYRot(), false);
                Husk husk = spawn(level, EntityType.HUSK, w(6.5, 0, 0), look.getCounterClockWise().toYRot(), true);
                husk.setHealth(12);
                husk.setTarget(bait);
            });
            camera(mc, 0, 2.0, -6.5, 0, 9);
        }, 12);
        record("gif_sharpened", 170);

        // Soul chain: three animals dragged in on chains of soul fire.
        act(mc -> {
            clearArena(mc);
            server(mc, level -> {
                trap(level, ModEntities.SOUL_CHAIN_TRAP.get(), w(0, 0, 0), 0, player(level));
                spawn(level, EntityType.COW, w(2.6, 0, 3.4), 30, true);
                spawn(level, EntityType.SHEEP, w(-3.3, 0, 2.0), 200, true);
                spawn(level, EntityType.PIG, w(3.6, 0, -1.6), 120, true);
            });
            camera(mc, 0, 3.4, -8.5, 0, 18);
        }, 12);
        record("gif_soul_chain", 110);
    }

    private static void hunterGifs() {
        // The hunt: sticky then sharpened traps thrown at a husk.
        act(mc -> {
            clearArena(mc);
            server(mc, level -> {
                hunter(level, w(-5.5, 0, 0), look.getClockWise().toYRot(), 2, true);
                spawn(level, EntityType.HUSK, w(5.5, 0, 0), look.getCounterClockWise().toYRot(), true);
            });
            camera(mc, 0, 4.5, -9, 0, 20);
        }, 45);
        record("gif_hunt", 220);

        // Knife: a husk goes for the hunter and gets cut.
        act(mc -> {
            clearArena(mc);
            server(mc, level -> {
                Villager hunter = hunter(level, w(-1.1, 0, 0), look.getClockWise().toYRot(), 2, true);
                Husk husk = spawn(level, EntityType.HUSK, w(1.1, 0, 0), look.getCounterClockWise().toYRot(), true);
                husk.setTarget(hunter);
            });
            camera(mc, 0, 1.6, -4.4, 0, 6);
        }, 4);
        record("gif_knife", 100);

        // Dodge: a pillager fires at the hunter; half the arrows are sidestepped with a puff of cloud.
        act(mc -> {
            clearArena(mc);
            server(mc, level -> {
                hunter(level, w(-3.5, 0, 0), look.getClockWise().toYRot(), 2, true);
                Pillager pillager = spawn(level, EntityType.PILLAGER, w(7.5, 0, 0), look.getCounterClockWise().toYRot(), false);
                pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            });
            camera(mc, 1.5, 3.0, -9.5, 0, 12);
        }, 12);
        Consumer<Minecraft> fire = mc -> server(mc, level -> {
            Pillager pillager = nearest(level, Pillager.class, w(7.5, 0, 0));
            Villager hunter = level.getEntitiesOfClass(Villager.class, new AABB(arena).inflate(20), MonsterHunterAI::isHunter).stream()
                    .findFirst().orElse(null);
            if (pillager == null || hunter == null) {
                return;
            }
            Arrow arrow = new Arrow(level, pillager, new ItemStack(Items.ARROW), null);
            arrow.setPos(pillager.getX(), pillager.getEyeY() - 0.1, pillager.getZ());
            double dx = hunter.getX() - arrow.getX();
            double dz = hunter.getZ() - arrow.getZ();
            double dy = hunter.getY(0.5) - arrow.getY();
            arrow.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.12, dz, 1.8F, 0.0F);
            level.addFreshEntity(arrow);
            pillager.swing(InteractionHand.MAIN_HAND);
        });
        record("gif_dodge", 170, at(15, fire), at(45, fire), at(75, fire), at(105, fire), at(135, fire));
        act(mc -> clearArena(mc), 10);
    }

    private static void naturalTent() {
        act(mc -> server(mc, level -> {
            var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            List<Holder<Structure>> holders = new ArrayList<>();
            for (String colour : TENT_COLOURS) {
                structures.getHolder(ResourceKey.create(Registries.STRUCTURE, MonsterHunterVillager.id("hunters_tent_" + colour))).ifPresent(holders::add);
            }
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holders), arena, 100, false);
            tent = found != null ? found.getFirst() : arena.offset(0, 0, 80);
            LOG.info("MHV_SHOWCASE: tent near {} ({})", tent, found != null ? found.getSecond().unwrapKey().orElse(null) : "none");
        }), 5);
        until(() -> tent != null, 1200);
        act(mc -> cmds(mc, String.format(Locale.ROOT, "tp @a %d 150 %d 0 70", tent.getX() + 8, tent.getZ() + 8)), 200);
        act(mc -> server(mc, level -> {
            BlockPos bench = null;
            for (int x = -16; x <= 24 && bench == null; x++) {
                for (int z = -16; z <= 24 && bench == null; z++) {
                    int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tent.getX() + x, tent.getZ() + z);
                    for (int y = top - 12; y <= top + 2; y++) {
                        BlockPos candidate = new BlockPos(tent.getX() + x, y, tent.getZ() + z);
                        if (level.getBlockState(candidate).is(ModBlocks.HUNTERS_TABLE.get())) {
                            bench = candidate;
                            break;
                        }
                    }
                }
            }
            if (bench == null) {
                LOG.warn("MHV_SHOWCASE: no workbench found near the tent");
                bench = tent.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tent.getX(), tent.getZ()));
            }
            tentOpen = openSide(level, bench);
            tentBench = bench;
            LOG.info("MHV_SHOWCASE: tent workbench at {}, open to the {}", bench, tentOpen);
        }), 10);
        until(() -> tentBench != null, 400);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            act(mc -> server(mc, level -> {
                Vec3 target = Vec3.atCenterOf(tentBench).add(0, 1, 0);
                lookAt(level, target.add(d.getStepX() * 10.0, 4.5, d.getStepZ() * 10.0), target);
            }), 60);
            still("tent_" + d.getName());
        }
        // Hero: a Monster Hunter at the tent's open side with its traps set in front of it.
        act(mc -> server(mc, level -> {
            Direction d = tentOpen;
            BlockPos stand = tentBench.relative(d, 4);
            stand = stand.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, stand.getX(), stand.getZ()));
            Villager hunter = hunter(level, Vec3.atBottomCenterOf(stand), d.toYRot(), 2, false);
            for (int side : new int[]{-1, 1}) {
                BlockPos at = stand.relative(d, 2).relative(side < 0 ? d.getCounterClockWise() : d.getClockWise(), 2);
                at = at.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()));
                trap(level, side < 0 ? ModEntities.STICKY_TRAP.get() : ModEntities.SHARPENED_TRAP.get(), Vec3.atBottomCenterOf(at), d.toYRot() + 25 * side, hunter);
            }
            Vec3 eye = Vec3.atBottomCenterOf(stand).add(d.getStepX() * 7.5 + d.getClockWise().getStepX() * 2.0, 2.3,
                    d.getStepZ() * 7.5 + d.getClockWise().getStepZ() * 2.0);
            lookAt(level, eye, Vec3.atBottomCenterOf(stand).add(-d.getStepX() * 2.0, 1.5, -d.getStepZ() * 2.0));
        }), 60);
        still("hero");
    }

    /** The side of a tent the workbench is open to: the direction with the fewest blocks in the way. */
    private static Direction openSide(ServerLevel level, BlockPos bench) {
        Direction open = Direction.SOUTH;
        int fewest = Integer.MAX_VALUE;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            int solid = 0;
            for (int k = 1; k <= 6; k++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (!level.getBlockState(bench.relative(d, k).above(dy)).isAir()) {
                        solid++;
                    }
                }
            }
            if (solid < fewest) {
                fewest = solid;
                open = d;
            }
        }
        return open;
    }

    /** Three tent colours side by side on the arena, turned so their entrances face the camera. */
    private static void tentRow() {
        act(mc -> {
            clearArena(mc);
            server(mc, level -> {
                String[] colours = {"red", "green", "cyan"};
                double[] sides = {-10.5, 0, 10.5};
                for (int i = 0; i < colours.length; i++) {
                    StructureTemplate template = level.getStructureManager().get(MonsterHunterVillager.id("monster_hunter_tent_" + colours[i])).orElseThrow();
                    Vec3i size = template.getSize();
                    BlockPos centre = wb(sides[i], 7).below();
                    BlockPos origin = centre.offset(-size.getX() / 2, 0, -size.getZ() / 2);
                    BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
                    // Place once unrotated to find which way this template opens, then re-place it facing the camera.
                    StructurePlaceSettings plain = new StructurePlaceSettings().setRotationPivot(pivot);
                    template.placeInWorld(level, origin, origin, plain, level.getRandom(), Block.UPDATE_CLIENTS);
                    BoundingBox box = template.getBoundingBox(plain, origin);
                    BlockPos bench = BlockPos.betweenClosedStream(box).filter(q -> level.getBlockState(q).is(ModBlocks.HUNTERS_TABLE.get()))
                            .findFirst().map(BlockPos::immutable).orElse(null);
                    Direction opens = bench != null ? openSide(level, bench) : Direction.SOUTH;
                    Rotation turn = Rotation.NONE;
                    for (Rotation r : Rotation.values()) {
                        if (r.rotate(opens) == look.getOpposite()) {
                            turn = r;
                        }
                    }
                    wipe(level, box, centre.getY());
                    StructurePlaceSettings turned = new StructurePlaceSettings().setRotation(turn).setRotationPivot(pivot);
                    template.placeInWorld(level, origin, origin, turned, level.getRandom(), Block.UPDATE_CLIENTS);
                    LOG.info("MHV_SHOWCASE: {} tent opens {} unrotated, turned {}", colours[i], opens, turn);
                }
            });
            camera(mc, 0, 9, -13, 0, 24);
        }, 80);
        still("tents");
        act(mc -> {
            server(mc, level -> wipe(level, BoundingBox.fromCorners(wb(-16, -4).below(), wb(16, 14).above(10)), arena.getY() - 1));
            clearArena(mc);
        }, 20);
    }

    /** Clears a box back to the arena's grass floor. */
    private static void wipe(ServerLevel level, BoundingBox box, int floorY) {
        for (BlockPos q : BlockPos.betweenClosed(box.minX(), floorY + 1, box.minZ(), box.maxX(), box.maxY() + 2, box.maxZ())) {
            level.setBlock(q, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos q : BlockPos.betweenClosed(box.minX(), floorY, box.minZ(), box.maxX(), floorY, box.maxZ())) {
            level.setBlock(q, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (Entity e : level.getEntities((Entity) null, AABB.of(box).inflate(2), e -> !(e instanceof Player))) {
            e.discard();
        }
    }

    private static void firstPerson() {
        // Throwing each trap, first person, HUD on; the camera turns between throws so they land apart.
        act(mc -> {
            clearArena(mc);
            cmds(mc, "gamemode survival @a", "clear @a",
                    "give @a " + MOD + "sticky_trap_item 4", "give @a " + MOD + "sharpened_trap_item 4", "give @a " + MOD + "soul_chain_trap_item 4",
                    "effect give @a minecraft:saturation infinite 1 true");
            camera(mc, 0, 0, -6.5, 0, 30);
            mc.options.hideGui = false;
        }, 30);
        act(mc -> mc.player.getInventory().selected = 0, 10);
        Consumer<Minecraft> use = mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        List<Event> throwing = new ArrayList<>();
        sweep(throwing, 0, 8, 0, -28);
        throwing.add(at(14, use));
        throwing.add(at(40, mc -> mc.player.getInventory().selected = 1));
        sweep(throwing, 40, 8, -28, 0);
        throwing.add(at(54, use));
        throwing.add(at(84, mc -> mc.player.getInventory().selected = 2));
        sweep(throwing, 84, 8, 0, 28);
        throwing.add(at(98, use));
        record("gif_throw", 150, throwing.toArray(Event[]::new));

        // Walking up to the sticky trap and picking it back up.
        act(mc -> {
            StickyTrapEntity trap = mc.level.getEntitiesOfClass(StickyTrapEntity.class, mc.player.getBoundingBox().inflate(14)).stream()
                    .min(Comparator.comparingDouble(t -> t.distanceToSqr(mc.player))).orElse(null);
            if (trap != null) {
                Vec3 stand = trap.position().subtract(look.getStepX() * 1.6, 0, look.getStepZ() * 1.6);
                cmds(mc, String.format(Locale.ROOT, "tp @a %s %.1f 55", p(stand), look.toYRot()));
            }
        }, 20);
        record("gif_pickup", 60, at(12, mc -> mc.level.getEntitiesOfClass(StickyTrapEntity.class, mc.player.getBoundingBox().inflate(5)).stream()
                .min(Comparator.comparingDouble(t -> t.distanceToSqr(mc.player)))
                .ifPresent(t -> mc.gameMode.interact(mc.player, t, InteractionHand.MAIN_HAND))));
    }

    /** A smooth camera turn for the throw GIF: one server teleport per tick from {@code from} to {@code to} degrees. */
    private static void sweep(List<Event> events, int start, int ticks, float from, float to) {
        for (int i = 1; i <= ticks; i++) {
            float yaw = from + (to - from) * i / ticks;
            events.add(at(start + i, mc -> camera(mc, 0, 0, -6.5, yaw, 30)));
        }
    }

    private static void screens() {
        // The workbench: shift-click inputs in, watch the preview, shift-click the traps out.
        act(mc -> {
            clearArena(mc);
            cmds(mc, "clear @a", "give @a " + MOD + "trap_prototype 16", "give @a minecraft:slime_ball 4", "give @a minecraft:soul_lantern 4",
                    "give @a " + MOD + "hunters_knife 1");
            server(mc, level -> level.setBlockAndUpdate(wb(0, 3),
                    ModBlocks.HUNTERS_TABLE.get().defaultBlockState().setValue(HuntersTableBlock.FACING, look.getOpposite())));
            camera(mc, 0, 0, -0.4, 0, 25);
            mc.player.getInventory().selected = 0;
            VisualCheckJei.filter("@monster");
        }, 25);
        act(mc -> {
            BlockPos bench = wb(0, 3);
            Direction face = look.getOpposite();
            Vec3 hit = Vec3.atCenterOf(bench).add(face.getStepX() * 0.5, 0, face.getStepZ() * 0.5);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, bench, false));
        }, 15);
        record("gif_craft", 180,
                at(20, quickMove(30)), at(40, quickMove(31)), at(78, quickMove(2)),
                at(98, quickMove(32)), at(134, quickMove(2)), at(152, quickMove(33)));
        act(mc -> {
            mc.player.closeContainer();
            server(mc, level -> level.setBlockAndUpdate(wb(0, 3), Blocks.AIR.defaultBlockState()));
        }, 10);

        // The Monster Hunter's trades.
        act(mc -> server(mc, level -> hunter(level, w(0, 0, 1.6), towardCamera(), 1, false)), 15);
        act(mc -> mc.level.getEntitiesOfClass(Villager.class, mc.player.getBoundingBox().inflate(6)).stream()
                .min(Comparator.comparingDouble(v -> v.distanceToSqr(mc.player)))
                .ifPresent(v -> mc.gameMode.interact(mc.player, v, InteractionHand.MAIN_HAND)), 20);
        still("trades");
        act(mc -> mc.player.closeContainer(), 10);

        // Inventory with the knife's tooltip and JEI listing the mod's items.
        act(mc -> {
            clearArena(mc);
            cmds(mc, "clear @a", "give @a " + MOD + "hunters_knife", "give @a " + MOD + "sticky_trap_item 16",
                    "give @a " + MOD + "sharpened_trap_item 16", "give @a " + MOD + "soul_chain_trap_item 16", "give @a " + MOD + "trap_prototype 32",
                    "give @a " + MOD + "hunters_table");
        }, 10);
        act(mc -> {
            mc.setScreen(new InventoryScreen(mc.player));
            tooltipSlot = 0;
        }, 20);
        still("inventory");
        act(mc -> {
            tooltipSlot = -1;
            mc.setScreen(null);
        }, 5);

        // JEI's recipe page for the workbench.
        act(mc -> VisualCheckJei.showWorkbenchRecipes(), 20);
        still("jei_recipes");
        act(mc -> mc.setScreen(null), 5);
    }

    private static Consumer<Minecraft> quickMove(int slot) {
        return mc -> mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, 0, ClickType.QUICK_MOVE, mc.player);
    }

    private static void night() {
        act(mc -> {
            clearArena(mc);
            cmds(mc, "gamemode spectator @a", "time set 18000");
            mc.options.hideGui = true;
            mc.options.gamma().set(1.0);
            server(mc, level -> {
                trap(level, ModEntities.STICKY_TRAP.get(), w(-3, 0, 0), 30, player(level));
                trap(level, ModEntities.SHARPENED_TRAP.get(), w(0, 0, 0), -20, player(level));
                trap(level, ModEntities.SOUL_CHAIN_TRAP.get(), w(3, 0, 0), 0, player(level));
            });
            camera(mc, 0, 1.3, -4.4, 0, 14);
        }, 40);
        still("traps_night");
        act(mc -> {
            server(mc, level -> {
                var zombieHunter = spawn(level, EntityType.ZOMBIE_VILLAGER, w(3.8, 0, 3.4), towardCamera(), true);
                zombieHunter.setVillagerData(zombieHunter.getVillagerData().setProfession(ModVillagers.MONSTER_HUNTER.get()).setLevel(2));
                spawn(level, EntityType.ZOMBIE, w(6.2, 0, 1.4), towardCamera(), true);
            });
            camera(mc, 1.4, 2.2, -5.0, 0, 14);
        }, 30);
        still("soul_chain_night");
        act(mc -> {
            cmds(mc, "time set 2000");
            mc.options.gamma().set(0.5);
        }, 5);
    }

    /**
     * Before/after clips for the release notes. Each "before" clip is this same section recorded on a
     * build with that one fix reverted.
     */
    private static void fixes() {
        // /kill on the three traps: they should vanish, not get stuck halfway through dying.
        act(mc -> {
            clearArena(mc);
            cmds(mc, "gamemode spectator @a");
            mc.options.hideGui = true;
            server(mc, level -> {
                trap(level, ModEntities.STICKY_TRAP.get(), w(-3, 0, 0), 30, player(level));
                trap(level, ModEntities.SHARPENED_TRAP.get(), w(0, 0, 0), -20, player(level));
                trap(level, ModEntities.SOUL_CHAIN_TRAP.get(), w(3, 0, 0), 0, player(level));
            });
            camera(mc, 0, 1.3, -4.4, 0, 14);
        }, 40);
        record("gif_fix_kill", 60, at(20, mc -> cmds(mc,
                "kill @e[type=" + MOD + "sticky_trap]", "kill @e[type=" + MOD + "sharpened_trap]", "kill @e[type=" + MOD + "soul_chain_trap]")));

        // Throwing a trap in creative mode: the stack should stay the same size.
        act(mc -> {
            clearArena(mc);
            cmds(mc, "gamemode creative @a", "clear @a", "give @a " + MOD + "sticky_trap_item 4");
            camera(mc, 0, 0, -6.5, 0, 30);
            mc.options.hideGui = false;
        }, 30);
        act(mc -> mc.player.getInventory().selected = 0, 10);
        record("gif_fix_creative", 50, at(10, mc -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND)));
        act(mc -> {
            clearArena(mc);
            cmds(mc, "gamemode spectator @a", "clear @a");
            mc.options.hideGui = true;
        }, 10);
    }
}
