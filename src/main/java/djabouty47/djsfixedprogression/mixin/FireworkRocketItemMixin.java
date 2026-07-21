package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemMixin {

    /**
     * Inject code when using fireworks, damaging elytra's more upon use.
     */
    @Inject(method = "use", at = @At("HEAD"))
    private void penalizeElytraOnBoost(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        //Check if elytra changes are enabled in configs
        if (DJsConfig.getInstance().enableElytraChanges) {
            if (!level.isClientSide() && player.isFallFlying()) {
                ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

                //If the player is wearing an elytra, take off 5 durability points
                if (chest.is(Items.ELYTRA) && player instanceof ServerPlayer serverPlayer)
                    chest.hurtAndBreak(5, serverPlayer, EquipmentSlot.CHEST);
            }
        }
    }
}
