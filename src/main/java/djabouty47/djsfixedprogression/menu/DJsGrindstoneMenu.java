package djabouty47.djsfixedprogression.menu;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class DJsGrindstoneMenu extends AbstractContainerMenu {
    private final Container repairSlot = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            DJsGrindstoneMenu.this.slotsChanged(this);
        }
    };
    private final Container resultSlot = new ResultContainer();
    private final ContainerLevelAccess access;
    private int xpToDrop = 0;

    public DJsGrindstoneMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public DJsGrindstoneMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(DJsFixedProgression.DJS_GRINDSTONE_MENU, containerId);
        this.access = access;

        //Input slot
        this.addSlot(new Slot(this.repairSlot, 0, 49, 34) {
            @Override
            public boolean mayPlace(@NotNull ItemStack itemStack) {
                return itemStack.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(itemStack);
            }
        });

        //Output slot
        this.addSlot(new Slot(this.resultSlot, 1, 129, 34) {
            @Override
            public boolean mayPlace(@NotNull ItemStack itemStack) { return false; }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack itemStack) {
                access.execute((level, pos) -> {
                    if (level instanceof ServerLevel serverLevel && xpToDrop > 0) {
                        ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), xpToDrop);
                    }
                    level.levelEvent(1042, pos, 0);
                });
                repairSlot.getItem(0).shrink(1);
            }
        });

        //Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; ++col) this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.clearContainer(player, this.repairSlot);
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        if (container == this.repairSlot) this.createResult();
    }

    private void createResult() {
        ItemStack input = this.repairSlot.getItem(0);
        this.xpToDrop = 0;

        //If input is empty, result is empty
        if (input.isEmpty()) this.resultSlot.setItem(0, ItemStack.EMPTY);
        else {
            //If gear has enchantments, strip enchantments from gear
            if (EnchantmentHelper.hasAnyEnchantments(input)) {
                this.xpToDrop = getExperienceFromItem(input);
                ItemStack stripped = input.copy();

                ItemEnchantments itemEnchantments = EnchantmentHelper.updateEnchantments(stripped, mutable -> mutable.removeIf(holder -> !holder.is(EnchantmentTags.CURSE)));
                if (stripped.is(Items.ENCHANTED_BOOK) && itemEnchantments.isEmpty()) stripped = stripped.transmuteCopy(Items.BOOK);

                this.resultSlot.setItem(0, stripped);
            }
            //Deconstruct gear
            else {
                this.resultSlot.setItem(0, getDeconstructionResult(input));
            }
        }
        this.broadcastChanges();
    }

    private ItemStack getDeconstructionResult(ItemStack input) {
        Item item = input.getItem();
        Item material = null;
        int maxAmount = 0;

        //Wood
        if (item == Items.WOODEN_SWORD || item == Items.WOODEN_HOE) { material = Items.OAK_PLANKS; maxAmount = 2; }
        else if (item == Items.WOODEN_SHOVEL || item == Items.WOODEN_SPEAR) { material = Items.OAK_PLANKS; maxAmount = 1; }
        else if (item == Items.WOODEN_PICKAXE || item == Items.WOODEN_AXE) { material = Items.OAK_PLANKS; maxAmount = 3; }

        //Leather
        else if (item == Items.LEATHER_HELMET) { material = Items.LEATHER; maxAmount = 5; }
        else if (item == Items.LEATHER_CHESTPLATE) { material = Items.LEATHER; maxAmount = 8; }
        else if (item == Items.LEATHER_LEGGINGS) { material = Items.LEATHER; maxAmount = 7; }
        else if (item == Items.LEATHER_BOOTS) { material = Items.LEATHER; maxAmount = 4; }

        //Stone
        else if (item == Items.STONE_SWORD || item == Items.STONE_HOE) { material = Items.STONE; maxAmount = 2; }
        else if (item == Items.STONE_SHOVEL || item == Items.STONE_SPEAR) { material = Items.STONE; maxAmount = 1; }
        else if (item == Items.STONE_PICKAXE || item == Items.STONE_AXE) { material = Items.STONE; maxAmount = 3; }

        //Diamond
        else if (item == Items.DIAMOND_SWORD || item == Items.DIAMOND_HOE) { material = Items.DIAMOND; maxAmount = 2; }
        else if (item == Items.DIAMOND_SHOVEL || item == Items.DIAMOND_SPEAR) { material = Items.DIAMOND; maxAmount = 1; }
        else if (item == Items.DIAMOND_PICKAXE || item == Items.DIAMOND_AXE) { material = Items.DIAMOND; maxAmount = 3; }
        else if (item == Items.DIAMOND_HELMET) { material = Items.DIAMOND; maxAmount = 5; }
        else if (item == Items.DIAMOND_CHESTPLATE) { material = Items.DIAMOND; maxAmount = 8; }
        else if (item == Items.DIAMOND_LEGGINGS) { material = Items.DIAMOND; maxAmount = 7; }
        else if (item == Items.DIAMOND_BOOTS) { material = Items.DIAMOND; maxAmount = 4; }

        //Netherite
        else if (item == Items.NETHERITE_SWORD || item == Items.NETHERITE_HOE || item == Items.NETHERITE_SHOVEL || item == Items.NETHERITE_SPEAR || item == Items.NETHERITE_PICKAXE || item == Items.NETHERITE_AXE ||
                item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) {
            material = Items.NETHERITE_INGOT; maxAmount = 1;
        }

        //Turtle helmet
        else if (item == Items.TURTLE_HELMET) { material = Items.TURTLE_SCUTE; maxAmount = 5; }

        //Ensure item can be deconstructed
        if (material != null) {
            int finalAmount = getFinalAmount(input, maxAmount);
            //Return deconstructed material
            return new ItemStack(material, finalAmount);
        }

        //Return nothing if invalid
        return ItemStack.EMPTY;
    }

    /**
     * Helper method to determine final material amount to return.
     * Depends on gear durability.
     */
    private static int getFinalAmount(ItemStack input, int maxAmount) {
        //Return max material amount if item is full durability
        if (input.getDamageValue() == 0) return maxAmount;

        //Return 1 material if item only yields 1 material
        if (maxAmount <= 1) return 1;

        //Determine gear health ratio
        float healthRatio = (float) (input.getMaxDamage() - input.getDamageValue()) / input.getMaxDamage();

        //Divide the damaged space into strict brackets
        int finalAmount = (int) Math.ceil(healthRatio * (maxAmount - 1));

        //Return material depending on health ratio (always return at least 1 material)
        return Math.max(1, finalAmount);
    }

    private int getExperienceFromItem(ItemStack itemStack) {
        int xpToDrop = 0;
        ItemEnchantments itemEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(itemStack);

        for (Object2IntMap.Entry<Holder<@NotNull Enchantment>> entry : itemEnchantments.entrySet()) {
            Holder<@NotNull Enchantment> holder = entry.getKey();
            int level = entry.getIntValue();
            if (!holder.is(EnchantmentTags.CURSE)) xpToDrop += level * 45;
        }
        if (xpToDrop > 0) {
            int j = (int)Math.ceil(xpToDrop / 2.0);
            return j + this.access.evaluate((level, pos) -> level.random.nextInt(j)).orElse(0);
        }
        return 0;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(this.access, player, Blocks.GRINDSTONE);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot != null && slot.hasItem()) {
            ItemStack slotItem = slot.getItem();
            itemStack = slotItem.copy();

            //Output slot
            if (i == 1) {
                if (!this.moveItemStackTo(slotItem, 2, 38, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(slotItem, itemStack);
            }
            //Input slot
            else if (i == 0) {
                if (!this.moveItemStackTo(slotItem, 2, 38, false)) return ItemStack.EMPTY;
            }
            //Inventory slots
            else {
                if (!this.moveItemStackTo(slotItem, 0, 1, false)) return ItemStack.EMPTY;
            }

            if (slotItem.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (slotItem.getCount() == itemStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, slotItem);
        }
        return itemStack;
    }
}
