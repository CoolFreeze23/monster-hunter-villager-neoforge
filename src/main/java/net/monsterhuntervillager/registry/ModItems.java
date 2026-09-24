package net.monsterhuntervillager.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.monsterhuntervillager.entity.TrapKind;
import net.monsterhuntervillager.item.HuntersKnifeItem;
import net.monsterhuntervillager.item.TrapItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MonsterHunterVillager.MODID);

    public static final DeferredItem<TrapItem> STICKY_TRAP_ITEM = ITEMS.register("sticky_trap_item", () -> new TrapItem(TrapKind.STICKY));
    public static final DeferredItem<TrapItem> SHARPENED_TRAP_ITEM = ITEMS.register("sharpened_trap_item", () -> new TrapItem(TrapKind.SHARPENED));
    public static final DeferredItem<TrapItem> SOUL_CHAIN_TRAP_ITEM = ITEMS.register("soul_chain_trap_item", () -> new TrapItem(TrapKind.SOUL_CHAIN));
    public static final DeferredItem<BlockItem> HUNTERS_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.HUNTERS_TABLE);
    public static final DeferredItem<Item> TRAP_PROTOTYPE = ITEMS.registerSimpleItem("trap_prototype");
    public static final DeferredItem<HuntersKnifeItem> HUNTERS_KNIFE = ITEMS.register("hunters_knife", HuntersKnifeItem::new);

    private ModItems() {
    }
}
