package net.monsterhuntervillager.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.monsterhuntervillager.entity.TrapKind;
import net.monsterhuntervillager.entity.TrapProjectileEntity;

/** A throwable trap: right-click throws it, and it deploys where it lands. */
public class TrapItem extends Item {
    private static final int THROW_COOLDOWN = 60;
    private static final float THROW_INACCURACY = 4.0F;

    private final TrapKind kind;

    public TrapItem(TrapKind kind) {
        super(new Item.Properties().stacksTo(16));
        this.kind = kind;
    }

    public TrapKind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, THROW_COOLDOWN);
        if (!level.isClientSide) {
            TrapProjectileEntity projectile = TrapProjectileEntity.create(kind, level, player);
            Vec3 look = player.getLookAngle();
            projectile.shoot(look.x, look.y, look.z, kind.throwSpeed(), THROW_INACCURACY);
            level.addFreshEntity(projectile);
        }
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
