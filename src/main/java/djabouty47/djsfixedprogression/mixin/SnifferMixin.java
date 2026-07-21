package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.config.DJsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Sniffer.class)
public class SnifferMixin extends Animal {
    //Scent status
    @Unique private String djs$activeScent = "UNIVERSAL";
    @Unique private int djs$scentCharges = 0;

    //Hunger status
    @Unique private boolean djs$isFed = false;
    @Unique private boolean djs$handFedWander = false; // Tracks if it should wander a short distance vs long distance
    @Unique private int djs$eatingTimer = 0;

    //Location data
    @Unique private Vec3 djs$lastDigPos = Vec3.ZERO;
    @Unique private BlockPos djs$targetComposter = null;
    @Unique private int djs$composterScanTimer = 0;

    //Biome loot tables
    @Unique private static final Item[] FOREST_ITEMS = {Items.SPRUCE_SAPLING, Items.MOSS_BLOCK, Items.FERN, Items.SWEET_BERRIES, Items.SPRUCE_LEAVES, Items.OAK_LEAVES};
    @Unique private static final Item[] JUNGLE_ITEMS = {Items.BAMBOO, Items.COCOA_BEANS, Items.JUNGLE_LEAVES, Items.JUNGLE_SAPLING};
    @Unique private static final Item[] SWAMP_ITEMS = {Items.LILY_PAD, Items.MANGROVE_PROPAGULE, Items.MANGROVE_LEAVES, Items.MANGROVE_ROOTS};

    protected SnifferMixin(EntityType<? extends @NotNull Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean isFood(@NotNull ItemStack itemStack) {
        return false;
    }

    /**
     * Inject code when player interacts with the sniffer, changing scent focus when fed a specific plant.
     */
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void feedSnifferScents(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        //Check if pig and sniffer changes are enabled in configs
        if (!DJsConfig.getInstance().enablePigAndSnifferChanges) return;

        //Get the scent based off the item in the player's hand
        ItemStack stack = player.getItemInHand(hand);
        String newScent = djs$getScentFromItem(stack);

        if (newScent != null) {
            if (!this.level().isClientSide()) {
                //Consume item if not in creative
                if (!player.isCreative()) stack.shrink(1);

                //Play sound effect and particles
                this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.5, this.getZ(), 7, 0.5, 0.5, 0.5, 0.0);

                //Update scent and hunger status
                this.djs$activeScent = newScent;
                this.djs$scentCharges = 5;
                this.djs$isFed = true;
                this.djs$handFedWander = true;
                this.djs$lastDigPos = this.position();
            }
            cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
        }
    }

