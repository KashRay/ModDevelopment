package djabouty47.djsfixedprogression.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudMixin extends Entity {
    @Shadow private PotionContents potionContents;

    public AreaEffectCloudMixin(EntityType<? extends @NotNull AreaEffectCloud> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Inject code at the end of each tick, extinguishing entities and blocks inside lingering water cloud.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void extinguishIfWater(CallbackInfo ci) {
        if (!this.level().isClientSide() && this.potionContents != null) {
            //Access potion data component directly from entity
            if (this.potionContents.is(Potions.WATER)) {
                AABB aabb = this.getBoundingBox();

                //Check all entities inside water cloud
                for (Entity entity : this.level().getEntities(this, aabb)) {
                    //Extinguish entities on fire
                    entity.clearFire();

                    //Damage water-sensitive mobs
                    if (entity instanceof EnderMan || entity instanceof Blaze) entity.hurtServer((ServerLevel) this.level(), this.damageSources().drown(), 1.0F);
                }

                //Check all blocks inside cloud radius
                BlockPos min = new BlockPos(Mth.floor(aabb.minX), Mth.floor(aabb.minY), Mth.floor(aabb.minZ));
                BlockPos max = new BlockPos(Mth.ceil(aabb.maxX), Mth.ceil(aabb.maxY), Mth.ceil(aabb.maxZ));

                BlockPos.betweenClosedStream(min, max).forEach((pos) -> {
                    BlockState state = this.level().getBlockState(pos);

                    //Extinguish standard fires
                    if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) this.level().removeBlock(pos, false);
                    //Extinguish lit campfires
                    else if (CampfireBlock.isLitCampfire(state)) {
                        CampfireBlock.dowse(null, this.level(), pos, state);
                        this.level().setBlock(pos, state.setValue(CampfireBlock.LIT, false), 3);
                    }
                });
            }
        }
    }
}
