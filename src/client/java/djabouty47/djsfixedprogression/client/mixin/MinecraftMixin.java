package djabouty47.djsfixedprogression.client.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow public LocalPlayer player;
    @Shadow public HitResult hitResult;
    @Final @Shadow public Options options;
    @Shadow protected abstract boolean startAttack();

    /**
     * Inject directly into method that checks if keys are being pressed.
     */
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void onHandleKeybinds(CallbackInfo ci) {
        //Check if combat changes are enabled in configs
        if (DJsConfig.getInstance().enableCombatChanges && this.options != null && this.player != null) {
            //Check if the attack button is being held down
            if (this.options.keyAttack.isDown()) {
                //If target is a block, do not interrupt mining
                if (this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) return;

                //Only attack when the cooldown is 100%
                if (this.player.getAttackStrengthScale(0.0F) >= 1.0F) this.startAttack();
            }
        }
    }

    /**
     * Inject code at the start of attacking, blocking the client animation if the weapon is on cooldown.
     */
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void preventEarlySwing(CallbackInfoReturnable<Boolean> cir) {
        //Check if combat changes are enabled in configs
        if (DJsConfig.getInstance().enableCombatChanges && this.player != null) {
            //If player is looking at a block, allow swinging to mine
            if (this.hitResult != null && this.hitResult.getType() == HitResult.Type.BLOCK) return;

            //If player is looking at an entity or empty air, and the weapon is on cooldown, cancel the network packet
            if (this.player.getAttackStrengthScale(0.0F) < 1.0F) cir.setReturnValue(false);
        }
    }
}