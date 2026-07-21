package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {
    @Shadow private boolean dealtDamage;

    @Inject(method = "tick", at = @At("HEAD"))
    private void returnFromVoid(CallbackInfo ci) {
        //Check if combat changes are enabled in configs
        if (DJsConfig.getInstance().enableCombatChanges) {
            ThrownTrident trident = (ThrownTrident) (Object) this;

            //Check if the trident falls 15 blocks below the bottom of the world
            if (trident.getY() < trident.level().getMinY() - 15) {
                //Fool the trident into thinking it hit something so loyalty triggers
                this.dealtDamage = true;
            }
        }
    }
}