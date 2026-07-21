package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.IUpgradableBeacon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockMixin {
    /**
     * Inject code when placing beacon, injecting saved NBT upgrades into the newly placed block.
     */
    @Inject(method = "setPlacedBy", at = @At("TAIL"))
    private void readUpgradesFromItem(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //Only trigger logic if server-side and block being placed is a beacon block
        if (!level.isClientSide() && (Object) this instanceof BeaconBlock) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);

            //Check if the item contains our saved upgrade tree
            if (customData.copyTag().contains("djs_upgrades")) {
                BlockEntity be = level.getBlockEntity(pos);

                if (be instanceof IUpgradableBeacon upgradable) {
                    //Inject the tree directly into the block
                    upgradable.djs$setUpgrades(customData.copyTag().getCompoundOrEmpty("djs_upgrades"));
                }
            }
        }
    }

    /**
     * Force waterlogged to default to false across the entire game when blocks register their states.
     */
    @ModifyVariable(method = "registerDefaultState", at = @At("HEAD"), argsOnly = true)
    private BlockState enforceWaterloggedDefault(BlockState state) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) return state.setValue(BlockStateProperties.WATERLOGGED, false);
        return state;
    }
}
