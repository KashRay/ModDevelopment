package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.ILinkableMinecart;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartFurnace.class)
public abstract class MinecartFurnaceMixin implements ILinkableMinecart {
    @Shadow public Vec3 push;
    @Shadow private int fuel;

    @Unique private boolean djs$paused = false;

    @Override public boolean djs$isFurnace() { return true; }
    @Override public boolean djs$isFurnaceFueled() { return this.fuel > 0; }
    @Override public Vec3 djs$getFurnaceDirection() { return new Vec3(this.push.x, 0, this.push.z); }
    @Override public void djs$setFurnaceDirection(Vec3 dir) { this.push = dir; }
    @Override public void djs$setFurnacePaused(boolean paused) { this.djs$paused = paused; }

    @Override
    public void djs$extinguish() {
        //Clear fuel and movement when water is dispensed
        this.fuel = 0;
        this.push = Vec3.ZERO;
        ((MinecartFurnace)(Object)this).setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void djs$addFuel(Vec3 direction) {
        //Add fuel and movement when fuel is dispensed
        this.fuel += 3600;
        this.push = direction;
    }

    /**
     * Inject code at the start of every tick, stopping furnace minecart fuel drain if paused.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void handleTrainPause(CallbackInfo ci) {
        //Check if minecart changes are enabled in configs
        if (!DJsConfig.getInstance().enableMinecartChanges) return;
        //Check if the fueled furnace minecart is braking
        if (this.djs$paused && this.fuel > 0) {
            //Counteract the fuel drain while paused
            this.fuel++;

            //Zero out movement
            ((MinecartFurnace)(Object)this).setDeltaMovement(Vec3.ZERO);
        }

    }

    /**
     * Inject code at the end of every tick, executing custom physics engine after vanilla calculates its push.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void enforceCustomPhysics(CallbackInfo ci) {
        //Check if minecart changes are enabled in configs
        if (DJsConfig.getInstance().enableMinecartChanges) this.djs$tickCustomPhysics();
    }

    /**
     * Inject code when interacting with furnace minecarts, adding additional item interaction logic.
     */
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onSpecialItemInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        //Check if minecart changes are enabled in configs
        if (!DJsConfig.getInstance().enableMinecartChanges) return;

        //Get the item in the player's hand
        ItemStack itemStack = player.getItemInHand(hand);

        //If the player is using water on the furnace minecart, drain all fuel and stop the minecart's velocity
        if (itemStack.is(Items.WATER_BUCKET) && this.fuel > 0) {
            if (!player.level().isClientSide()) {
                this.fuel = 0;
                this.push = Vec3.ZERO;

                //Remove water from bucked when consumed (unless in creative)
                if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));

                //Play extinguish fire sound when furnace minecart is put out
                player.level().playSound(null, ((MinecartFurnace) (Object) this).blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}