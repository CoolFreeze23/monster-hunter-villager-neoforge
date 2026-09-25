package net.monsterhuntervillager.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.entity.SharpenedTrapEntity;
import net.monsterhuntervillager.entity.SoulChainTrapEntity;
import net.monsterhuntervillager.entity.StickyTrapEntity;
import net.monsterhuntervillager.entity.TrapKind;
import net.monsterhuntervillager.entity.TrapProjectileEntity;
import net.monsterhuntervillager.hunter.HunterCombat;
import net.monsterhuntervillager.hunter.MonsterHunterAI;
import net.monsterhuntervillager.menu.HuntersTableMenu;
import net.monsterhuntervillager.registry.ModAttachments;
import net.monsterhuntervillager.registry.ModBlocks;
import net.monsterhuntervillager.registry.ModEntities;
import net.monsterhuntervillager.registry.ModItems;
import net.monsterhuntervillager.registry.ModVillagers;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

@GameTestHolder(MonsterHunterVillager.MODID)
@PrefixGameTestTemplate(false)
public class MonsterHunterGameTests {
    /** 24x8x24: stone floor under open sky. GameTest places templates one block above the test origin, so the floor is relative y=1 and entities stand at y=2. */
    private static final String ARENA = "arena";
    private static final BlockPos MIDDLE = new BlockPos(12, 2, 12);

