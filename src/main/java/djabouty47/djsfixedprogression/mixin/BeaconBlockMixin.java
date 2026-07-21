package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.menu.DJsBeaconMenu;
import djabouty47.djsfixedprogression.network.BeaconSyncPayload;
import djabouty47.djsfixedprogression.procedures_and_util.IUpgradableBeacon;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeaconBlock.class)
public abstract class BeaconBlockMixin {
    /**
     * Inject code when player interacts with beacon, opening custom UI and cancelling vanilla UI.
     */
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void openCustomUI(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        //Check if beacon changes are enabled in configs
        if (DJsConfig.getInstance().enableBeaconChanges) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                //Open custom menu provider instead of vanilla one
                player.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, p) -> new DJsBeaconMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
                        Component.translatable("container.beacon")
                ));
                player.awardStat(Stats.INTERACT_WITH_BEACON);

                //Create a copy of the block's tag
                CompoundTag tag = new CompoundTag();
                if (level.getBlockEntity(pos) instanceof IUpgradableBeacon be) tag = be.djs$getUpgrades().copy();

                //Inject the level
                tag.putInt("djs_beacon_level", IUpgradableBeacon.getBeaconLevel(level, pos));

                //Inject the beacon coordinates
                tag.putInt("beacon_x", pos.getX());
                tag.putInt("beacon_z", pos.getZ());

                //Send the sync packet
                ServerPlayNetworking.send(serverPlayer, new BeaconSyncPayload(tag));
            }
            //Consume click to prevent vanilla from running
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
