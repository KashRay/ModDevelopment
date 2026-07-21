package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class VillagerMixin {
    /**
     * Inject code after calculating discount prices, reducing cost if within range of a beacon with an upgrade reducing trade costs.
     */
    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void applyBeaconDiscount(Player player, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        Villager villager = (Villager)(Object)this;

        //Check if villager within range of a beacon with the villager cost upgrade
        if (BeaconTracker.hasUpgrade(villager.level(), villager.blockPosition(), "villager_deals")) {
            //Loop through all villager trades
            for (MerchantOffer offer : villager.getOffers()) {
                //Apply a 20% discount to the primary ingredient cost
                double discountMultiplier = 0.2;
                int originalCost = offer.getBaseCostA().getCount();

                //Calculate the discount (ensuring it takes off at least 1 item if possible)
                int discountAmount = Math.max(1, (int) Math.floor(originalCost * discountMultiplier));

                //Subtract discount from vanilla calculated cost
                int newPriceDiff = offer.getSpecialPriceDiff() - discountAmount;
                offer.setSpecialPriceDiff(newPriceDiff);
            }
        }
    }
}
