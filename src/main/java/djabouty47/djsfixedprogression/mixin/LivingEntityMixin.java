package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow public abstract boolean isUsingItem();
    @Shadow protected ItemStack useItem;
    @Shadow public abstract int getTicksUsingItem();
    @Shadow private @Nullable EntityReference<@NotNull LivingEntity> lastHurtByMob;
    @Shadow private int lastHurtByMobTimestamp;
    @Shadow public abstract void stopUsingItem();
    @Shadow @Final protected EntityEquipment equipment;
    @Shadow public abstract boolean isFallFlying();
    @Shadow public abstract ItemStack getItemBySlot(EquipmentSlot slot);

    @Shadow
    public abstract ItemStack getItemInHand(InteractionHand interactionHand);

    @Unique private int updraftTicks = 0;
    @Unique private boolean isStrongUpdraft = false;

    /**
     * Inject when player starts blocking, removing the 5-tick delay
     */
    @Inject(method = "getItemBlockingWith", at = @At("HEAD"), cancellable = true)
    private void makeShieldInstant(CallbackInfoReturnable<ItemStack> cir) {
        //Check if combat changes are enabled in configs
        if (DJsConfig.getInstance().enableCombatChanges && this.isUsingItem()) {
            //Start blocking as soon as player interacts with the shield
            if (!this.useItem.isEmpty() && this.useItem.getItem().equals(Items.SHIELD)) cir.setReturnValue(this.useItem);
        }
    }

    /**
     * Inject code when starting to use a shield, adding a visual cooldown indicator to the hotbar exactly when blocking starts.
     */
    @Inject(method = "startUsingItem", at = @At("TAIL"))
    private void addParryWindowVisual(InteractionHand hand, CallbackInfo ci) {
        //Check if combat changes are enabled in configs
        if (!DJsConfig.getInstance().enableCombatChanges) return;

        //Get the item in the player's hand
        ItemStack item = this.getItemInHand(hand);

        //If the item is a shield, add a cooldown when activated
        if (item.is(Items.SHIELD) && (Object) this instanceof Player player) player.getCooldowns().addCooldown(item, 10);
    }

    /**
     * Inject when blocking attacks, limiting the valid angle of attacks, implementing parrying logic, and implementing thorns enchantment.
     */
    @Inject(method = "applyItemBlocking", at = @At("RETURN"), cancellable = true)
    private void modifyBlockedDamage(ServerLevel world, DamageSource source, float damage, CallbackInfoReturnable<Float> cir) {
        float blockedDamage = cir.getReturnValueF();

        //If vanilla decided to drop some damage, double-check math
        if (blockedDamage > 0.0F) {
            LivingEntity livingEntity = (LivingEntity) (Object) this;

            //Check if combat changes are enabled in configs
            if (DJsConfig.getInstance().enableCombatChanges) {
                Vec3 attackerPosition = source.getSourcePosition();

                //Check if the attack exceeds 100 degrees
                if (attackerPosition != null) {
                    Vec3 lookVector = livingEntity.getViewVector(1.0F);
                    Vec3 attackVector = attackerPosition.subtract(livingEntity.position());
                    attackVector = new Vec3(attackVector.x, 0.0, attackVector.z).normalize();

                    //If dot product is < 0.64 (outside the 100-degree front cone), take full damage
                    if (attackVector.dot(lookVector) < 0.64) {
                        cir.setReturnValue(0.0F);
                        return;
                    }
                }

                //Check if the player used the shield within the parry window
                boolean isParry = livingEntity.getTicksUsingItem() <= 10;

                //Check if the damage source was a projectile
                boolean isProjectile = source.is(DamageTypeTags.IS_PROJECTILE);

                //Fully block projectiles
                if (isProjectile) blockedDamage = damage;
                //If attack was parried successfully
                else if (isParry) {
                    //If damage exceeds parry cap, reduce the remainder of the incoming damage
                    if (damage > 20.0F) blockedDamage = 20.0F + ((damage - 20.0F) * 0.5F);
                    //Otherwise fully block up to 10 hearts of damage
                    else blockedDamage = damage;

                    //Inflict counter-knockback to attackers
                    Entity attacker = source.getDirectEntity();
                    if (attacker instanceof LivingEntity livingAttacker) livingAttacker.knockback(0.6D, livingEntity.getX() - livingAttacker.getX(), livingEntity.getZ() - livingAttacker.getZ());

                    //Play sound for parrying
                    if (!isProjectile) world.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0F, 1.5F);
                }
                //If blocking attack normally, reduce incoming damage
                else blockedDamage = damage * 0.5F;

                //Apply newly calculated blocked amount
                cir.setReturnValue(blockedDamage);
            }

            //Look up thorns enchantment in registry
            var thornsHolder = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.THORNS);

            //Check if shield has thorns attachment
            int thornsLevel = this.useItem.getEnchantments().getLevel(thornsHolder);

            if (thornsLevel > 0) {
                Entity attacker = source.getDirectEntity();

                if (attacker != null) {
                    if (livingEntity.getRandom().nextFloat() < (thornsLevel * 0.15f)) {
                        //Deal 1 to 4 damage back to the attacker
                        float thornsDamage = 1.0f + livingEntity.getRandom().nextInt(4);
                        attacker.hurtServer(world, livingEntity.damageSources().thorns(livingEntity), thornsDamage);

                        //Thorns applies an extra durability penalty
                        this.useItem.hurtAndBreak(2, livingEntity, EquipmentSlot.OFFHAND);
                    }
                }
            }
        }
    }

    /**
     * Inject code immediately when player is hurt, interrupting consuming.
     */
    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void interruptConsumables(ServerLevel level, DamageSource source, float f, CallbackInfoReturnable<Boolean> cir) {
        //Check if combat changes are enabled in configs
        if (DJsConfig.getInstance().enableCombatChanges && this.isUsingItem()) {
            //If the item is a consumable, cancel the action
            if (!this.useItem.isEmpty() && this.useItem.has(DataComponents.CONSUMABLE)) this.stopUsingItem();
        }
    }

    /**
     * Inject code to double XP dropped by mobs if killed at night under skylight.
     */
    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void boostNightXP(ServerLevel serverLevel, Entity entity, CallbackInfoReturnable<Integer> cir) {
        //Check if XP changes are enabled in configs
        if (DJsConfig.getInstance().enableXPChanges) {
            LivingEntity livingEntity = (LivingEntity) (Object) this;

            //Check if it is currently nighttime
            if (serverLevel.isDarkOutside()) {
                //Check if mob has access to skylight
                if (serverLevel.canSeeSky(livingEntity.blockPosition())) {
                    //Multiply final XP drop by 2
                    int originalXP = cir.getReturnValue();
                    cir.setReturnValue(originalXP * 2);
                }
            }
        }
    }

    /**
     * Inject code when entity is hurt by the server, removing invincibility frames specifically for projectiles.
     */
    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void removeProjectileInvulnerabilityFrames(ServerLevel level, DamageSource source, float f, CallbackInfoReturnable<Boolean> cir) {
        //Check if combat changes are enabled in configs
        if (DJsConfig.getInstance().enableCombatChanges) {
            //Check if the damage is coming from any projectile, reset the invulnerability timer to 0 before the damage is processed
            if (source.is(DamageTypeTags.IS_PROJECTILE)) this.invulnerableTime = 0;
        }
    }

    /**
     * Inject code at the start of a tick, handling weather grounding, extra durability drain, and campfire updrafts.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickElytraNerfs(CallbackInfo ci) {
        if (this.isFallFlying()) {
            //Check if within radius of a beacon with an upgrade allowing flight during rain
            if (!this.level().isClientSide() && !BeaconTracker.hasUpgrade(this.level(), this.blockPosition(), "elytra_rain")) {
                //Prevent flying if player is wet or on fire (flag 7 forcefully closes elytra)
                if (this.isInWaterOrRain() || this.isOnFire()) this.setSharedFlag(7, false);
            }

            //Check if elytra changes are enabled in configs
            if (DJsConfig.getInstance().enableElytraChanges) {
                //Double the standard durability drain rate
                if (!this.level().isClientSide() && this.tickCount % 20 == 10) {
                    ItemStack chest = this.getItemBySlot(EquipmentSlot.CHEST);
                    if (chest.is(Items.ELYTRA) && (Object) this instanceof ServerPlayer player)
                        chest.hurtAndBreak(1, player, EquipmentSlot.CHEST);
                }

                BlockPos blockPos = this.blockPosition();
                //Scan downwards up to 20 blocks
                for (int i = 1; i <= 20; i++) {
                    BlockPos checkPos = blockPos.below(i);
                    BlockState state = this.level().getBlockState(checkPos);

                    //Check if there is a campfire below
                    if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
                        if (state.getValue(CampfireBlock.LIT)) {
                            //Alter the updraft momentum depending on the state of the campfire
                            boolean isSignalFire = state.getValue(CampfireBlock.SIGNAL_FIRE);
                            int maxReach = isSignalFire ? 20 : 10;

                            if (i <= maxReach) {
                                //Catch the updraft and set the timer to 30 ticks (1.5 seconds)
                                this.updraftTicks = 30;
                                this.isStrongUpdraft = isSignalFire;
                                break;
                            }
                        }
                    }
                    //If we hit a solid block (like a roof), stop scanning to save performance
                    else if (state.canOcclude() && state.isCollisionShapeFullBlock(this.level(), checkPos)) break;
                }


                //Apply lingering momentum
                if (this.updraftTicks > 0) {
                    this.updraftTicks--;
                    Vec3 delta = this.getDeltaMovement();

                    //Calculate fading boost (strongest at 30 ticks, weakens as timer approaches 0)
                    double maxBoost = this.isStrongUpdraft ? 0.25 : 0.10;
                    double currentBoost = maxBoost * ((float) this.updraftTicks / 30.0f);

                    //Soft cap the upward velocity
                    if (delta.y < 1.5) this.setDeltaMovement(delta.x, delta.y + currentBoost, delta.z);
                }
            }

            //If they land or close the elytra, immediately kill upward momentum
            else this.updraftTicks = 0;
        }
    }

    /**
     * Inject code after getting hurt, to prevent flying when getting hurt.
     */
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void stopFlyingOnDamage(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> cir) {
        //Check if elytra changes are enabled in configs
        if (DJsConfig.getInstance().enableElytraChanges) {
            //If damaged, force close the elytra
            if (cir.getReturnValue() && this.isFallFlying()) this.setSharedFlag(7, false);
        }
    }

    /**
     * Inject code after getting hurt, changing bane of arthropods into vampirism (steal health from living mobs).
     */
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void vampirismLifeSteal(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        //If attack successfully dealt damage and was caused by a living entity
        if (cir.getReturnValue() && source.getEntity() instanceof LivingEntity entity) {
            LivingEntity victim = (LivingEntity) (Object) this;

            //Prevent healing from undead mobs
            if (victim.getType().is(DJsFixedProgression.VAMPIRISM_TARGETS)) {
                //Get the weapon from the player's hand
                ItemStack weapon = entity.getMainHandItem();

                //Look up bane of arthropods enchantment
                var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var bane = registry.get(Enchantments.BANE_OF_ARTHROPODS);

                //Check if bane of arthropods is present on the weapon
                if (bane.isPresent()) {
                    //Get the enchantment level
                    int enchantmentLevel = weapon.getEnchantments().getLevel(bane.get());
                    if (enchantmentLevel > 0) {
                        //Chances scale from 10% at level 1 to 50% at level 5
                        if (level.random.nextFloat() < (enchantmentLevel * 0.10F)) {
                            //Heal 1 full heart
                            entity.heal(2.0F);

                            //Spawn life steal particles on entity
                            level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + 1.0, entity.getZ(), enchantmentLevel, 0.3, 0.3, 0.3, 0.1);

                        }
                    }
                }
            }
        }
    }

    /**
     * Inject code before dropping loot, ensuring specially-tagged zombies don't drop any loot.
     */
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void cancelAmbushLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        //If the zombie has the tag, cancel the loot generation
        if (this.getTags().contains("djs_ambush_zombie")) ci.cancel();
    }

    /**
     * Inject code before dropping XP, ensuring specially-tagged zombies don't drop any XP.
     */
    @Inject(method = "getExperienceReward", at = @At("HEAD"), cancellable = true)
    private void cancelAmbushXP(ServerLevel level, Entity entity, CallbackInfoReturnable<Integer> cir) {
        //If the zombie has the tag, cancel the XP generation
        if (this.getTags().contains("djs_ambush_zombie")) cir.setReturnValue(0);
    }
}
