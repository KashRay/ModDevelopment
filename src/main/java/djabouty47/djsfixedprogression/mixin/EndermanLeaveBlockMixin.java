package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal")
public class EndermanLeaveBlockMixin {
    @Unique private EnderMan djs$enderman;

    /**
     * Work around for getting enderman object.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void captureEnderman(EnderMan enderman, CallbackInfo ci) {
        this.djs$enderman = enderman;
    }

    /**
     * Inject code when an enderman tries to place a block, preventing it if within range of a beacon with an active ability preventing mob griefing.
     */
    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void protectBlocks(CallbackInfoReturnable<Boolean> cir) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //If enderman is inside an active beacon radius, force the enderman to ignore protected blocks
        if (this.djs$enderman != null && BeaconTracker.isAbilityActive(this.djs$enderman.level(), this.djs$enderman.blockPosition(), "mob_griefing")) cir.setReturnValue(false);
    }
}
