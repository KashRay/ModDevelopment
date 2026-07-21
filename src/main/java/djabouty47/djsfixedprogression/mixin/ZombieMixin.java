package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Zombie.class)
public abstract class ZombieMixin extends Monster {
    protected ZombieMixin(EntityType<? extends @NotNull Monster> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Inject code after registering zombie's goals, adding sniffer egg destruction (similar to turtle eggs).
     */
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void addSnifferEggGoal(CallbackInfo ci) {
        //Check if pig and sniffer changes are enabled in configs
        if (!DJsConfig.getInstance().enablePigAndSnifferChanges) return;

        //Make zombie search out sniffer eggs, similarly to turtle eggs
        this.goalSelector.addGoal(4, new RemoveBlockGoal(Blocks.SNIFFER_EGG, (Zombie)(Object) this, 1.0D, 3));
    }
}
