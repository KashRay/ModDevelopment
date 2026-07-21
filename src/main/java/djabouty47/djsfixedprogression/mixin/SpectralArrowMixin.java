package djabouty47.djsfixedprogression.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

@Mixin(SpectralArrow.class)
public abstract class SpectralArrowMixin extends AbstractArrow {
    //Safely track the target each player is currently locked onto
    @Unique private static final WeakHashMap<Entity, LivingEntity> LOCKED_TARGETS = new WeakHashMap<>();

    //Track within 10 second lock
    @Unique private static final WeakHashMap<Entity, Long> TARGET_TIMEOUTS = new WeakHashMap<>();

    //Ensure arrow is only slowed down once upon firing
    @Unique private boolean hasBeenSlowed = false;

    protected SpectralArrowMixin(net.minecraft.world.entity.EntityType<? extends @NotNull AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * When a spectral arrow hits a target, lock onto it.
     */
    @Redirect(method = "doPostHurtEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean applyLockOn(LivingEntity target, MobEffectInstance effect, Entity effectSource) {
        //Get the owner of the arrow
        Entity owner = this.getOwner();

        //Prevent locking onto yourself, and ensure the owner exists
        if (owner != null && !target.equals(owner)) {
            //Lock onto this specific target
            LOCKED_TARGETS.put(owner, target);

            //Set expiration for 10 seconds (200 ticks) from now
            TARGET_TIMEOUTS.put(owner, this.level().getGameTime() + 200L);
        }

        //Completely erase the vanilla glowing visual effect
        return false;
    }

    /**
     * Inject code at the start of each tick, adding homing and slower flight logic.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void applyHomingLogic(CallbackInfo ci) {
        //Stop tracking if the arrow hits the ground
        if (this.isInGround()) return;

        //Slow down the arrow the moment it spawns
        if (!this.hasBeenSlowed && this.tickCount > 1) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            this.hasBeenSlowed = true;
        }

        //Get the owner of the arrow
        Entity owner = this.getOwner();
        if (owner == null) return;

        //Verify the player has a locked target that is still alive
        LivingEntity target = LOCKED_TARGETS.get(owner);
        if (target == null || !target.isAlive() || target.isSpectator()) return;

        //Verify lock on hasn't expired
        Long timeout = TARGET_TIMEOUTS.get(owner);
        if (timeout == null || this.level().getGameTime() > timeout) {
            LOCKED_TARGETS.remove(owner);
            return;
        }

        //Get the arrows current movement
        Vec3 currentMovement = this.getDeltaMovement();

        //Aim for the center of the entity's body
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);
        Vec3 toTarget = targetCenter.subtract(this.position()).normalize();

        //Get the speed of the arrow
        double speed = currentMovement.length();

        //Set the smoothness of the homing (curve)
        double turnSpeed = 0.12;

        //Blend current trajectory with target direction
        Vec3 newMovement = currentMovement.normalize().scale(1.0 - turnSpeed).add(toTarget.scale(turnSpeed)).normalize().scale(speed);

        //Counteract vanilla arrow gravity
        newMovement = newMovement.add(0, 0.05, 0);

        this.setDeltaMovement(newMovement);

        //Tell the server to constantly stream this new trajectory to the client
        if (!this.level().isClientSide() && this.tickCount % 2 == 0) this.needsSync = true;
    }
}