    /**
     * Inject code at the start of the sniffer's server-side ticking logic, handling composter eating and distance checking.
     */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void handleComposterAndDistance(CallbackInfo ci) {
        //Check if pig and sniffer changes are enabled in configs
        if (!DJsConfig.getInstance().enablePigAndSnifferChanges) return;

        //If the sniffer hasn't been fed
        if (!this.djs$isFed) {
            //If the sniffer is actively eating at a composter
            if (this.djs$eatingTimer > 0) {
                //Decrement timer
                this.djs$eatingTimer--;

                //Stop moving while eating
                this.getNavigation().stop();

                if (this.djs$targetComposter != null) {
                    //Force sniffer to turn and look at composter
                    this.getLookControl().setLookAt(this.djs$targetComposter.getX() + 0.5, this.djs$targetComposter.getY() + 0.5, this.djs$targetComposter.getZ() + 0.5);

                    //Play eating sounds
                    if (this.djs$eatingTimer % 10 == 0) this.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
                }

                //Once the timer hits 0, consume the composter
                if (this.djs$eatingTimer <= 0) {
                    if (this.djs$targetComposter != null) {
                        BlockState state = this.level().getBlockState(this.djs$targetComposter);
                        //If composter is still valid and full
                        if (state.is(Blocks.COMPOSTER) && state.getValue(ComposterBlock.LEVEL) == 8) {
                            this.level().setBlock(this.djs$targetComposter, state.setValue(ComposterBlock.LEVEL, 0), 3);
                            this.heal(10.0F);
                            this.playSound(SoundEvents.PLAYER_BURP, 1.0F, 1.0F);
                            this.djs$isFed = true;
                        }
                        //Update status if someone emptied or broke the composter while eating
                        else this.djs$targetComposter = null;
                    }
                    //Restore vanilla sniffer movement speed
                    Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.1D);
                }
            }
            //If not eating, handle composter searching
            else {
                this.djs$composterScanTimer--;

                //Scan for a full composter if previous one was forgotten or missing
                if (this.djs$composterScanTimer <= 0) {
                    this.djs$composterScanTimer = 40;
                    if (this.djs$targetComposter == null || !this.level().getBlockState(this.djs$targetComposter).is(Blocks.COMPOSTER)) this.djs$targetComposter = djs$findComposter();
                }

                //If a composter is found
                if (this.djs$targetComposter != null) {
                    //If the composter is close enough, eat the compost
                    if (this.blockPosition().closerThan(this.djs$targetComposter, 2.5)) {
                        BlockState state = this.level().getBlockState(this.djs$targetComposter);

                        //If the composter is full, start eating
                        if (state.getValue(ComposterBlock.LEVEL) == 8) {
                            this.djs$eatingTimer = 60;

                            //Freeze movement
                            Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(0.0D);
                        }
                        //The remembered composter is empty, forget about it and find a new one
                        else this.djs$targetComposter = null;
                    }
                    //Force the sniffer to path find to the composter
                    else this.getNavigation().moveTo(this.djs$targetComposter.getX(), this.djs$targetComposter.getY(), this.djs$targetComposter.getZ(), 1.2);
                }
            }
            //Wipe memory if not fed
            this.getBrain().eraseMemory(MemoryModuleType.SNIFFER_DIGGING);
            this.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
        }
        //If fed, enforce distance requirements
        else {
            //Determine hand-fed or composter-fed distance
            double requiredDistanceSq = this.djs$handFedWander ? 25.0 : 225.0;

            //Force the sniffer to wander further if too close to previous dig position
            if (this.position().distanceToSqr(this.djs$lastDigPos) < 225.0) {
                this.getBrain().eraseMemory(MemoryModuleType.SNIFFER_DIGGING);
                this.getBrain().eraseMemory(MemoryModuleType.SNIFFER_SNIFFING_TARGET);
            }
        }
    }

    /**
     * Inject code at the end of each tick, pitching the sniffer's head down while eating.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void syncClientEatingVisuals(CallbackInfo ci) {
        //Check if pig and sniffer changes are enabled in configs
        if (!DJsConfig.getInstance().enablePigAndSnifferChanges) return;

        //If the movement speed has been frozen to 0 by the server, pitch the head down!
        if (this.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) <= 0.01D) {
            this.setXRot(40.0F);
            this.xRotO = 40.0F;
        }
    }

    /**
     * Inject code when storing previous digging location, cancelling it so the sniffer has to rely on custom logic.
     */
    @Inject(method = "storeExploredPosition", at = @At("HEAD"), cancellable = true)
    private void overrideStoreExploredPosition(BlockPos blockPos, CallbackInfoReturnable<Sniffer> cir) {
        //Check if pig and sniffer changes are enabled in configs
        if (!DJsConfig.getInstance().enablePigAndSnifferChanges) return;

        cir.setReturnValue((Sniffer) (Object) this);
    }

    /**
     * Inject code when the Sniffer drops its loot, spawning custom loot table instead.
     */
    @Inject(method = "dropSeed", at = @At("HEAD"), cancellable = true)
    private void dropCustomLoot(CallbackInfo ci) {
        //Check if pig and sniffer changes are enabled in configs
        if (!DJsConfig.getInstance().enablePigAndSnifferChanges) return;

        if (this.level() instanceof ServerLevel serverLevel) {
            //Randomly spawn custom loot
            ItemStack loot = djs$getSnifferLoot(this.getRandom());
            this.spawnAtLocation(serverLevel, loot, 0.5f);

            //Consume the fed state and reset hand-fed wander
            this.djs$isFed = false;
            this.djs$handFedWander = false;
            this.djs$lastDigPos = this.position();

            //Consume the scent charge
            if (!this.djs$activeScent.equals("UNIVERSAL")) {
                this.djs$scentCharges--;
                if (this.djs$scentCharges <= 0) this.djs$activeScent = "UNIVERSAL";
            }

            //Set a 4-minute cooldown before the next sniff
            this.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 4800L);
        }
        //Prevent the vanilla torch flower/pitcher drop
        ci.cancel();
    }

    /**
     * Helper method to search nearby area for a full composter.
     */
    @Unique
    private BlockPos djs$findComposter() {
        BlockPos current = this.blockPosition();
        for (int x = -10; x <= 10; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -10; z <= 10; z++) {
                    BlockPos pos = current.offset(x, y, z);
                    BlockState state = this.level().getBlockState(pos);
                    if (state.is(Blocks.COMPOSTER) && state.getValue(ComposterBlock.LEVEL) == 8) return pos;
                }
            }
        }
        return null;
    }

    /**
     * Helper method that gets the corresponding scent for the loot table based off an item.
     */
    @Unique
    private String djs$getScentFromItem(ItemStack stack) {
        for (Item item : FOREST_ITEMS) if (stack.is(item)) return "FOREST";
        for (Item item : JUNGLE_ITEMS) if (stack.is(item)) return "JUNGLE";
        for (Item item : SWAMP_ITEMS) if (stack.is(item)) return "SWAMP";
        return null;
    }

    /**
     * Method to compute the weighted random loot table for the sniffer.
     * Fed scent influences loot table return item.
     */
    @Unique
    private ItemStack djs$getSnifferLoot(RandomSource random) {
        //If scent is active, pull from specific biome table instead
        if (!this.djs$activeScent.equals("UNIVERSAL")) {
            Item[] pool = switch (this.djs$activeScent) {
                case "FOREST" -> FOREST_ITEMS;
                case "JUNGLE" -> JUNGLE_ITEMS;
                case "SWAMP" -> SWAMP_ITEMS;
                default -> null;
            };

            if (pool != null) return new ItemStack(pool[random.nextInt(pool.length)]);
        }

        //Universal table if no scent is present
        int uRoll = random.nextInt(100);
        if (uRoll < 5) return new ItemStack(Items.SNORT_POTTERY_SHERD);
        if (uRoll < 10) return new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
        if (uRoll < 20) return new ItemStack(Items.TORCHFLOWER_SEEDS);
        if (uRoll < 30) return new ItemStack(Items.PITCHER_POD);
        if (uRoll < 45) return new ItemStack(Items.GOLD_NUGGET, random.nextInt(3) + 1);
        if (uRoll < 60) return new ItemStack(Items.IRON_NUGGET, random.nextInt(3) + 1);
        if (uRoll < 75) return new ItemStack(Items.COPPER_NUGGET, random.nextInt(3) + 1);
        if (uRoll < 90) return new ItemStack(Items.DEAD_BUSH);
        return new ItemStack(Items.WHEAT_SEEDS);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) {
        return null;
    }
}
