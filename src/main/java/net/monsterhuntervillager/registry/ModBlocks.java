package net.monsterhuntervillager.registry;

import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.block.HuntersTableBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MonsterHunterVillager.MODID);

    public static final DeferredBlock<HuntersTableBlock> HUNTERS_TABLE = BLOCKS.register("hunters_table", HuntersTableBlock::new);

    private ModBlocks() {
    }
}
