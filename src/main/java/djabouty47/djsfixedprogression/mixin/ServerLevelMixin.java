package djabouty47.djsfixedprogression.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Shadow @Final private ServerLevelData serverLevelData;
    @Shadow public abstract void setDayTime(long l);

    /**
     * Inject code when initializing the server, overwriting the starting time of newly generated worlds.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void startWorldLateInDay(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;

        //Ensure world is brand new and in the overworld
        if (level.dimension() == Level.OVERWORLD && this.serverLevelData.getGameTime() == 0L) this.setDayTime(7000L);
    }
}
