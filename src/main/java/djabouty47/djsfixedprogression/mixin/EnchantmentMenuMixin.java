package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.access.IEnchantmentMenuExtension;
import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EnchantingTableBlock;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu implements IEnchantmentMenuExtension {
    @Shadow @Final private ContainerLevelAccess access;
    @Unique private final DataSlot maxEnchantmentSlots = DataSlot.standalone();

    protected EnchantmentMenuMixin(MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    /**
     * Inject code at the end of the constructor, registering syncing data slot.
     */
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void onInit(int containerId, Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        //Check if anvil and enchanting changes are enabled in configs
        if (DJsConfig.getInstance().enableAnvilAndEnchantingChanges) {
            this.addDataSlot(this.maxEnchantmentSlots);
            //Default to 1 slot at 0 bookshelves
            this.maxEnchantmentSlots.set(1);

            // Grab the vanilla Lapis slot
            Slot oldLapisSlot = this.slots.get(1);

            // Create a custom slot that accepts our enchanting ingredients
            Slot customIngredientSlot = new Slot(oldLapisSlot.container, oldLapisSlot.getContainerSlot(), oldLapisSlot.x, oldLapisSlot.y) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return isValidIngredient(stack);
                }
            };

            //Ensure the new slot knows it is supposed to be slot 1
            customIngredientSlot.index = oldLapisSlot.index;

            //Swap vanilla swap with custom one
            this.slots.set(1, customIngredientSlot);
        }
    }

    /**
     * Inject code when quick moving, overriding vanilla shift-click logic to allow different ingredients.
     */
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void customMoveQuick(Player player, int i, CallbackInfoReturnable<ItemStack> cir) {
        //Check if anvil and enchanting changes are enabled in configs
        if (!DJsConfig.getInstance().enableAnvilAndEnchantingChanges) return;

        ItemStack itemStack1 = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);

        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack1 = itemStack2.copy();

            //Taking tool out
            if (i == 0) {
                if (!this.moveItemStackTo(itemStack2, 2, 38, true)) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    return;
                }
            }
            //Taking ingredient out
            else if (i == 1) {
                if (!this.moveItemStackTo(itemStack2, 2, 38, true)) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    return;
                }
            }
            //Quick moving a valid resource in
            else if (isValidIngredient(itemStack2)) {
                if (!this.moveItemStackTo(itemStack2, 1, 2, true)) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    return;
                }
            }
            //Quick moving a tool in
            else {
                if (this.slots.getFirst().hasItem() || !this.slots.getFirst().mayPlace(itemStack2)) {
                    cir.setReturnValue(ItemStack.EMPTY);
                    return;
                }
                ItemStack itemStack3 = itemStack2.copyWithCount(1);
                itemStack2.shrink(1);
                this.slots.getFirst().setByPlayer(itemStack3);
            }

            if (itemStack2.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (itemStack2.getCount() == itemStack1.getCount()) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            slot.onTake(player, itemStack2);
        }
        cir.setReturnValue(itemStack1);
    }

    /**
     * Inject code the moment the table updates, checking slots per bookshelf.
     */
    @Inject(method = "slotsChanged", at = @At("RETURN"))
    private void calculateBookshelfSlots(Container container, CallbackInfo ci) {
        //Check if anvil and enchanting changes are enabled in configs
        if (!DJsConfig.getInstance().enableAnvilAndEnchantingChanges) return;

        //Execute code safely in the world
        this.access.execute((level, pos) -> {
            int bookshelves = 0;

            //Loop through 5x5 layout vanilla uses for bookshelves
            for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) bookshelves++;
            }

            //Translate bookshelves into max slots
            int slots = 1;
            if (bookshelves >= 15) slots = 4;
            else if (bookshelves >= 10) slots = 3;
            else if (bookshelves >= 5) slots = 2;

            //Update the data slot (tells the client GUI to update)
            this.maxEnchantmentSlots.set(slots);
        });
    }

    /**
     * Implement custom interface method so client GUI can access data.
     * @return The number of enchantment slots the current enchanting table has
     */
    @Override
    public int djsfixedprogression$getAvailableEnchantmentSlots() {
        return this.maxEnchantmentSlots.get();
    }

    /**
     * Helper method to check valid ingredients for ingredient slot.
     * @param stack The item to be placed in the slot
     * @return True if the item can fit in the slot, false if it can't
     */
    @Unique
    private boolean isValidIngredient(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() > 1;
    }
}
