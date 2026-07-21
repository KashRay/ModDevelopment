package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.menu.DJsEnchantmentMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantingTableBlock.class)
public class EnchantingTableBlockMixin {
    /**
     * Inject code when player interacts with enchanting table, opening custom UI and cancelling vanilla UI.
     */
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void openOverhauledEnchantingUI(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        //Check if anvil and enchanting changes are enabled in configs
        if (DJsConfig.getInstance().enableAnvilAndEnchantingChanges) {
            if (!level.isClientSide()) {
                //Open custom menu provider instead of vanilla one
                player.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, p) -> new DJsEnchantmentMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
                        Component.translatable("container.enchant")
                ));
            }
            //Consume click to prevent vanilla from running
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
