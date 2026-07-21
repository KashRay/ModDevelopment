package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VibrationSystem.Listener.class)
public class VibrationListenerMixin {
    /**
     * Inject code to prevent all vibration listeners from being notified of player sounds if within range of a beacon with an ability preventing sculk detection.
     */
    @Inject(method = "handleGameEvent", at = @At("HEAD"), cancellable = true)
    private void deafenSculk(ServerLevel serverLevel, Holder<@NotNull GameEvent> holder, GameEvent.Context context, Vec3 vec3, CallbackInfoReturnable<Boolean> cir) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //Verify vibration was caused by a player
        if (context.sourceEntity() instanceof Player player) {
            //Cancel vibration if the player is within the radius of a beacon with the sculk detection ability
            if (BeaconTracker.isAbilityActive(serverLevel, player.blockPosition(), "disable_sculk")) cir.setReturnValue(false);
        }
    }
}
