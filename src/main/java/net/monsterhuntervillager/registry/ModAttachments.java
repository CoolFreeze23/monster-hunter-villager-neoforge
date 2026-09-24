package net.monsterhuntervillager.registry;

import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.hunter.HunterState;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MonsterHunterVillager.MODID);

    // Saved with the villager: current quarry and the three combat cooldowns.
    public static final Supplier<AttachmentType<HunterState>> HUNTER_STATE =
            ATTACHMENTS.register("hunter_state", () -> AttachmentType.serializable(HunterState::new).build());

    // Game time until which an entity counts as held by a trap. Deliberately not saved:
    // the original kept a 5-tick "trapped" counter in the persistent NBT of every living
    // entity and decremented it every tick, which bloated every mob's save data.
    public static final Supplier<AttachmentType<Long>> TRAPPED_UNTIL =
            ATTACHMENTS.register("trapped_until", () -> AttachmentType.builder(() -> 0L).build());

    private ModAttachments() {
    }
}
