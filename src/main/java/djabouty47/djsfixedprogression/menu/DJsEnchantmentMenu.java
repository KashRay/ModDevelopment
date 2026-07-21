package djabouty47.djsfixedprogression.menu;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import djabouty47.djsfixedprogression.procedures_and_util.EnchantmentCostHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class DJsEnchantmentMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    public final Container enchantmentSlots = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            DJsEnchantmentMenu.this.slotsChanged(this);
        }
    };

    /**
     * Client-side constructor.
     */
    public DJsEnchantmentMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    /**
     * Server-side constructor.
     */
    public DJsEnchantmentMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(DJsFixedProgression.DJS_ENCHANTMENT_MENU, containerId);
        this.access = access;

        //Slot 0: the item being enchanted
        this.addSlot(new Slot(this.enchantmentSlots, 0, 8, 54) {
            @Override
            public int getMaxStackSize() { return 1; }
        });
        //Slot 1: lapis lazuli slot
        this.addSlot(new Slot(this.enchantmentSlots, 1, 26, 54) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) { return stack.is(Items.LAPIS_LAZULI); }
        });
        //Slot 2: ingredient slot
        this.addSlot(new Slot(this.enchantmentSlots, 2, 44, 54) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) { return !stack.isEmpty() && stack.getMaxStackSize() > 1; }
        });

        //Add standard Player Inventory slots
        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        //Add standard Player Hotbar slots
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemInSlot = slot.getItem();
            itemStack = itemInSlot.copy();

            //Taking items out of enchanting table (slots 0 = tool, slot 1 = lapis lazuli, slot 2 = ingredient)
            if (index >= 0 && index <= 2) {
                //Move back into player inventory slots
                if (!this.moveItemStackTo(itemInSlot, 3, 39, true)) return ItemStack.EMPTY;
            }
            //Moving items into enchanting table from inventory
            else {
                //Moving lapis into lapis slot
                if (itemInSlot.is(Items.LAPIS_LAZULI)) {
                    if (!this.moveItemStackTo(itemInSlot, 1, 2, false)) return ItemStack.EMPTY;
                }
                //Moving tool into slot 0
                else if (this.slots.get(0).mayPlace(itemInSlot) && itemInSlot.getMaxStackSize() == 1) {
                    if (!this.moveItemStackTo(itemInSlot, 0, 1, false)) return ItemStack.EMPTY;
                }
                //Moving ingredients into slot 2
                else if (this.slots.get(2).mayPlace(itemInSlot)) {
                    if (!this.moveItemStackTo(itemInSlot, 2, 3, false)) return ItemStack.EMPTY;
                }
                //Moving to inventory
                else if (index >= 3 && index <= 29) {
                    if (!this.moveItemStackTo(itemInSlot, 30, 39, false)) return ItemStack.EMPTY;
                }
                //Moving into hotbar
                else if (index >= 30 && index <= 38) {
                    if (!this.moveItemStackTo(itemInSlot, 3, 30, false)) return ItemStack.EMPTY;
                }
            }

            if (itemInSlot.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (itemInSlot.getCount() == itemStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, itemInSlot);
        }

        return itemStack;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.enchantmentSlots));
    }

    /**
     * Method called when player clicks an enchantment slot but doesn't have the items in the payment slot yet.
     * Moves item from their inventory into the ingredient slot.
     */
    public void tryAutoMoveItems(Identifier enchantmentId, boolean useToken, Player player) {
        //Creative players don't need to move items
        if (player.isCreative()) return;

        //Ensure tool is in the enchantment slot
        ItemStack toolStack = this.enchantmentSlots.getItem(0);
        if (toolStack.isEmpty()) return;

        //Fetch enchantment data
        var registry = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var optionalEnchantment = registry.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, enchantmentId));
        if (optionalEnchantment.isEmpty()) return;
        var enchantmentHolder = optionalEnchantment.get();

        //Get next level for enchantment
        int nextLevel = toolStack.getEnchantments().getLevel(enchantmentHolder) + 1;

        //Get enchantment and curse token status
        boolean isCurse = enchantmentHolder.is(EnchantmentTags.CURSE);
        int curseTokens = EnchantmentCostHelper.getAvailableCurseTokens(toolStack);
        boolean usingToken = !isCurse && useToken && curseTokens > 0;

        //If using a token, no ingredients are required
        if (usingToken) return;

        //Calculate the dynamic enchantment cost
        var costData = EnchantmentCostHelper.calculateCost(toolStack, enchantmentHolder, nextLevel);
        int lapisNeeded = EnchantmentCostHelper.getTierMultiplier(toolStack);

        //Cannot auto-move if item is anvil lock
        if (costData.ingredient() == Items.ANVIL) return;

        boolean allReturned = true;

        //Return any items currently in lapis payment slot back to player's inventory
        Slot lapisSlot = this.slots.get(1);
        if (lapisSlot.hasItem() && !lapisSlot.getItem().is(Items.LAPIS_LAZULI)) {
            ItemStack current = lapisSlot.getItem();

            //Attempt to move item natively
            this.moveItemStackTo(current, 3, 39, false);

            //Check if current ingredient is empty
            if (!current.isEmpty()) allReturned = false;
        }

        //Return any items currently in ingredient payment slot back to player's inventory
        Slot ingredientSlot = this.slots.get(2);
        if (ingredientSlot.hasItem() && !ingredientSlot.getItem().is(costData.ingredient())) {
            ItemStack current = ingredientSlot.getItem();

            //Attempt to move item natively
            this.moveItemStackTo(current, 3, 39, false);

            //Check if current ingredient is empty
            if (!current.isEmpty()) allReturned = false;
        }

        //Put lapis and ingredients if all items have been returned
        if (allReturned) {
            pullItemIntoSlot(lapisSlot, Items.LAPIS_LAZULI, lapisNeeded);
            pullItemIntoSlot(ingredientSlot, costData.ingredient(), costData.ingredientAmount());

            //Sync newly moved items to the client screen
            this.broadcastChanges();
        }
    }

    /**
     * Helper method for fetching items from the players inventory and moving it into the corresponding ingredient slot.
     */
    private void pullItemIntoSlot(Slot targetSlot, Item itemType, int totalNeeded) {
        //Calculate required ingredient amount
        int currentlyInSlot = targetSlot.hasItem() ? targetSlot.getItem().getCount() : 0;
        int needed = totalNeeded - currentlyInSlot;
        if (needed <= 0) return;

        ItemStack newPayment = targetSlot.hasItem() ? targetSlot.getItem() : ItemStack.EMPTY;

        //Loop over all inventory slots
        for (int i = 3; i < 39 && needed > 0; i++) {
            Slot invSlot = this.slots.get(i);
            if (invSlot.hasItem() && invSlot.getItem().is(itemType)) {
                ItemStack invStack = invSlot.getItem();
                int take = Math.min(invStack.getCount(), needed);

                //Deduct items
                invStack.shrink(take);
                if (newPayment.isEmpty()) newPayment = new ItemStack(itemType, take);
                else newPayment.grow(take);
                needed -= take;
                invSlot.setChanged();
            }
        }

        //Notify of new change
        targetSlot.set(newPayment);
        targetSlot.setChanged();
    }
}
