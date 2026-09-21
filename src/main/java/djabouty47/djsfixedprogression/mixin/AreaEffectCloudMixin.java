package djabouty47.djsfixedprogression.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudMixin extends Entity {
    public AreaEffectCloudMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow public abstract float getRadius();
    @Shadow public abstract Entity getOwner();

    /**
     * Inject method at the end of each tick, extinguishing fires and damaging water-sensitive mobs within lingering water range.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickWaterCloud(CallbackInfo ci) {
        if (!this.level().isClientSide() && this.getTags().contains("djs_water_cloud")) {
            //Process every 5 ticks to save server performance
            if (this.tickCount % 5 == 0) {
                float radius = this.getRadius();
                AABB bounds = this.getBoundingBox();

                //Damage water-sensitive mobs continuously
                List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, bounds);
                for (LivingEntity entity : entities) {
                    if (entity.isSensitiveToWater()) entity.hurtServer((ServerLevel) this.level(), this.damageSources().indirectMagic(this, this.getOwner()), 2.0F); // 1 heart
                }

                //Extinguish fires in radius continuously
                BlockPos center = this.blockPosition();
                int r = (int) Math.ceil(radius);

                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
                    if (pos.distToCenterSqr(this.position()) <= radius * radius) {
                        BlockState state = this.level().getBlockState(pos);

                        //Extinguish fires
                        if (state.getBlock() instanceof BaseFireBlock) this.level().removeBlock(pos, false);
                        //Extinguish campfires
                        else if (state.is(BlockTags.CAMPFIRES) && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) this.level().setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 3);
                    }
                }
            }
        }
    }
}
