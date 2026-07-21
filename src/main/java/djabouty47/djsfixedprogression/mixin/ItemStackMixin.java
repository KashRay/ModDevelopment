package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.UpdateCopperArmorStatsProcedure;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import djabouty47.djsfixedprogression.procedures_and_util.UpdateCopperToolStatsProcedure;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    /**
     * Inject at beginning of each inventory tick, checking if copper armor and tool stats match their corresponding stage.
     */
    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void onInventoryTick(Level level, Entity entity, EquipmentSlot equipmentSlot, CallbackInfo ci) {
        //Check if tool and armor rebalancing is enabled in configs
        if (!DJsConfig.getInstance().enableToolAndArmorRebalance) return;

        // Only run on the server
        if (level.isClientSide()) return;

        ItemStack stack = (ItemStack) (Object) this;

        // Run checking only once per second (every 20 ticks)
        if (level.getGameTime() % 20 == 0) {
            if (entity instanceof net.minecraft.world.entity.player.Player player) {

                // Check Tools
                if (stack.is(Items.COPPER_SWORD) || stack.is(Items.COPPER_PICKAXE) || stack.is(Items.COPPER_AXE) || stack.is(Items.COPPER_SHOVEL) || stack.is(Items.COPPER_HOE) || stack.is(Items.COPPER_SPEAR)) {
                    UpdateCopperToolStatsProcedure.execute(player, stack);
                }

                // Check Armor
                else if (stack.is(Items.COPPER_HELMET)) UpdateCopperArmorStatsProcedure.execute(player, stack, net.minecraft.world.entity.EquipmentSlot.HEAD);
                else if (stack.is(Items.COPPER_CHESTPLATE)) UpdateCopperArmorStatsProcedure.execute(player, stack, net.minecraft.world.entity.EquipmentSlot.CHEST);
                else if (stack.is(Items.COPPER_LEGGINGS)) UpdateCopperArmorStatsProcedure.execute(player, stack, net.minecraft.world.entity.EquipmentSlot.LEGS);
                else if (stack.is(Items.COPPER_BOOTS)) UpdateCopperArmorStatsProcedure.execute(player, stack, net.minecraft.world.entity.EquipmentSlot.FEET);
            }
        }
    }

    /**
     * Inject code after tools take damage, checking if copper tool stats match their corresponding stage.
     */
    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", at = @At("RETURN"))
    private void onHurtAndBreak(int amount, ServerLevel level, ServerPlayer player, Consumer<Item> onBreak, CallbackInfo ci) {
        //Check if tool and armor rebalancing is enabled in configs
        if (!DJsConfig.getInstance().enableToolAndArmorRebalance) return;

        ItemStack stack = (ItemStack) (Object) this;

        //Check if item that just took damage is a copper tool
        if (stack.is(Items.COPPER_SWORD) || stack.is(Items.COPPER_PICKAXE) || stack.is(Items.COPPER_AXE) || stack.is(Items.COPPER_SHOVEL) || stack.is(Items.COPPER_HOE) || stack.is(Items.COPPER_SPEAR)) {
            //If a player is holding it, trigger the stat calculation
            if (player != null) {
                UpdateCopperToolStatsProcedure.execute(player, stack);
            }
        }
    }

    /**
     * Inject code after returning consumable use duration, cutting drinking time in half.
     */
    @Inject(method = "getUseDuration", at = @At("RETURN"), cancellable = true)
    private void halvePotionDrinkTime(LivingEntity livingEntity, CallbackInfoReturnable<Integer> cir) {
        //Check if combat changes are enabled in configs
        if (DJsConfig.getInstance().enableCombatChanges) {
            ItemStack stack = (ItemStack) (Object) this;
            if (stack.is(Items.POTION)) cir.setReturnValue(cir.getReturnValue() / 2);
        }
    }
}