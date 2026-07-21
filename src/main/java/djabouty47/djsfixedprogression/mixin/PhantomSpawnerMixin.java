package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {
    /**
     * Completely disables the Overworld phantom spawner if the config is enabled.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void cancelOverworldPhantoms(ServerLevel serverLevel, boolean bl, CallbackInfo ci) {
        //Check if phantom changes are enabled in configs
        if (!DJsConfig.getInstance().enablePhantomChanges) return;

        //Cancel phantom spawning in the overworld
        ci.cancel();
    }
}
