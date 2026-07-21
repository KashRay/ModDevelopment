package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class PreventSpawningMixin {
    /**
     * Inject code when spawning a mob, blocking hostile mob spawns if within range of a beacon with an active ability preventing mob spawning.
     */
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void blockHostileMobSpawns(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //Only block hostile mobs
        if (entity instanceof Enemy) {
            //Instantly delete mobs that try spawning within range of the active beacon with the ability
            if (BeaconTracker.isAbilityActive(entity.level(), entity.blockPosition(), "mob_spawning")) cir.setReturnValue(false);
        }
    }
}