    /**
     * Traps remove themselves when no player is within 400 blocks (as in the original), so trap tests
     * park a real, in-level spectator well above the arena, out of every trap's reach.
     */
    private static ServerPlayer witness(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SPECTATOR);
        Vec3 above = helper.absoluteVec(new Vec3(12.5, 40.0, 12.5));
        player.moveTo(above.x, above.y, above.z);
        return player;
    }

    private static void release(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private static Villager hunter(GameTestHelper helper, BlockPos pos) {
        Villager villager = helper.spawn(EntityType.VILLAGER, pos);
        // Level 2 so vanilla's ResetProfession doesn't strip the job for lack of a workstation.
        villager.setVillagerData(villager.getVillagerData().setProfession(ModVillagers.MONSTER_HUNTER.get()).setLevel(2));
        return villager;
    }

    // ---- registration ------------------------------------------------------------------------

    @GameTest(template = ARENA)
    public static void workbenchIsTheMonsterHuntersJobSite(GameTestHelper helper) {
        Optional<Holder<PoiType>> poi = PoiTypes.forState(ModBlocks.HUNTERS_TABLE.get().defaultBlockState());
        helper.assertTrue(poi.isPresent(), "Hunter's Workbench is not a point of interest");
        helper.assertTrue(poi.get().is(ModVillagers.MONSTER_HUNTER_POI.getKey()), "Workbench maps to the wrong POI: " + poi.get());
        helper.assertTrue(poi.get().is(PoiTypeTags.ACQUIRABLE_JOB_SITE), "Workbench POI is missing from #acquirable_job_site");
        helper.assertTrue(ModVillagers.MONSTER_HUNTER.get().acquirableJobSite().test(poi.get()), "Profession does not accept its own workbench");
        helper.succeed();
    }

    /** The tent templates were saved by 1.20.1; they must upgrade and still contain the workbench. */
    @GameTest(template = ARENA)
    public static void tentTemplatesLoadAndHoldAWorkbench(GameTestHelper helper) {
        var templates = helper.getLevel().getStructureManager();
        for (String colour : List.of("blue", "brown", "cyan", "dark_gray", "gray", "green", "purple", "red", "white")) {
            var id = MonsterHunterVillager.id("monster_hunter_tent_" + colour);
            var template = templates.get(id);
            helper.assertTrue(template.isPresent(), "Tent template missing: " + id);
            var workbenches = template.get().filterBlocks(BlockPos.ZERO,
                    new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(), ModBlocks.HUNTERS_TABLE.get());
            helper.assertTrue(!workbenches.isEmpty(), "Tent " + colour + " has no Hunter's Workbench after upgrading");
        }
        helper.succeed();
    }

    // ---- workbench ---------------------------------------------------------------------------

    @GameTest(template = ARENA)
    public static void workbenchCraftsEachTrap(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.setBlock(MIDDLE, ModBlocks.HUNTERS_TABLE.get());
        HuntersTableMenu menu = new HuntersTableMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(MIDDLE)));

        expectResult(helper, menu, Items.SLIME_BALL, ModItems.STICKY_TRAP_ITEM.get());
        expectResult(helper, menu, Items.SOUL_LANTERN, ModItems.SOUL_CHAIN_TRAP_ITEM.get());
        expectResult(helper, menu, ModItems.HUNTERS_KNIFE.get(), ModItems.SHARPENED_TRAP_ITEM.get());
        expectResult(helper, menu, Items.DIRT, Items.AIR);

        // Shift-clicking the result crafts as many as the inputs allow and consumes them.
        menu.getSlot(HuntersTableMenu.PROTOTYPE_SLOT).set(new ItemStack(ModItems.TRAP_PROTOTYPE.get(), 2));
        menu.getSlot(HuntersTableMenu.ADDITION_SLOT).set(new ItemStack(Items.SLIME_BALL, 2));
        menu.clicked(HuntersTableMenu.RESULT_SLOT, 0, ClickType.QUICK_MOVE, player);
        int crafted = player.getInventory().countItem(ModItems.STICKY_TRAP_ITEM.get());
        helper.assertTrue(crafted == 2, "Expected 2 sticky traps from 2 prototypes + 2 slime balls, got " + crafted);
        helper.assertTrue(menu.getSlot(HuntersTableMenu.PROTOTYPE_SLOT).getItem().isEmpty(), "Prototypes were not consumed");
        helper.assertTrue(menu.getSlot(HuntersTableMenu.ADDITION_SLOT).getItem().isEmpty(), "Slime balls were not consumed");
        helper.assertTrue(menu.getSlot(HuntersTableMenu.RESULT_SLOT).getItem().isEmpty(), "Result slot not cleared after inputs ran out");

        // The prototype slot only takes prototypes.
        helper.assertFalse(menu.getSlot(HuntersTableMenu.PROTOTYPE_SLOT).mayPlace(new ItemStack(Items.SLIME_BALL)), "Prototype slot accepted a slime ball");
        helper.succeed();
    }

    private static void expectResult(GameTestHelper helper, HuntersTableMenu menu, Item addition, Item expected) {
        menu.getSlot(HuntersTableMenu.PROTOTYPE_SLOT).set(new ItemStack(ModItems.TRAP_PROTOTYPE.get()));
        menu.getSlot(HuntersTableMenu.ADDITION_SLOT).set(new ItemStack(addition));
        ItemStack result = menu.getSlot(HuntersTableMenu.RESULT_SLOT).getItem();
        helper.assertTrue(result.is(expected) && (expected == Items.AIR || result.getCount() == 1),
                "Prototype + " + addition + " gave " + result + ", expected " + expected);
        menu.getSlot(HuntersTableMenu.PROTOTYPE_SLOT).set(ItemStack.EMPTY);
        menu.getSlot(HuntersTableMenu.ADDITION_SLOT).set(ItemStack.EMPTY);
    }

    // ---- traps -------------------------------------------------------------------------------

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void thrownTrapDeploysOwnedTrap(GameTestHelper helper) {
        ServerPlayer witness = witness(helper);
        Villager thrower = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        TrapProjectileEntity projectile = TrapProjectileEntity.create(TrapKind.STICKY, helper.getLevel(), thrower);
        Vec3 start = helper.absoluteVec(new Vec3(12.5, 5.0, 12.5));
        projectile.setPos(start.x, start.y, start.z);
        projectile.shoot(0.0, -1.0, 0.0, 0.6F, 0.0F);
        helper.getLevel().addFreshEntity(projectile);

        helper.succeedWhen(() -> {
            List<StickyTrapEntity> traps = helper.getEntities(ModEntities.STICKY_TRAP.get());
            helper.assertTrue(traps.size() == 1, "Expected one deployed sticky trap, found " + traps.size());
            helper.assertTrue(traps.get(0).isOwnedBy(thrower), "Deployed trap is not owned by its thrower");
            helper.assertTrue(projectile.isRemoved(), "Projectile was not removed after deploying");
            release(helper, witness);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void stickyTrapHoldsWhatStepsInIt(GameTestHelper helper) {
        ServerPlayer witness = witness(helper);
        StickyTrapEntity trap = helper.spawn(ModEntities.STICKY_TRAP.get(), MIDDLE);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, MIDDLE);
        husk.setDeltaMovement(0.3, 0.0, 0.0);

        helper.succeedWhen(() -> {
            helper.assertTrue(HunterCombat.isTrapped(husk), "Husk standing in a sticky trap is not trapped");
            helper.assertTrue(trap.isInUse(), "Sticky trap does not register that it is holding something");
            helper.assertTrue(husk.getDeltaMovement().horizontalDistance() < 0.01, "Sticky trap did not stop the husk");
            release(helper, witness);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void sharpenedTrapCutsAStrugglingVictim(GameTestHelper helper) {
        ServerPlayer witness = witness(helper);
        helper.spawn(ModEntities.SHARPENED_TRAP.get(), MIDDLE);
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, MIDDLE);
        helper.onEachTick(() -> husk.setDeltaMovement(0.02, husk.getDeltaMovement().y, 0.0));

        helper.succeedWhen(() -> {
            helper.assertTrue(husk.getHealth() < husk.getMaxHealth(), "Sharpened trap did not hurt the husk");
            helper.assertTrue(husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Sharpened trap did not slow the husk");
            release(helper, witness);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void soulChainDragsEverythingButItsOwner(GameTestHelper helper) {
        ServerPlayer witness = witness(helper);
        SoulChainTrapEntity trap = helper.spawn(ModEntities.SOUL_CHAIN_TRAP.get(), MIDDLE);
        Villager owner = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new Vec3(12.5, 2.0, 15.5));
        trap.setOwner(owner);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(16.5, 2.0, 12.5));
        Vec3 trapPos = trap.position();
        double pigStart = pig.position().distanceTo(trapPos);
        Vec3 ownerStart = owner.position();

        helper.runAfterDelay(40, () -> {
            double pigNow = pig.position().distanceTo(trapPos);
            helper.assertTrue(pigNow < pigStart - 0.2, "Soul chain did not drag the pig in (" + pigStart + " -> " + pigNow + ")");
            helper.assertTrue(HunterCombat.isTrapped(pig), "Pig held by a soul chain is not trapped");
            helper.assertFalse(HunterCombat.isTrapped(owner), "Soul chain held its own owner");
            helper.assertTrue(owner.position().distanceTo(ownerStart) < 0.05, "Soul chain moved its own owner");
            release(helper, witness);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void ownerPicksTheirTrapBackUp(GameTestHelper helper) {
        ServerPlayer owner = witness(helper);
        owner.setGameMode(GameType.SURVIVAL);
        SharpenedTrapEntity trap = helper.spawn(ModEntities.SHARPENED_TRAP.get(), MIDDLE);
        trap.setOwner(owner);
        Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);

        trap.interact(stranger, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertFalse(trap.isRemoved(), "A stranger picked up someone else's trap");
        trap.interact(owner, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(trap.isRemoved(), "Owner could not pick their trap up");
        helper.succeedWhen(() -> {
            helper.assertItemEntityPresent(ModItems.SHARPENED_TRAP_ITEM.get(), MIDDLE, 1.5);
            release(helper, owner);
        });
    }

    /** A soul chain trap a player throws must never hold or drag that player. */
    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void thrownSoulChainSparesItsThrower(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 stand = helper.absoluteVec(new Vec3(12.5, 2.0, 6.5));
        player.moveTo(stand.x, stand.y, stand.z, 0.0F, 45.0F);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.SOUL_CHAIN_TRAP_ITEM.get()));
        ModItems.SOUL_CHAIN_TRAP_ITEM.get().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.runAfterDelay(60, () -> {
            List<SoulChainTrapEntity> traps = helper.getEntities(ModEntities.SOUL_CHAIN_TRAP.get());
            helper.assertTrue(traps.size() == 1, "Expected one deployed soul chain trap, found " + traps.size());
            SoulChainTrapEntity trap = traps.get(0);
            helper.assertTrue(trap.distanceTo(player) < 4.0, "Trap landed too far away to test (" + trap.distanceTo(player) + ")");
            helper.assertTrue(trap.isOwnedBy(player), "Deployed trap's owner is '" + trap.ownerId() + "', thrower is " + player.getStringUUID());
            helper.assertTrue(player.getStringUUID().equals(TrapJoinRecorder.OWNER_AT_JOIN.get(trap.getUUID())),
                    "The trap joined the world without its owner (owner at join: '" + TrapJoinRecorder.OWNER_AT_JOIN.get(trap.getUUID())
                            + "'), so clients would see it ownerless for a tick");
            helper.assertFalse(HunterCombat.isTrapped(player), "The soul chain trap held its own thrower");
            release(helper, player);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void killCommandRemovesTraps(GameTestHelper helper) {
        ServerPlayer witness = witness(helper);
        StickyTrapEntity trap = helper.spawn(ModEntities.STICKY_TRAP.get(), MIDDLE);
        trap.kill();
        helper.assertTrue(trap.isRemoved(), "/kill did not remove the trap");
        release(helper, witness);
        helper.succeed();
    }

    // ---- knife -------------------------------------------------------------------------------

    @GameTest(template = ARENA)
    public static void knifeBonusDependsOnTrapped(GameTestHelper helper) {
        Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        Husk free = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(8, 2, 8));
        Husk trapped = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(16, 2, 16));
        HunterCombat.markTrapped(trapped);

        HunterCombat.knifeHit(free, attacker);
        HunterCombat.knifeHit(trapped, attacker);

        float max = free.getMaxHealth();
        float freeDamage = max - free.getHealth();
        float trappedDamage = max - trapped.getHealth();
        // Husk armour is 2, so allow a little under the raw rolls: 1 + 1..6% and 4 + 5..8% of max health.
        helper.assertTrue(freeDamage > 0.9F && freeDamage <= 1.0F + max * 0.06F + 0.01F, "Untrapped knife bonus out of range: " + freeDamage);
        helper.assertTrue(trappedDamage > 4.0F && trappedDamage <= 4.0F + max * 0.08F + 0.01F, "Trapped knife bonus out of range: " + trappedDamage);
        helper.assertTrue(free.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && trapped.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Knife did not slow its targets");
        helper.succeed();
    }

    // ---- the hunter --------------------------------------------------------------------------

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void hunterTrapsANearbyMonster(GameTestHelper helper) {
        ServerPlayer witness = witness(helper);
        Villager hunter = hunter(helper, new BlockPos(8, 2, 12));
        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(12, 2, 12));

        helper.succeedWhen(() -> {
            helper.assertTrue(husk.getUUID().equals(hunter.getData(ModAttachments.HUNTER_STATE).quarry()), "Hunter never picked the husk as its quarry");
            boolean trapThrown = helper.getEntities(ModEntities.STICKY_TRAP.get()).stream().anyMatch(trap -> trap.isOwnedBy(hunter))
                    || helper.getEntities(ModEntities.SHARPENED_TRAP.get()).stream().anyMatch(trap -> trap.isOwnedBy(hunter));
            helper.assertTrue(trapThrown, "Hunter has not thrown a trap at the husk yet");
            release(helper, witness);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void hunterHuntsWhatTheQuarryTagAdds(GameTestHelper helper) {
        // TestQuarryPack puts silverfish in the quarry tag; they are not on the original list.
        helper.assertTrue(EntityType.SILVERFISH.is(MonsterHunterAI.QUARRY), "The test data pack did not put silverfish in the quarry tag");
        ServerPlayer witness = witness(helper);
        Villager hunter = hunter(helper, new BlockPos(8, 2, 12));
        Silverfish silverfish = helper.spawnWithNoFreeWill(EntityType.SILVERFISH, new BlockPos(12, 2, 12));

        helper.succeedWhen(() -> {
            helper.assertTrue(silverfish.getUUID().equals(hunter.getData(ModAttachments.HUNTER_STATE).quarry()), "Hunter never picked the tagged silverfish as its quarry");
            boolean trapThrown = helper.getEntities(ModEntities.STICKY_TRAP.get()).stream().anyMatch(trap -> trap.isOwnedBy(hunter))
                    || helper.getEntities(ModEntities.SHARPENED_TRAP.get()).stream().anyMatch(trap -> trap.isOwnedBy(hunter));
            helper.assertTrue(trapThrown, "Hunter has not thrown a trap at the silverfish yet");
            release(helper, witness);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void hunterIgnoresMobsOffTheList(GameTestHelper helper) {
        // Endermites are neither on the original list nor in the quarry tag.
        Villager hunter = hunter(helper, new BlockPos(8, 2, 12));
        helper.spawnWithNoFreeWill(EntityType.ENDERMITE, new BlockPos(12, 2, 12));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(hunter.getData(ModAttachments.HUNTER_STATE).quarry() == null, "Hunter went after an endermite");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void hunterTurnsOnWhoeverHurtsIt(GameTestHelper helper) {
        Villager hunter = hunter(helper, MIDDLE);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        hunter.hurt(hunter.damageSources().mobAttack(zombie), 1.0F);
        helper.assertTrue(zombie.getUUID().equals(hunter.getData(ModAttachments.HUNTER_STATE).quarry()), "Hunter did not turn on its attacker");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void ordinaryMobsAreLeftAlone(GameTestHelper helper) {
        // The original wrote tracking data into the saved NBT of every living entity and treated
        // every entity as a potential hunter. Nothing should touch mobs that aren't involved.
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        Villager farmer = helper.spawn(EntityType.VILLAGER, new BlockPos(16, 2, 16));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(zombie.getPersistentData().isEmpty(), "Zombie picked up persistent data: " + zombie.getPersistentData());
            helper.assertFalse(farmer.hasData(ModAttachments.HUNTER_STATE), "An ordinary villager was given hunter state");
            helper.assertFalse(zombie.hasData(ModAttachments.TRAPPED_UNTIL), "An untrapped zombie was marked");
            helper.succeed();
        });
    }
}
