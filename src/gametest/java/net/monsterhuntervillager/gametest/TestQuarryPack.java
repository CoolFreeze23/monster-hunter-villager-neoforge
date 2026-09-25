package net.monsterhuntervillager.gametest;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Test-only: in runs with GameTests enabled, turns on a small data pack that puts silverfish in the
 * {@code monster_hunter_villager:quarry} tag, the way an addon would add its own creatures.
 * Silverfish are not on the original list, so a hunter going after one proves the tag is read.
 */
@EventBusSubscriber(modid = MonsterHunterVillager.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class TestQuarryPack {
    private TestQuarryPack() {
    }

    @SubscribeEvent
    public static void addPack(AddPackFindersEvent event) {
        boolean gameTests = Boolean.getBoolean("neoforge.enableGameTest") || System.getProperty("neoforge.enabledGameTestNamespaces") != null;
        if (event.getPackType() != PackType.SERVER_DATA || !gameTests) {
            return;
        }
        event.addPackFinders(MonsterHunterVillager.id("test_packs/quarry"), PackType.SERVER_DATA,
                Component.literal("Monster Hunter Villager test quarry"), PackSource.BUILT_IN, true, Pack.Position.TOP);
    }
}
