package djabouty47.djsfixedprogression.mixin;

import com.mojang.serialization.Codec;
import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.network.FoodHistorySyncPayload;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import djabouty47.djsfixedprogression.procedures_and_util.IFoodHistory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Mixin(Player.class)
public abstract class PlayerMixin implements IFoodHistory {
    @Unique private final Deque<String> djs$foodHistory = new LinkedList<>();
    @Shadow public abstract float getAttackStrengthScale(float baseTime);

    @Override
    public Deque<String> djs$getFoodHistory() { return this.djs$foodHistory; }

    @Override
    public void djs$addFoodToHistory(String itemId) {
        //Add food item to history
        this.djs$foodHistory.addFirst(itemId);

        //Keep only last 10 food items
        if (this.djs$foodHistory.size() > 10) this.djs$foodHistory.removeLast();

        //Sync to client
        if ((Object) this instanceof ServerPlayer serverPlayer) ServerPlayNetworking.send(serverPlayer, new FoodHistorySyncPayload(new ArrayList<>(this.djs$foodHistory)));
    }

    @Override
    public float djs$getCurrentMultiplier(String itemId) {
        int count = 0;

        //Loop over food history
        for (String pastFood : this.djs$foodHistory) {
            //Count how many times food item appears on list
            if (pastFood.equals(itemId)) count++;
        }

        //Return saturation level depending on how many times food was recently consumed
        if (count == 0) return 1.0F; //100% saturation
        if (count == 1) return 0.50F; //50% saturation
        if (count == 2) return 0.25F; //25% saturation
        return 0.0F; //0% saturation
    }

    /**
     * Inject code when calculating if the player has slept long enough, cancelling it immediately to prevent vanilla sleeping logic.
     */
    @Inject(method = "isSleepingLongEnough", at = @At("HEAD"), cancellable = true)
    private void preventVanillaNightSkip(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    /**
     * Inject code after saving data, saving food history data.
     */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveFoodHistory(ValueOutput valueOutput, CallbackInfo ci) {
        //Check if hunger and regeneration changes are enabled in configs
        if (!DJsConfig.getInstance().enableHungerAndRegenerationChanges) return;

        //Save food history data
        valueOutput.store("djs_food_history", Codec.STRING.listOf(), List.copyOf(this.djs$foodHistory));
    }

    /**
     * Inject code after reading saved data, reading food history data.
     */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadFoodHistory(ValueInput valueInput, CallbackInfo ci) {
        //Check if hunger and regeneration changes are enabled in configs
        if (!DJsConfig.getInstance().enableHungerAndRegenerationChanges) return;

        //Load food history data
        this.djs$foodHistory.clear();
        List<String> list = valueInput.read("djs_food_history", Codec.STRING.listOf()).orElse(List.of());
        this.djs$foodHistory.addAll(list);
    }

    /**
     * Inject code when a player tries attacking an entity, preventing it if their weapon is on cooldown.
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void lockAttackUntilCharged(Entity entity, CallbackInfo ci) {
        //Check if combat changes are enabled in configs
        if (DJsConfig.getInstance().enableCombatChanges) {
            //0.5F is the baseline check minecraft uses for combat calculations
            float cooldown = this.getAttackStrengthScale(0.5F);

            //Block the attack if the weapon isn't fully charged
            if (cooldown < 1.0F) ci.cancel();
        }
    }

    /**
     * Inject code when calculating XP needed for the next level, returning a constant amount.
     */
    @Inject(method = "getXpNeededForNextLevel", at = @At("HEAD"), cancellable = true)
    private void makeXPLinear(CallbackInfoReturnable<Integer> cir) {
        //Check if XP changes are enabled in configs
        if (DJsConfig.getInstance().enableXPChanges) {
            //Force every level to require exactly 30XP orbs
            cir.setReturnValue(DJsConfig.getInstance().flatXPPerLevel);
        }
    }

    /**
     * Inject code when calculating player mining speed, doubling deepslate mining speed if within range of a beacon with a deepslate mining upgrade.
     */
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void boostDeepslateMiningSpeed(BlockState blockState, CallbackInfoReturnable<Float> cir) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //Check if block being mined is deepslate
        if (blockState.getBlock().equals(Blocks.DEEPSLATE)) {
            Player player = (Player) (Object) this;

            //If player is within range of a beacon with a deepslate mining upgrade, increase destroy speed
            if (BeaconTracker.hasUpgrade(player.level(), player.blockPosition(), "mine_deepslate")) cir.setReturnValue(cir.getReturnValue() * 2.0F);
        }
    }

    /**
     * Inject code when a player attempts to open their elytra, preventing deployment unless they have fallen a sufficient height.
     */
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void preventElytraBunnyHopping(CallbackInfoReturnable<Boolean> cir) {
        // Check if Elytra changes are enabled in the configs
        if (!DJsConfig.getInstance().enableElytraChanges) return;

        Player player = (Player) (Object) this;

        //Deny elytra deployment if they haven't fallen more than 1 block
        if (player.fallDistance < 1.5F) cir.setReturnValue(false);
    }
}
