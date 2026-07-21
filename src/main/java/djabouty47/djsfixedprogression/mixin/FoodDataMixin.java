package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow private int tickTimer;
    @Shadow public abstract float getSaturationLevel();
    @Shadow public abstract int getFoodLevel();
    @Unique private float djs$lastSyncedSaturation = -1.0F;

    /**
     * Modify food level required for fast saturation healing. Set to 999 to completely disable the fast healing mechanic.
     */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 20))
    private int disableFastHealing(int original) {
        //Check if hunger and regeneration changes are enabled in configs
        return DJsConfig.getInstance().enableHungerAndRegenerationChanges ? 999 : original;
    }

    /**
     * Modify the food level required for normal regeneration. Lower to 7 (3.5 drumsticks) so players heal even when most hungry.
     */
    @ModifyConstant(method = "tick", constant = @Constant(intValue = 18))
    private int lowerRegenerationThreshold(int original) {
        //Check if hunger and regeneration changes are enabled in configs
        return DJsConfig.getInstance().enableHungerAndRegenerationChanges ? 7 : original;
    }

    /**
     * Modify amount of exhaustion added when normal regeneration triggers.Lower to 4.0F so healing is less punishing on hunger.
     */
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 6.0F))
    private float reduceHealingExhaustion(float original) {
        //Check if hunger and regeneration changes are enabled in configs
        return DJsConfig.getInstance().enableHungerAndRegenerationChanges ? 4.0F : original;
    }

    /**
     * Inject code at the start of each tick, pausing health regen for 8 seconds after taking damage.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void pauseHealing(ServerPlayer serverPlayer, CallbackInfo ci) {
        //Check if hunger and regeneration changes are enabled in configs
        if (!DJsConfig.getInstance().enableHungerAndRegenerationChanges) return;

        //Prevent internal healing clock from advancing until combat cooldown ends
        Integer cooldown = DJsFixedProgression.REGEN_COOLDOWNS.get(serverPlayer);
        if (cooldown != null && cooldown > 0) this.tickTimer = 0;
    }

    /**
     * Modify saturation calculation, applying dynamic palette multiplier.
     */
    @ModifyVariable(method = "add(IF)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float applySaturationMultiplier(float incomingSaturation) {
        //Check if hunger and regeneration changes are enabled in configs
        return DJsConfig.getInstance().enableHungerAndRegenerationChanges ? incomingSaturation * DJsFixedProgression.currentSaturationMultiplier : incomingSaturation;
    }

    /**
     * Modify saturation calculation, removing hunger saturation clamp.
     */
    @ModifyArg(method = "add(IF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(FFF)F"), index = 2)
    private float uncapSaturation(float originalMax) {
        // Check if hunger and regeneration changes are enabled in configs
        return DJsConfig.getInstance().enableHungerAndRegenerationChanges ? 20.0F : originalMax;
    }

    /**
     * Inject code at the end of each tick, manually syncing saturation to the client if it changes.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void syncSaturationSmoothly(ServerPlayer serverPlayer, CallbackInfo ci) {
        //Check if hunger and regeneration changes are enabled in configs
        if (!DJsConfig.getInstance().enableHungerAndRegenerationChanges) return;

        //Get current saturation
        float currentSaturation = this.getSaturationLevel();

        //Send a health update packet anytime the saturation drops
        if (currentSaturation != this.djs$lastSyncedSaturation) {
            serverPlayer.connection.send(new ClientboundSetHealthPacket(serverPlayer.getHealth(), this.getFoodLevel(), currentSaturation));
            this.djs$lastSyncedSaturation = currentSaturation;
        }
    }
}
