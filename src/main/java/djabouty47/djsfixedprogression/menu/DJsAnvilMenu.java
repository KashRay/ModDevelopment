package djabouty47.djsfixedprogression.menu;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class DJsAnvilMenu extends ItemCombinerMenu {
    //Data slot for tracking and syncing XP cost to client UI
    public final DataSlot cost = DataSlot.standalone();


    public DJsAnvilMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public DJsAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(DJsFixedProgression.DJS_ANVIL_MENU, containerId, playerInventory, access, createInputSlotDefinitions());

        //Register cost slot so server syncs it to the player's screen
        this.addDataSlot(this.cost);
    }

    /**
     * Helper method to define the layout, passed directly to the super constructor.
     */
    private static ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
        return ItemCombinerMenuSlotDefinition.create()
                .withSlot(0, 53, 34, stack -> true) // Slot 0: gear
                .withSlot(1, 93, 34, stack -> true) // Slot 1: repair item/material, enchant, or name tag
                .withResultSlot(2, 141, 34) // Slot 2: output
                .build();
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(BlockTags.ANVIL);
    }

    @Override
    protected boolean mayPickup(@NotNull Player player, boolean hasItem) {
        //Player can take item if they are in creative or have enough XP
        return hasItem && (player.isCreative() || player.experienceLevel >= this.cost.get());
    }

    @Override
    protected void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        //Deduct XP cost securely on server
        if (!player.isCreative()) player.giveExperienceLevels(-this.cost.get());

        //Player successfully forged item, shrink ingredients by 1
        this.inputSlots.getItem(0).shrink(1);
        this.inputSlots.getItem(1).shrink(1);

        //Handle anvil sound and block degradation
        this.access.execute((level, pos) -> {
            BlockState blockState = level.getBlockState(pos);

            //12% chance to degrade the anvil if the player is not in creative
            if (!player.isCreative() && blockState.is(BlockTags.ANVIL) && player.getRandom().nextFloat() < 0.12F) {
                BlockState damagedState = AnvilBlock.damage(blockState);
                //Check if anvil is destroyed
                if (damagedState == null) {
                    //Remove anvil and play breaking sound
                    level.removeBlock(pos, false);
                    level.levelEvent(1029, pos, 0);
                }
                //Use anvil and damage
                else {
                    level.setBlock(pos, damagedState, 2);
                    level.levelEvent(1030, pos, 0);
                }
            }
            //If the anvil didn't degrade, just play the normal sound
            else level.levelEvent(1030, pos, 0);
        });
    }

    @Override
    public void createResult() {
        ItemStack input1 = this.getSlot(0).getItem();
        ItemStack input2 = this.getSlot(1).getItem();

        //If left slot is empty, clear result and stop
        if (input1.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
            this.broadcastChanges();
            return;
        }

        ItemStack result = input1.copy();

        //Check if renaming item with a nametag
        if (input2.is(Items.NAME_TAG)) {
            //Set custom name
            result.set(DataComponents.CUSTOM_NAME, input2.getHoverName());
            this.resultSlots.setItem(0, result);

            //Charge 0 XP
            this.cost.set(0);

            //Return changes
            this.broadcastChanges();
            return;
        }

        //Check if repairing an item with a material
        if (input1.isValidRepairItem(input2)) {
            //Check if the item is damaged
            if (input1.isDamaged()) {
                //Repair item
                result.setDamageValue(0);
                this.resultSlots.setItem(0, result);

                //Charge XP depending on how much durability was fixed
                double damageRatio = (double) input1.getDamageValue() / input1.getMaxDamage();
                int xpCost = (int) Math.max(1, Math.ceil(damageRatio * 4.0));
                this.cost.set(xpCost);
            }
            //Otherwise block repair
            else {
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                this.cost.set(0);
            }

            //Return changes
            this.broadcastChanges();
            return;
        }

        //Check if combining gear and/or books
        boolean isInput1Book = input1.is(Items.ENCHANTED_BOOK);
        boolean isInput2Book = input2.is(Items.ENCHANTED_BOOK);

        //Ensure items are the same type or second item is a book
        if (!input1.is(input2.getItem()) && !isInput2Book) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
            this.broadcastChanges();
            return;
        }

        //Get enchantments on current item and item to be combined
        ItemEnchantments.Mutable currentEnchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(input1));
        ItemEnchantments incomingEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(input2);

        //Track which items are enchanted
        boolean input1Enchanted = !currentEnchantments.keySet().isEmpty();
        boolean input2Enchanted = !incomingEnchantments.keySet().isEmpty();

        //Track if changes were made
        boolean hasChanges = false;
        boolean hasEnchantmentChanges = false;

        //Check if an enchanted book is being applied to gear
        if ((input1.is(Items.ENCHANTED_BOOK) || input2.is(Items.ENCHANTED_BOOK)) && (input1Enchanted || input2Enchanted)) hasEnchantmentChanges = true;
        //Check if enchanted gear is being combined
        else if (input1Enchanted && input2Enchanted) hasEnchantmentChanges = true;

        //Check if combining gear
        if (result.isDamageableItem() && input1.getItem() == input2.getItem()) {
            //Merge durability
            int maxDamage = result.getMaxDamage();
            int health1 = maxDamage - input1.getDamageValue();
            int health2 = maxDamage - input2.getDamageValue();

            //Add both health pools and 12% extra durability
            int combinedHealth = health1 + health2 + (int) (maxDamage * 0.12);
            int newDamage = maxDamage - combinedHealth;
            if (newDamage < 0) newDamage = 0;

            if (newDamage < result.getDamageValue()) {
                result.setDamageValue(newDamage);
                hasChanges = true;
            }
        }

        //Loop over all enchantments on item to be combined
        for (Holder<@NotNull Enchantment> enchantment : incomingEnchantments.keySet()) {
            //Get the level of the current enchantment and incoming enchantment
            int incomingLevel = incomingEnchantments.getLevel(enchantment);
            int currentLevel = currentEnchantments.getLevel(enchantment);

            //Get the final enchantment level
            int finalLevel;
            if (currentLevel == incomingLevel && incomingLevel > 0) {
                finalLevel = Math.min(currentLevel + 1, enchantment.value().getMaxLevel());
                //If the level increased past both original items, charge XP
                if (finalLevel > currentLevel) hasEnchantmentChanges = true;
            }
            else finalLevel = Math.max(currentLevel, incomingLevel);

            //Check compatability against existing enchantments
            boolean isCompatible = true;
            for (Holder<@NotNull Enchantment> existing : currentEnchantments.keySet()) {
                if (!existing.equals(enchantment) && !Enchantment.areCompatible(enchantment, existing)) {
                    isCompatible = false;
                    break;
                }
            }

            //Apply if enchantments are compatible and if enchantment can be improved
            if (isCompatible && finalLevel > currentLevel) {
                currentEnchantments.set(enchantment, finalLevel);
                hasChanges = true;
            }
        }

        //If resulting item has changes, process final result
        if (hasChanges) {
            EnchantmentHelper.setEnchantments(result, currentEnchantments.toImmutable());
            int finalCost = 0;

            //Calculate final enchantment cost based on final item's enchantments
            if (hasEnchantmentChanges && (result.isEnchanted() || result.is(Items.ENCHANTED_BOOK))) {
                ItemEnchantments resultEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(result);

                //Add each enchantment level to cost
                for (Object2IntMap.Entry<Holder<@NotNull Enchantment>> entry : resultEnchantments.entrySet()) finalCost += entry.getIntValue();
            }

            //Set final cost and result item
            this.cost.set(finalCost);
            this.resultSlots.setItem(0, result);
        }
        //Otherwise block final result changes
        else {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.cost.set(0);
        }

        //Return changes
        this.broadcastChanges();
    }
}
