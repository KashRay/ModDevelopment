package djabouty47.djsfixedprogression.procedures_and_util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface IUpgradableBeacon {
    CompoundTag djs$getUpgrades();
    void djs$setUpgrades(CompoundTag tag);

    /**
     * Helper method for calculation of active pyramid layers.
     */
    static int getBeaconLevel(Level level, BlockPos pos) {
        int customLevels = 0;

        //Select a target block for comparison
        Block targetBlock = level.getBlockState(pos.below()).getBlock();

        //Ensure target block is a valid beacon block
        if (!level.getBlockState(pos.below()).is(BlockTags.BEACON_BASE_BLOCKS)) return 0;

        //Loop over all pyramid layers
        for (int yOffset = 1; yOffset <= 4; yOffset++) {
            int y = pos.getY() - yOffset;
            if (y < level.getMinY()) break;

            boolean layerValid = true;

            //Loop over all blocks in the pyramid layer
            for (int x = pos.getX() - yOffset; x <= pos.getX() + yOffset && layerValid; x++) {
                for (int z = pos.getZ() - yOffset; z <= pos.getZ() + yOffset; z++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));

                    //Ensure block is a valid beacon block
                    if (!state.is(BlockTags.BEACON_BASE_BLOCKS)) {
                        layerValid = false;
                        break;
                    }

                    //If the target block is a copper block
                    if (targetBlock.defaultBlockState().is(BlockTags.COPPER)) {
                        //Ensure blocks are a variant of the copper block
                        if (!state.getBlock().defaultBlockState().is(BlockTags.COPPER)) layerValid = false;
                    }
                    //Ensure non-copper blocks match target block exactly
                    else if (!state.getBlock().equals(targetBlock)) layerValid = false;
                }
            }

            //Stop checking lower layers if current layer is incomplete
            if (!layerValid) break;

            //If all blocks of the layer are valid, then add to the valid layer count
            customLevels++;
        }
        return customLevels;
    }
}