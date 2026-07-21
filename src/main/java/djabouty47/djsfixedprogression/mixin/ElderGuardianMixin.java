package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.procedures_and_util.ElderGuardianDashGoal;
import djabouty47.djsfixedprogression.procedures_and_util.ElderGuardianTailSlapGoal;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ElderGuardian.class)
public abstract class ElderGuardianMixin extends Guardian {
    @Unique public final TagKey<@NotNull Block> SMASHABLE_TAG = TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:elder_guardian_smashable"));
    @Unique private ServerBossEvent bossEvent;

    protected ElderGuardianMixin(EntityType<? extends @NotNull Guardian> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Inject code when initializing static attributes, bumping health from 80 to 150.
     */
    @Inject(method = "createAttributes", at = @At("HEAD"), cancellable = true)
    private static void customAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        cir.setReturnValue(Guardian.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.3F)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MAX_HEALTH, 150.0D));
    }

    /**
     * Inject code after initialization, adding custom AI goals into goal selector.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addCustomGoals(EntityType<? extends @NotNull ElderGuardian> entityType, Level level, CallbackInfo ci) {
        ElderGuardian self = (ElderGuardian) (Object) this;

        //Initialize boss bar
        this.bossEvent = (ServerBossEvent)(new ServerBossEvent(
                Component.translatable("entity.minecraft.elder_guardian"),
                BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarOverlay.PROGRESS
        )).setDarkenScreen(true);

        //Add attack goals
        this.goalSelector.addGoal(2, new ElderGuardianDashGoal(self, SMASHABLE_TAG));
        this.goalSelector.addGoal(3, new ElderGuardianTailSlapGoal(self));
    }

    /**
     * Inject code at the end of each server tick, updating the boss bar fill percentage.
     */
    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void updateBossBar(ServerLevel serverLevel, CallbackInfo ci) {
        //Update boss bar progress
        if (this.bossEvent != null) this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    /**
     * Add boss bar to players that see the elder guardian.
     */
    @Override
    public void startSeenByPlayer(@NotNull ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.bossEvent != null) this.bossEvent.addPlayer(player);
    }

    /**
     * Remove boss bar from players that leave.
     */
    @Override
    public void stopSeenByPlayer(@NotNull ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (this.bossEvent != null) this.bossEvent.removePlayer(player);
    }
}
