package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Pig.class)
public abstract class PigMixin extends Animal {
    @Unique private int djs$sniffCooldown = 400 + this.random.nextInt(600);
    @Unique private int djs$diggingTimer = 0;

    protected PigMixin(EntityType<? extends @NotNull Animal> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Inject code at the end of each tick a pig is being ridden, stopping the pig to occasionally dig up items.
     */
    @Inject(method = "tickRidden", at = @At("TAIL"))
    private void sniffForLoot(Player player, Vec3 input, CallbackInfo ci) {
        //Check if pig and sniffer changes are enabled in configs
        if (!DJsConfig.getInstance().enablePigAndSnifferChanges) return;

        //Ensure player is holding a carrot on the stick
        if (!level().isClientSide() && player.isHolding(Items.CARROT_ON_A_STICK)) {
            //If the pig is currently digging
            if (this.djs$diggingTimer > 0) {
                this.djs$diggingTimer--;

                if (this.level() instanceof ServerLevel serverLevel) {
                    //Kick up dirt particles and play digging sound
                    if (this.djs$diggingTimer % 5 == 0) {
                        BlockPos floorPos = this.blockPosition().below();
                        BlockState floorState = this.level().getBlockState(floorPos);
                        serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floorState), this.getX(), this.getY() + 0.1, this.getZ(), 3, 0.2, 0.1, 0.2, 0.05);
                        this.playSound(SoundEvents.PIG_STEP, 1.0F, 0.5F);
                    }
                }

                //Finish digging and drop loot
                if (this.djs$diggingTimer == 0) {
                    ItemStack loot = this.djs$getPigLoot(this.getRandom());
                    this.spawnAtLocation((ServerLevel) level(), loot, 0.2F);
                    this.playSound(SoundEvents.PIG_AMBIENT, 1.0F, 1.5F);

                    //Digging is done, restore movement speed
                    Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.25D);
                    this.djs$sniffCooldown = 400 + this.random.nextInt(600);
                }
            }
            //If not digging, progress cooldown
            else {
                this.djs$sniffCooldown--;
                if (this.djs$sniffCooldown <= 0) {
                    //Check block below the pig
                    BlockPos floorPos = this.blockPosition().below();
                    BlockState blockState = this.level().getBlockState(floorPos);

                    //Ensure block below pig is a diggable surface
                    if (blockState.is(BlockTags.SNIFFER_DIGGABLE_BLOCK)) {
                        //Set digging timer to 40 ticks (2 seconds)
                        this.djs$diggingTimer = 40;

                        //Curious snort warning
                        this.playSound(SoundEvents.PIG_AMBIENT, 1.0F, 0.5F);

                        //Stop movement speed when digging
                        Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.0D);
                    }
                    else {
                        //Set digging timer to 20 ticks (1 second)
                        this.djs$diggingTimer = 20;
                    }
                }
            }
        }

        //Check movement speed for digging
        if (this.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) <= 0.01D) {
            //Force head down
            this.setXRot(50.0F);
            this.xRotO = 50.0F;
        }
    }

    /**
     * Inject code ensuring if the player hops off a pig while it is digging, it's movement is unfrozen.
     */
    @Inject(method = "getControllingPassenger", at = @At("RETURN"))
    private void resetPigSpeedOnDismount(CallbackInfoReturnable<LivingEntity> cir) {
        //Check if pig and sniffer changes are enabled in configs
        if (!DJsConfig.getInstance().enablePigAndSnifferChanges) return;

        if (!this.level().isClientSide() && this.djs$diggingTimer > 0) {
            //If the pig has no valid controller (player dismounted or unequipped the carrot)
            if (cir.getReturnValue() == null) {
                this.djs$diggingTimer = 0;
                this.djs$sniffCooldown = 400 + this.random.nextInt(600);
                Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.25D);
            }
        }
    }


    /**
     * Method to compute the weighted random loot table for the pig.
     */
    @Unique
    private ItemStack djs$getPigLoot(RandomSource random) {
        int roll = random.nextInt(100);

        //Ultra rare loot
        if (roll < 2) return new ItemStack(Items.GOLDEN_CARROT);
        if (roll < 3) return new ItemStack(Items.GOLDEN_APPLE);

        //Rare
        if (roll < 7) return new ItemStack(Items.GOLD_NUGGET, random.nextInt(3) + 1);
        if (roll < 9) return new ItemStack(Items.IRON_NUGGET, random.nextInt(3) + 1);
        if (roll < 11) return new ItemStack(Items.COPPER_NUGGET, random.nextInt(3) + 1);
        if (roll < 13) return new ItemStack(Items.CARROT);
        if (roll < 15) return new ItemStack(Items.POTATO);
        if (roll < 17) return new ItemStack(Items.PUMPKIN_SEEDS, random.nextInt(2) + 1);
        if (roll < 19) return new ItemStack(Items.MELON_SEEDS, random.nextInt(2) + 1);
        if (roll < 21) return new ItemStack(Items.BEETROOT_SEEDS, random.nextInt(2) + 1);

        //Common
        if (roll < 30) return new ItemStack(Items.BONE_MEAL, random.nextInt(2) + 1);
        if (roll < 45) return new ItemStack(Items.MUD);
        if (roll < 60) return new ItemStack(Items.LEAF_LITTER, random.nextInt(2) + 1);
        if (roll < 75) return new ItemStack(Items.DIRT);
        if (roll < 87) return new ItemStack(Items.RED_MUSHROOM);
        if (roll < 91) return new ItemStack(Items.BROWN_MUSHROOM);
        return new ItemStack(Items.WHEAT_SEEDS);
    }
}
