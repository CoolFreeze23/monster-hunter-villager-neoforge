package net.monsterhuntervillager.hunter;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** What a Monster Hunter remembers between ticks. Saved with the villager. */
public final class HunterState implements INBTSerializable<CompoundTag> {
    /** The entity the hunter is after: the last monster it spotted, or whoever last hurt it. */
    @Nullable
    UUID quarry;
    /** Counts down while engaged; traps are only thrown during its last 20 ticks. Re-rolled to 30-100 at zero. */
    double trapCooldown;
    /** Ticks until the next trap throw or potion. */
    double throwCooldown;
    /** Ticks until the next knife strike. */
    double meleeCooldown;

    @Nullable
    public UUID quarry() {
        return quarry;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (quarry != null) {
            tag.putUUID("Quarry", quarry);
        }
        tag.putDouble("TrapCooldown", trapCooldown);
        tag.putDouble("ThrowCooldown", throwCooldown);
        tag.putDouble("MeleeCooldown", meleeCooldown);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        quarry = tag.hasUUID("Quarry") ? tag.getUUID("Quarry") : null;
        trapCooldown = tag.getDouble("TrapCooldown");
        throwCooldown = tag.getDouble("ThrowCooldown");
        meleeCooldown = tag.getDouble("MeleeCooldown");
    }
}
