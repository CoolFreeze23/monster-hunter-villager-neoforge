package net.monsterhuntervillager.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.monsterhuntervillager.recipe.HuntersWorkbenchInput;
import net.monsterhuntervillager.registry.ModBlocks;
import net.monsterhuntervillager.registry.ModItems;
import net.monsterhuntervillager.registry.ModMenus;
import net.monsterhuntervillager.registry.ModRecipes;

/**
 * The Hunter's Workbench screen: trap prototype + addition = trap.
 *
 * <p>The original recomputed the output slot from a player-tick hook every tick while the screen
 * was open and consumed the inputs through a client-to-server packet. The result is now updated
 * only when an input changes, and taking it consumes the inputs on the server directly.
 */
public class HuntersTableMenu extends AbstractContainerMenu {
    public static final int PROTOTYPE_SLOT = 0;
    public static final int ADDITION_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INVENTORY_START = 3;
    private static final int HOTBAR_START = 30;
    private static final int HOTBAR_END = 39;

    private final ContainerLevelAccess access;
    private final Level level;
    private final Container inputs = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ResultContainer result = new ResultContainer();

    public HuntersTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(id, inventory, ContainerLevelAccess.create(inventory.player.level(), extraData.readBlockPos()));
    }

    public HuntersTableMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.HUNTERS_TABLE.get(), id);
        this.access = access;
        this.level = inventory.player.level();

        addSlot(new Slot(inputs, 0, 13, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.TRAP_PROTOTYPE.get());
            }
        });
        addSlot(new Slot(inputs, 1, 13, 17));
        addSlot(new Slot(result, 0, 76, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                stack.onCraftedBy(player.level(), player, stack.getCount());
                inputs.removeItem(0, 1);
                inputs.removeItem(1, 1);
                super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    public ItemStack prototype() {
        return inputs.getItem(0);
    }

    public ItemStack result() {
        return result.getItem(0);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == inputs) {
            updateResult();
        }
    }

    private void updateResult() {
        if (!(level instanceof ServerLevel server)) {
            return; // the server's result is synced to the client
        }
        HuntersWorkbenchInput input = new HuntersWorkbenchInput(inputs.getItem(0), inputs.getItem(1));
        ItemStack output = server.getRecipeManager()
                .getRecipeFor(ModRecipes.HUNTERS_WORKBENCH.get(), input, server)
                .map(holder -> holder.value().assemble(input, server.registryAccess()))
                .orElse(ItemStack.EMPTY);
        result.setItem(0, output);
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.HUNTERS_TABLE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((world, pos) -> clearContainer(player, inputs));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == RESULT_SLOT) {
            if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else if (index < INVENTORY_START) {
            if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PROTOTYPE_SLOT, RESULT_SLOT, false)) {
            if (index < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, INVENTORY_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }
}
