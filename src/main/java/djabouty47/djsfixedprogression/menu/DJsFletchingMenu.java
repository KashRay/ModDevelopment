package djabouty47.djsfixedprogression.menu;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class DJsFletchingMenu extends ItemCombinerMenu {
    public DJsFletchingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public DJsFletchingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(DJsFixedProgression.DJS_FLETCHING_TABLE_MENU, containerId, playerInventory, access, createInputSlotDefinitions());
    }

    private static ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
        return ItemCombinerMenuSlotDefinition.create()
                .withSlot(0, 53, 34, stack -> stack.is(Items.ARROW)) //Slot 0: base arrows only
                .withSlot(1, 93, 34, stack -> stack.is(Items.LINGERING_POTION)) //Slot 1: lingering potion only
                .withResultSlot(2, 141, 34) // Slot 2: output
                .build();
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(Blocks.FLETCHING_TABLE);
    }

    @Override
    protected void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        //Shrink the input slots (consumes 4 arrows and 1 lingering potion)
        this.inputSlots.getItem(0).shrink(4);
        this.inputSlots.getItem(1).shrink(1);

        //Play fletching villager work sound since table has no native sound
        this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.VILLAGER_WORK_FLETCHER, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.1F + 0.9F));
    }

    @Override
    public void createResult() {
        ItemStack arrowStack = this.getSlot(0).getItem();
        ItemStack modifierStack = this.getSlot(1).getItem();

        //Require at least 4 arrows and a lingering potion to proceed
        if (arrowStack.is(Items.ARROW) && arrowStack.getCount() >= 4 && modifierStack.is(Items.LINGERING_POTION)) {
            ItemStack result = new ItemStack(Items.TIPPED_ARROW, 4);

            //Copy exact potion effects over to new arrows
            PotionContents contents = modifierStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            result.set(DataComponents.POTION_CONTENTS, contents);

            //Return results
            this.resultSlots.setItem(0, result);
            this.broadcastChanges();
            return;
        }

        //If conditions aren't met, display nothing
        this.resultSlots.setItem(0, ItemStack.EMPTY);
        this.broadcastChanges();
    }
}
