package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HappyGhast.class)
public abstract class HappyGhastMixin extends Animal {

    protected HappyGhastMixin(EntityType<? extends @NotNull Animal> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Modify the speed parameter passed into travelFlying to account for potion effects.
     */
    @ModifyArg(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/happyghast/HappyGhast;travelFlying(Lnet/minecraft/world/phys/Vec3;FFF)V"), index = 1)
    private float modifyFlyingSpeed(float originalSpeed) {
        //Check if happy ghast changes are enabled in configs
        if (!DJsConfig.getInstance().enableHappyGhastChanges) return originalSpeed;

        //Adjust speed based on movement-speed-affecting attributes
        double speedMultiplier = this.getAttributeValue(Attributes.MOVEMENT_SPEED) / this.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        return (float) (this.getAttributeValue(Attributes.FLYING_SPEED) * speedMultiplier) * 5.0F / 3.0F;
    }

    /**
     * Inject code when ridden by a player to apply the same speed modifiers (potions).
     */
    @Inject(method = "getRiddenInput", at = @At("RETURN"), cancellable = true)
    private void applyPotionSpeedToRidden(Player player, Vec3 vec3, CallbackInfoReturnable<Vec3> cir) {
        //Check if happy ghast changes are enabled in configs
        if (!DJsConfig.getInstance().enableHappyGhastChanges) return;

        //Calculate movement speed multiplier
        double speedMultiplier = this.getAttributeValue(Attributes.MOVEMENT_SPEED) / this.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        cir.setReturnValue(cir.getReturnValue().scale(speedMultiplier));
    }

    /**
     * Inject code at the end of each tick, forcing happy ghast to algin to the 1.0 block grid when a player stands on its roof.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void alignToGridWhenStoodOn(CallbackInfo ci) {
        //Check if happy ghast changes are enabled in configs
        if (!DJsConfig.getInstance().enableHappyGhastChanges) return;

        //Ignore if the ghast is actively being ridden
        if (this.isVehicle()) return;

        //Create a thin scanning box just above the ghast's roof
        AABB roofBox = this.getBoundingBox().move(0, 0.2, 0);
        Player riderOnRoof = null;

        //Check if player is physically standing on top of the ghast
        for (Entity entity : this.level().getEntities(this, roofBox)) {
            //Ensure they are actually on the roof, not just jumping nearby
            if (entity instanceof  Player player && player.getY() >= this.getY() + this.getBbHeight() - 0.5) {
                riderOnRoof = player;
                break;
            }
        }

        //If there is a player standing on top of the ghast
        if (riderOnRoof != null) {
            //Calculate perfectly centered integers (block corners)
            double targetX = Math.round(this.getX());
            double targetY = Math.round(this.getY());
            double targetZ = Math.round(this.getZ());

            //Smoothly pull ghast to perfect grid alignment
            double dx = targetX - this.getX();
            double dy = targetY - this.getY();
            double dz = targetZ - this.getZ();

            //Check if hte happy ghast is misaligned at all
            if (Math.abs(dx) > 0.001 || Math.abs(dy) > 0.001 || Math.abs(dz) > 0.001) {
                //Instantly move ghast to perfect grid coordinate
                this.setPos(targetX, targetY, targetZ);

                //Instantly teleport the player by the exact same offset to prevent falling through
                riderOnRoof.setPos(riderOnRoof.getX() + dx, riderOnRoof.getY() + dy, riderOnRoof.getZ() + dz);

                //Kill momentum
                this.setDeltaMovement(0, 0, 0);
            }
        }
    }
}
