package net.monsterhuntervillager.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The bare trap frame shown in the Hunter's Workbench preview before a trap type is chosen. Inert. */
public class EmptyTrapEntity extends AbstractTrapEntity {
    public EmptyTrapEntity(EntityType<? extends EmptyTrapEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public TrapKind kind() {
        return null;
    }

    @Override
    protected void affectVictims() {
    }
}
