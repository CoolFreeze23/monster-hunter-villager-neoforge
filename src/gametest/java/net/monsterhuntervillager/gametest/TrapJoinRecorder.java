package net.monsterhuntervillager.gametest;

import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.entity.AbstractTrapEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-only: records each trap's owner at the moment it joins the world. Clients receive a trap's
 * data as it is when it joins, so an owner set any later reaches them a tick late.
 */
@EventBusSubscriber(modid = MonsterHunterVillager.MODID)
public final class TrapJoinRecorder {
    static final Map<UUID, String> OWNER_AT_JOIN = new ConcurrentHashMap<>();

    private TrapJoinRecorder() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AbstractTrapEntity trap) {
            OWNER_AT_JOIN.put(trap.getUUID(), trap.ownerId());
        }
    }
}
