package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import djabouty47.djsfixedprogression.procedures_and_util.IUpgradableBeacon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin implements IUpgradableBeacon {
    @Unique private CompoundTag djs$upgrades = new CompoundTag();

    @Override public CompoundTag djs$getUpgrades() { return this.djs$upgrades; }
    @Override public void djs$setUpgrades(CompoundTag tag) { this.djs$upgrades = tag; }

    /**
     * Inject code after saving block data, saving beacon upgrades.
     */
    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void saveUpgrades(ValueOutput output, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs and upgrades exist before saving upgrades
        if (DJsConfig.getInstance().enableBeaconChanges && !this.djs$upgrades.isEmpty()) output.store("djs_upgrades", CompoundTag.CODEC, this.djs$upgrades);
    }

    /**
     * Inject code after loading block data, loading beacon upgrades.
     */
    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void loadUpgrades(ValueInput input, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs before loading custom tag and upgrades
        if (DJsConfig.getInstance().enableBeaconChanges) if (DJsConfig.getInstance().enableBeaconChanges) input.read("djs_upgrades", CompoundTag.CODEC).ifPresent(tag -> this.djs$upgrades = tag);
    }

    /**
     * Inject code when applying beacon effects, changing it to rely on the pyramid material rather than the UI-based selection.
     * Also update the beacon tracker with this beacon's data.
     * Also increase beacon effect duration if within range of a beacon with an active effect time extension upgrade.
     */
    @Inject(method = "applyEffects", at = @At("HEAD"), cancellable = true)
    private static void applyCustomModularEffects(Level level, BlockPos pos, int vanillaLevels, @Nullable Holder<@NotNull MobEffect> primary, @Nullable Holder<@NotNull MobEffect> secondary, CallbackInfo ci) {
        //Check if beacon changes are enabled in configs
        if (!DJsConfig.getInstance().enableBeaconChanges) return;

        //Cancel vanilla UI-based effect application
        ci.cancel();

        int customLevels = IUpgradableBeacon.getBeaconLevel(level, pos);

        //Update the beacon tracker
        if (level.getBlockEntity(pos) instanceof IUpgradableBeacon be) BeaconTracker.updateBeacon(level, pos, customLevels, be.djs$getUpgrades());

        //If the pyramid is valid, apply effects
        if (customLevels > 0) {
            Holder<@NotNull MobEffect> beaconEffect = getEffectForBlock(level.getBlockState(pos.below()).getBlock());

            if (beaconEffect != null) {
                //1 layer = 20 block radius, 4 layers = 100 block radius
                double radius = (customLevels * 20 + 20);

                //Base beacon effect amplifier
                int amplifier = 0;

                //Base time boost amplifier
                int timeBoosts = 0;

                //Effect flags
                boolean applyToPets = false;
                boolean inflictWither = false;
                boolean inflictGlowing = false;

                //Fetch NBT to calculate amplifiers and time boosts
                if (level.getBlockEntity(pos) instanceof IUpgradableBeacon beacon) {
                    CompoundTag upgrades = beacon.djs$getUpgrades();
                    //If effect increase upgrade is purchased, upgrade effect to level 2
                    if (upgrades.contains("effect_increase")) amplifier = 1;

                    //Count how many times upgrades are owned
                    if (upgrades.contains("effect_time_1")) timeBoosts++;
                    if (upgrades.contains("effect_time_2")) timeBoosts++;
                    if (upgrades.contains("effect_time_3")) timeBoosts++;
                    if (upgrades.contains("effect_time_4")) timeBoosts++;

                    //Check if pets should receive effects
                    if (upgrades.contains("pet_effects")) applyToPets = true;

                    //Check if ability to inflict wither effect on hostile mobs was purchased
                    if (upgrades.contains("inflict_wither")) {
                        //Read the ability duration
                        long durationEnd = upgrades.getLongOr("inflict_wither_duration_end", 0);

                        //If the ability is ongoing, inflict effect
                        if (level.getGameTime() < durationEnd) inflictWither = true;
                    }

                    //Check if ability to inflict glowing effect on hostile mobs was purchased
                    if (upgrades.contains("mob_glowing")) {
                        //Read the ability duration
                        long durationEnd = upgrades.getLongOr("mob_glowing_duration_end", 0);

                        //If the ability is ongoing, inflict effect
                        if (level.getGameTime() < durationEnd) inflictGlowing = true;
                    }
                }

                //Determine the effect duration (each time boost adds 10 seconds)
                int duration = (9 + customLevels * 2 + (timeBoosts * 10)) * 20;

                //Grab all players within range
                AABB aabb = (new AABB(pos)).inflate(radius).expandTowards(0.0D, level.getHeight(), 0.0D);
                List<Player> players = level.getEntitiesOfClass(Player.class, aabb);

                //Only apply physical potion effects on server
                if (!level.isClientSide()) {
                    //Apply effects to all players within range
                    for (Player player : players)
                        player.addEffect(new MobEffectInstance(beaconEffect, duration, amplifier, true, true));

                    //If pet effect upgrade is active
                    if (applyToPets) {
                        //Apply beacon effects to standard pets (dogs, cats, parrots) within range
                        List<TamableAnimal> pets = level.getEntitiesOfClass(TamableAnimal.class, aabb);
                        for (TamableAnimal pet : pets) {
                            //Apply effect if tamed
                            if (pet.isTame())
                                pet.addEffect(new MobEffectInstance(beaconEffect, duration, amplifier, true, true));
                        }

                        //Apply beacon effects to ride-able pets (horses, donkeys, llamas)
                        List<AbstractHorse> horses = level.getEntitiesOfClass(AbstractHorse.class, aabb);
                        for (AbstractHorse horse : horses) {
                            //Apply effect if tamed
                            if (horse.isTamed())
                                horse.addEffect(new MobEffectInstance(beaconEffect, duration, amplifier, true, true));
                        }

                        //Apply beacon effects to happy ghast
                        List<HappyGhast> happyGhasts = level.getEntitiesOfClass(HappyGhast.class, aabb);
                        for (HappyGhast happyGhast : happyGhasts)
                            happyGhast.addEffect(new MobEffectInstance(beaconEffect, duration, amplifier, true, true));
                    }

                    //Check if wither ability is active
                    if (inflictWither) {
                        //Search for all living entities, filtering those that implement the enemy interface
                        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, aabb, e -> e instanceof Enemy);

                        //Apply wither effect to all hostile mobs
                        for (LivingEntity enemy : enemies)
                            enemy.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, 0, true, true));
                    }

                    //Check if glowing ability is active
                    if (inflictGlowing) {
                        //Search for all living entities, filtering those that implement the enemy interface
                        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, aabb, e -> e instanceof Enemy);

                        //Apply glowing effect to all hostile mobs
                        for (LivingEntity enemy : enemies)
                            enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, true, true));
                    }
                }
            }
        }
    }

    /**
     * Inject code when removing the beacon, clearing it from the tracker if it gets broken/removed.
     */
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void removeFromTracker(CallbackInfo ci) {
        BeaconBlockEntity beacon = (BeaconBlockEntity) (Object) this;
        if (beacon.getLevel() != null) BeaconTracker.removeBeacon(beacon.getLevel(), beacon.getBlockPos());
    }

    /**
     * Helper method for mapping specific block types to their core beacon abilities.
     */
    @Unique
    private static Holder<@NotNull MobEffect> getEffectForBlock(Block block) {
        if (block.equals(Blocks.COAL_BLOCK)) return MobEffects.NIGHT_VISION;
        if (block.defaultBlockState().is(BlockTags.COPPER)) return MobEffects.SPEED;
        if (block.equals(Blocks.IRON_BLOCK)) return MobEffects.JUMP_BOOST;
        if (block.equals(Blocks.GOLD_BLOCK)) return MobEffects.STRENGTH;
        if (block.equals(Blocks.QUARTZ_BLOCK)) return MobEffects.INVISIBILITY;
        if (block.equals(Blocks.LAPIS_BLOCK)) return DJsFixedProgression.CLEANSE_EFFECT;
        if (block.equals(Blocks.REDSTONE_BLOCK)) return MobEffects.HASTE;
        if (block.equals(Blocks.DIAMOND_BLOCK)) return MobEffects.REGENERATION;
        if (block.equals(Blocks.EMERALD_BLOCK)) return MobEffects.LUCK;
        if (block.equals(Blocks.NETHERITE_BLOCK)) return MobEffects.RESISTANCE;
        return null;
    }
}
