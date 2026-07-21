package djabouty47.djsfixedprogression.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import djabouty47.djsfixedprogression.DJsFixedProgression;
import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.network.SleepVoteCancelPayload;
import djabouty47.djsfixedprogression.procedures_and_util.BeaconTracker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Unit;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    @Unique private int djs$xpToDrain = 0;

    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    /**
     * Inject code right before sleeping, checking if the player meets custom sleep conditions.
     */
    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void checkCustomSleepConditions(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ServerLevel level = player.level().getLevel();

        //Prevent sleeping during thunderstorms
        if (level.isThundering()) {
            player.displayClientMessage(Component.literal("You may not rest now; the storm has grown too violent").withStyle(ChatFormatting.WHITE), true);
            cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
            return;
        }

        //Check if the sleep time delay is enabled in config
        if (DJsConfig.getInstance().enableDelayedSleep) {
            long timeOfDay = player.level().getDayTime() % 24000;
            int minTime = DJsConfig.getInstance().minimumSleepTime;

            //Block sleep between vanilla sleep time and the custom time (unless thundering)
            if (timeOfDay >= 12542 && timeOfDay < minTime) {
                //Notify player, return OTHER_PROBLEM, and break before reaching XP warning message
                player.displayClientMessage(Component.literal("You can sleep only at night").withStyle(ChatFormatting.WHITE), true);
                cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
                return;
            }
        }

        int cost = DJsConfig.getInstance().bedXPCost;

        //Check if the player has enough XP to sleep or is in creative mode
        if (cost > 0 && !player.isCreative() && player.experienceLevel < cost) {
            //Notify player and return OTHER_PROBLEM
            player.displayClientMessage(Component.literal("You may not rest now; you need at least " + cost + " XP levels").withStyle(ChatFormatting.WHITE), true);
            cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
        }
    }

    /**
     * Inject code after a successful sleep attempt to initialize XP drain tracker.
     */
    @Inject(method = "startSleepInBed", at = @At("RETURN"))
    private void onSuccessfulSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        //If the sleep was successful, prime the debt
        if (cir.getReturnValue().right().isPresent()) this.djs$xpToDrain = DJsConfig.getInstance().bedXPCost;
    }

    /**
     * Inject at the end of each tick, fast-forwarding time upon sleeping.
     * As the player sleeps, drain XP and hunger.
     * Pause sleep and fast-forwarding when mobs are nearby.
     * Sleeping in the dark spawns a zombie.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void handleFastForwardSleep(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ServerLevel level = player.level();

        if (player.isSleeping()) {
            int sleepTimer = player.getSleepTimer();

            //Check if player is fully asleep
            if (sleepTimer >= 100) {
                //Get all sleeping players
                List<ServerPlayer> sleepingPlayers = level.players().stream().filter(Player::isSleeping).toList();

                //Check if any sleeping player is in danger
                boolean anyInDanger = sleepingPlayers.stream().anyMatch(p -> !level.getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(16.0)).isEmpty());

                //Designate only one player to control global fast-forward
                boolean isPrimarySleeper = !sleepingPlayers.isEmpty() && sleepingPlayers.getFirst() == player;

                //If a monster poses a threat to any of the sleeping players
                if (anyInDanger) {
                    //Reset the tick speed if the player is the primary sleeper
                    if (isPrimarySleeper && level.getServer().tickRateManager().tickrate() > 20.0F) level.getServer().tickRateManager().setTickRate(20.0F);

                    //Warn specific players who are in danger
                    boolean amIInDanger = !level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(16.0)).isEmpty();
                    if (amIInDanger && player.tickCount % 40 == 0) player.displayClientMessage(Component.literal("You feel a hostile presence approaching...").withStyle(ChatFormatting.WHITE), false);
                }
                //Otherwise continue sleeping
                else {
                    //Check if the player is in creative or peaceful
                    if (!player.isCreative() && level.getDifficulty() != Difficulty.PEACEFUL) {
                        //Check if the bed has sufficient lighting
                        if (level.getBrightness(LightLayer.BLOCK, player.blockPosition().above()) <= 4) {
                            //If not, have a 5% chance per second of spawning a zombie nearby
                            if (level.random.nextInt(2000) == 0) {
                                Zombie zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.EVENT);
                                if (zombie != null) {
                                    //Spawn zombie at the bed
                                    BlockPos pos = player.blockPosition();
                                    zombie.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                                    zombie.addTag("djs_ambush_zombie");
                                    level.addFreshEntity(zombie);

                                    //Notify player and stop sleeping
                                    player.stopSleepInBed(true, true);
                                    player.displayClientMessage(Component.literal("You were awoken by a wandering monster").withStyle(ChatFormatting.WHITE), true);
                                    return;
                                }
                            }
                        }
                    }

                    //Fast-forward time and drain XP if all players are sleeping
                    boolean allSleeping = sleepingPlayers.size() == level.players().size();
                    if (allSleeping) {
                        //If this is the primary sleeper, increase the server speed
                        if (isPrimarySleeper && level.getServer().tickRateManager().tickrate() == 20.0F) level.getServer().tickRateManager().setTickRate(200.0F);

                        //Spread XP drain perfectly over fast-forwarded night
                        int cost = DJsConfig.getInstance().bedXPCost;
                        int drainInterval = cost > 0 ? Math.max(20, 10000 / cost) : 10000;

                        //Smoothly drain XP throughout the night
                        if (this.djs$xpToDrain > 0 && !player.isCreative() && player.tickCount % drainInterval == 0) {
                            player.giveExperienceLevels(-1);
                            this.djs$xpToDrain--;
                        }
                    }
                    //If someone leaves their bed or joins the server, abort the time-lapse
                    else if (isPrimarySleeper && level.getServer().tickRateManager().tickrate() > 20.0F) level.getServer().tickRateManager().setTickRate(20.0F);

                    //Check if it is morning
                    long timeOfDay = level.getDayTime() % 24000;
                    boolean isMorning = timeOfDay >= 0 && timeOfDay < 12000;
                    if (isMorning || level.isThundering()) {
                        //Clear any sleep votes
                        DJsFixedProgression.SLEEP_VOTES.clear();

                        //Force players out of the bed
                        player.stopSleepInBed(true, true);

                        //Alert players about not being able to sleep during a thunderstorm
                        if (level.isThundering()) player.displayClientMessage(Component.literal("You may not rest now; the storm has grown too violent").withStyle(ChatFormatting.WHITE), true);
                    }
                }
            }
        }
    }

    /**
     * Inject code at the end of each tick, revoking sleep vote skip if player enters the dark.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void validateVoteState(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        //Check if player has a successful vote
        if (DJsFixedProgression.SLEEP_VOTES.contains(player.getUUID())) {
            //Ensure player is not in creative or peaceful mode
            if (!player.isCreative() && player.level().getDifficulty() != Difficulty.PEACEFUL) {
                //Check if the player has entered the dark
                if (player.level().getBrightness(LightLayer.BLOCK, player.blockPosition().above()) <= 4) {
                    //Revoke the vote
                    DJsFixedProgression.SLEEP_VOTES.remove(player.getUUID());

                    //Notify player
                    player.displayClientMessage(Component.literal("Your vote to skip the night was cancelled because you entered the dark").withStyle(ChatFormatting.WHITE), true);

                    //Cancel player vote on client
                    ServerPlayNetworking.send(player, new SleepVoteCancelPayload());
                }
            }
        }
    }

    /**
     * Inject code when the player leaves the bed, resetting the tick rate and cancelling their night skip vote.
     */
    @Inject(method = "stopSleepInBed", at = @At("HEAD"))
    private void onWakeUp(boolean bl, boolean bl2, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        //Slow the tickrate down when leaving bed
        if (player.level().getServer().tickRateManager().tickrate() > 20.0F) player.level().getServer().tickRateManager().setTickRate(20.0F);

        //Remove night skip vote if player leaves their bed
        if (DJsFixedProgression.SLEEP_VOTES.remove(player.getUUID())) {
            //Cancel player vote on client
            ServerPlayNetworking.send(player, new SleepVoteCancelPayload());

            //Check if anyone else is still sleeping
            boolean anyoneElseSleeping = player.level().getLevel().players().stream().anyMatch(p -> p != player && p.isSleeping());

            //If no one else is sleeping, cancel skip night vote for everyone
            if (!anyoneElseSleeping && !DJsFixedProgression.SLEEP_VOTES.isEmpty()) {
                DJsFixedProgression.SLEEP_VOTES.clear();
                player.level().getLevel().getServer().getPlayerList().broadcastSystemMessage(Component.literal("Sleep vote cancelled because all players left their beds").withStyle(ChatFormatting.WHITE), false);

                //Reset client for all players
                for (ServerPlayer p : player.level().getLevel().players()) ServerPlayNetworking.send(p, new SleepVoteCancelPayload());
            }
        }
    }

    /**
     * Inject after each game tick, adding a small amount of exhaustion
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void applyIdleExhaustion(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        //Check if idle hunger drain is enabled in config
        if (!DJsConfig.getInstance().enableIdleHungerDrain) return;

        //Check if beacon changes are enabled and player isn't within range of a beacon with an upgrade preventing afk hunger drain
        if (DJsConfig.getInstance().enableBeaconChanges && !this.level().isClientSide() && BeaconTracker.hasUpgrade(this.level(), this.blockPosition(), "afk_drain")) return;

        //Check if player is in survival/adventure
        if (!player.isCreative() && !player.isSpectator()) {
            //Add a small amount of exhaustion every 20 ticks
            if (player.tickCount % 20 == 0) player.causeFoodExhaustion(DJsConfig.getInstance().idleExhaustionPerSecond);
        }
    }

    /**
     * Inject code when setting respawn position, intercepting and cancelling the action if it is a bed.
     */
    @Inject(method = "setRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void disableBedRespawn(ServerPlayer.RespawnConfig respawnConfig, boolean bl, CallbackInfo ci) {
        //Check if campfire spawn changes are enabled in configs
        if (DJsConfig.getInstance().enableCampfireRespawningInsteadOfBed && respawnConfig != null && !respawnConfig.forced()) {
            BlockState state = this.level().getBlockState(respawnConfig.respawnData().pos());

            //If the block is a bed, cancel the spawn point update
            if (state.getBlock() instanceof BedBlock) ci.cancel();
        }
    }

    /**
     * Inject code to intercept native spawn resolution to safely spawn players next to solid campfires.
     */
    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
    private void overrideCampfireRespawn(boolean bl, TeleportTransition.PostTeleportTransition postTeleportTransition, CallbackInfoReturnable<TeleportTransition> cir) {
        //Fetch player and respawn data
        ServerPlayer player = (ServerPlayer) (Object) this;
        ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();

        //Check if player has campfire tag
        boolean isCampfireSpawn = player.getTags().contains("djs_campfire_spawn");

        //Check if campfire respawn changes are enabled in configs
        if (!DJsConfig.getInstance().enableCampfireRespawningInsteadOfBed) {
            //Ensure campfire respawn data is erased if the player has it
            if (isCampfireSpawn) {
                player.setRespawnPosition(null, false);
                player.removeTag("djs_campfire_spawn");
            }
            return;
        }

        //Check if the player is respawning at a campfire
        if (respawnConfig != null && respawnConfig.forced() && isCampfireSpawn) {
            //Safely fetch the spawn details
            BlockPos spawnPos = respawnConfig.respawnData().pos();
            assert player.getRespawnConfig() != null;
            ResourceKey<@NotNull Level> dimKey = player.getRespawnConfig().respawnData().dimension();
            ServerLevel spawnLevel = player.level().getServer().getLevel(dimKey);

            //Verify they are respawning at the correct campfire
            if (spawnLevel != null) {
                BlockState state = spawnLevel.getBlockState(spawnPos);

                //If target spawn block is still a lit campfire, find a safe adjacent block
                if (state.is(BlockTags.CAMPFIRES) && state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) {
                    Direction facing = state.hasProperty(CampfireBlock.FACING) ? state.getValue(CampfireBlock.FACING) : Direction.NORTH;

                    //Find safe, unobstructed block adjacent to campfire
                    Optional<Vec3> safePos = BedBlock.findStandUpPosition(EntityType.PLAYER, spawnLevel, spawnPos, facing, respawnConfig.respawnData().yaw());

                    //If a safe position is found
                    if (safePos.isPresent()) {
                        cir.setReturnValue(new TeleportTransition(spawnLevel, safePos.get(), Vec3.ZERO, respawnConfig.respawnData().yaw(), respawnConfig.respawnData().pitch(), postTeleportTransition));
                        return;
                    }
                }
                //The campfire was unlit or destroyed, erase player spawn point
                player.setRespawnPosition(null, false);
                player.removeTag("djs_campfire_spawn");

                //Return missing bed handler
                cir.setReturnValue(TeleportTransition.missingRespawnBlock(player, postTeleportTransition));
            }
        }
    }

    /**
     * Inject code after getting hurt, restarting the 8-second healing pause timer whenever the player takes damage.
     */
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void setRegenCooldown(ServerLevel serverLevel, DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        //Check if hunger and regeneration changes are enabled in configs
        if (!DJsConfig.getInstance().enableHungerAndRegenerationChanges) return;

        //Set cooldown for 160 ticks (8 seconds)
        if (cir.getReturnValue()) DJsFixedProgression.REGEN_COOLDOWNS.put((ServerPlayer) (Object) this, 160);
    }

    /**
     * Inject code at the end of each tick, decrementing the regeneration tick timer.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void tickRegenCooldown(CallbackInfo ci) {
        //Check if hunger and regeneration changes are enabled in configs
        if (!DJsConfig.getInstance().enableHungerAndRegenerationChanges) return;

        Integer cooldown = DJsFixedProgression.REGEN_COOLDOWNS.get(this);
        if (cooldown != null && cooldown > 0) DJsFixedProgression.REGEN_COOLDOWNS.put(this, cooldown - 1);
    }
}
