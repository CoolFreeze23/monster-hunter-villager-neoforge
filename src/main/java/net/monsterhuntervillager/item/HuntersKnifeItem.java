package net.monsterhuntervillager.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.monsterhuntervillager.hunter.HunterCombat;

import java.util.List;

public class HuntersKnifeItem extends SwordItem {
    private static final Tier TIER = new Tier() {
        @Override
        public int getUses() {
            return 156;
        }

        @Override
        public float getSpeed() {
            return 4.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return -1.0F;
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_STONE_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 7;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };

    public HuntersKnifeItem() {
        super(TIER, new Item.Properties().attributes(SwordItem.createAttributes(TIER, 3, -1.4F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        HunterCombat.knifeHit(target, attacker);
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        for (int line = 1; line <= 3; line++) {
            tooltip.add(Component.translatable("item.monster_hunter_villager.hunters_knife.desc" + line).withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
