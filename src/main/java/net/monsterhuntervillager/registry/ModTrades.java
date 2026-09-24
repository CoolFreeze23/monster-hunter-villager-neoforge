package net.monsterhuntervillager.registry;

import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.monsterhuntervillager.MonsterHunterVillager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

/** The Monster Hunter's trade table, unchanged from the original (same items, prices, uses, XP and multipliers). */
@EventBusSubscriber(modid = MonsterHunterVillager.MODID)
public final class ModTrades {
    private ModTrades() {
    }

    @SubscribeEvent
    public static void registerTrades(VillagerTradesEvent event) {
        if (event.getType() != ModVillagers.MONSTER_HUNTER.get()) {
            return;
        }
        var trades = event.getTrades();

        List<ItemListing> novice = trades.get(1);
        novice.add(sell(Items.LEATHER, 8, Items.EMERALD, 3, 15, 4, 0.05F));
        novice.add(sell(Items.RABBIT_FOOT, 2, Items.EMERALD, 3, 10, 4, 0.05F));
        novice.add(sell(Items.EMERALD, 16, ModItems.TRAP_PROTOTYPE.get(), 3, 4, 5, 0.05F));

        List<ItemListing> apprentice = trades.get(2);
        apprentice.add(sell(Items.ROTTEN_FLESH, 20, Items.EMERALD, 1, 40, 2, 0.05F));
        apprentice.add(sell(Items.BONE, 15, Items.EMERALD, 2, 35, 2, 0.05F));
        apprentice.add(sell(Items.SPIDER_EYE, 8, Items.EMERALD, 3, 25, 2, 0.05F));
        apprentice.add(sell(Items.SLIME_BALL, 3, Items.EMERALD, 3, 32, 3, 0.05F));
        apprentice.add(sell(Items.EMERALD, 8, Items.LEAD, 1, 10, 4, 0.05F));

        List<ItemListing> journeyman = trades.get(3);
        journeyman.add(sell(Items.EMERALD, 16, Items.BOW, 1, 2, 10, 0.05F));
        journeyman.add(sell(Items.GUNPOWDER, 10, Items.EMERALD, 2, 10, 2, 0.05F));
        journeyman.add(sell(Items.ENDER_PEARL, 1, Items.EMERALD, 3, 12, 4, 0.0F));
        journeyman.add(sell(Items.EMERALD, 34, ModItems.HUNTERS_KNIFE.get(), 1, 2, 20, 0.05F));
        journeyman.add(sell(Items.EMERALD, 22, ModItems.STICKY_TRAP_ITEM.get(), 4, 4, 12, 0.05F));
        journeyman.add(sell(Items.EMERALD, 52, ModItems.SHARPENED_TRAP_ITEM.get(), 1, 10, 10, 0.05F));
        journeyman.add(sell(Items.PRISMARINE_CRYSTALS, 1, Items.EMERALD, 1, 10, 8, 0.0F));
        journeyman.add(sell(Items.PRISMARINE_SHARD, 1, Items.EMERALD, 2, 10, 8, 0.0F));

        List<ItemListing> expert = trades.get(4);
        expert.add(sell(Items.PHANTOM_MEMBRANE, 2, Items.EMERALD, 3, 14, 5, 0.05F));
        expert.add(sell(Items.GHAST_TEAR, 1, Items.EMERALD, 8, 14, 5, 0.0F));
        expert.add(sell(Items.EMERALD, 19, Items.CROSSBOW, 1, 2, 14, 0.05F));
        expert.add(sell(Items.EMERALD, 6, Items.SPECTRAL_ARROW, 4, 6, 8, 0.05F));
        expert.add(sell(Items.EMERALD, 5, Items.ENDER_PEARL, 1, 8, 8, 0.01F));
        expert.add(sell(Items.EMERALD, 2, Items.FERMENTED_SPIDER_EYE, 2, 8, 6, 0.01F));
        expert.add(sell(Items.TOTEM_OF_UNDYING, 1, Items.EMERALD, 22, 5, 16, 0.0F));

        List<ItemListing> master = trades.get(5);
        master.add(sell(Items.SHULKER_SHELL, 1, Items.EMERALD, 2, 12, 5, 0.0F));
        master.add(sell(Items.BLAZE_ROD, 2, Items.EMERALD, 2, 15, 4, 0.05F));
        master.add(sell(Items.ENDER_EYE, 1, Items.EMERALD, 8, 15, 8, 0.0F));
        master.add(sell(Items.WITHER_SKELETON_SKULL, 1, Items.EMERALD, 20, 10, 8, 0.0F));
        master.add(sell(Items.DRAGON_BREATH, 1, Items.EMERALD, 24, 8, 10, 0.0F));
        master.add(sell(Items.NETHER_STAR, 1, Blocks.EMERALD_BLOCK, 9, 1, 26, 0.0F));
        master.add(sell(Blocks.DRAGON_EGG, 1, Blocks.EMERALD_BLOCK, 60, 1, 32, 0.0F));
        master.add(sell(Blocks.EMERALD_BLOCK, 62, Blocks.DRAGON_EGG, 1, 1, 18, 0.0F));
        master.add(sell(Items.DRAGON_HEAD, 1, Items.EMERALD, 40, 10, 16, 0.0F));
        master.add(sell(Blocks.SCULK_CATALYST, 1, Items.EMERALD, 6, 10, 12, 0.0F));
        master.add(sell(Items.ECHO_SHARD, 1, Items.EMERALD, 10, 10, 10, 0.0F));
    }

    private static ItemListing sell(ItemLike price, int priceCount, ItemLike result, int resultCount, int maxUses, int xp, float priceMultiplier) {
        return new BasicItemListing(new ItemStack(price, priceCount), new ItemStack(result, resultCount), maxUses, xp, priceMultiplier);
    }
}
