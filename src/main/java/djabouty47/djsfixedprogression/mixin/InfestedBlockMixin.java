package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.InfestedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InfestedBlock.class)
public class InfestedBlockMixin {
    /**
     * Inject code when attempting to spawn a silverfish from mining an infested block, dropping the infested block if within range of a beacon with an upgrade allowing infested block mining.
     */
    @Inject(method = "spawnInfestation", at = @At("HEAD"), cancellable = true)
    private void preventSilverfish(ServerLevel serverLevel, BlockPos blockPos, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //Check if within range of a beacon with an upgrade allowing infested block mining
        if (BeaconTracker.hasUpgrade(serverLevel, blockPos, "mine_infested")) {
            //Pop the infested block
            Block.popResource(serverLevel, blockPos, new ItemStack((InfestedBlock)(Object)this));

            //Abort the silverfish spawn
            ci.cancel();
        }
    }
}
