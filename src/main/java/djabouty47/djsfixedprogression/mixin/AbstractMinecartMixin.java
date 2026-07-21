package djabouty47.djsfixedprogression.mixin;

import djabouty47.djsfixedprogression.DJsFixedProgression;
import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.ILinkableMinecart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends Entity implements ILinkableMinecart {
    @Shadow public abstract boolean isOnRails();
    @Shadow public abstract boolean isFurnace();

    @Unique private UUID djs$parentUUID = null;
    @Unique private UUID djs$childUUID = null;
    @Unique private String djs$chainToChild = null;
    @Unique private int djs$clientLinkParent = -1;
    @Unique private int djs$clientLinkChild = -1;
    @Unique private final Deque<MinecartStep> djs$history = new LinkedList<>();
    @Unique private int djs$rampBuffer = 0;

    public AbstractMinecartMixin(EntityType<?> type, Level world) { super(type, world); }

    @Override public void djs$setParent(UUID uuid) { this.djs$parentUUID = uuid; }
    @Override public UUID djs$getParent() { return this.djs$parentUUID; }
    @Override public void djs$setChild(UUID uuid) { this.djs$childUUID = uuid; }
    @Override public UUID djs$getChild() { return this.djs$childUUID; }
    @Override public void djs$setChainToChild(String name) { this.djs$chainToChild = name; }
    @Override public String djs$getChainToChild() { return this.djs$chainToChild; }

    @Override public void djs$setClientLinkParent(int id) { this.djs$clientLinkParent = id; }
    @Override public int djs$getClientLinkParent() { return this.djs$clientLinkParent; }
    @Override public void djs$setClientLinkChild(int id) { this.djs$clientLinkChild = id; }
    @Override public int djs$getClientLinkChild() { return this.djs$clientLinkChild; }
    @Override public Deque<MinecartStep> djs$getHistory() { return this.djs$history; }

    /**
     * Inject code into max tSpeed calculation, uncapping the vanilla tSpeed cap for the furnace minecart.
     */
    @Inject(method ="getMaxSpeed", at = @At("RETURN"), cancellable = true, require = 0)
    private void uncapSpeedLocal(CallbackInfoReturnable<Double> cir) {
        //Check if minecart changes are enabled in configs, and if this minecart is a furnace minecart
        if (DJsConfig.getInstance().enableMinecartChanges && this.isFurnace()) cir.setReturnValue(Math.max(cir.getReturnValue(), 0.8));
    }

    /**
     * Inject code into other mapping of getMaxSpeed, uncapping the vanilla tSpeed cap for the furnace minecart.
     */
    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true, require = 0)
    private void uncapSpeedServer(ServerLevel level, CallbackInfoReturnable<Double> cir) {
        //Check if minecart changes are enabled in configs, and if this minecart is a furnace minecart
        if (DJsConfig.getInstance().enableMinecartChanges && this.isFurnace()) cir.setReturnValue(Math.max(cir.getReturnValue(), 0.8));
    }

    /**
     * Helper method to determine if a specific minecart is found within a train system.
     */
    @Unique private boolean djs$isInSameTrain(Entity other) {
        if (!(other instanceof AbstractMinecart otherMinecart)) return false;

        //Server Check
        if (this.djs$parentUUID != null && this.djs$parentUUID.equals(other.getUUID())) return true;
        if (this.djs$childUUID != null && this.djs$childUUID.equals(other.getUUID())) return true;

        //Client Check
        if (this.level().isClientSide()) {
            if (this.djs$clientLinkParent != -1 && this.djs$clientLinkParent == other.getId()) return true;
            if (this.djs$clientLinkChild != -1 && this.djs$clientLinkChild == other.getId()) return true;
        }

        List<AbstractMinecart> train = this.djs$getTrainMinecarts(this.level());
        return train.contains(otherMinecart);
    }

    /**
     * Inject code to disable vanilla collision for linked minecarts.
     */
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void disableTrainCollisionRay(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        //Check if minecart changes are enabled in configs and if the minecart we want to disable collision for is in the same train
        if (DJsConfig.getInstance().enableMinecartChanges && djs$isInSameTrain(entity)) cir.setReturnValue(false);
    }

    /**
     * Inject code to prevent other entities from being able to push follower minecarts.
     */
    @Inject(method = "push", at = @At("HEAD"), cancellable = true)
    private void disableTrainPushing(Entity entity, CallbackInfo ci) {
        //Check if minecart changes are enabled in configs
        if (!DJsConfig.getInstance().enableMinecartChanges) return;

        //Prevent minecarts in the same train from pushing each other
        if (djs$isInSameTrain(entity)) ci.cancel();

        //Prevent players, mobs, and other random entities from nudging follower minecarts
        else {
            boolean hasParent = this.djs$parentUUID != null || (this.level().isClientSide() && this.djs$clientLinkParent != -1);
            if (hasParent) ci.cancel();
        }
    }

    /**
     * Inject code to prevent follower minecarts from being movable by walking into them.
     */
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void makeFollowersUnpushable(CallbackInfoReturnable<Boolean> cir) {
        //Check if minecart changes are enabled in configs
        if (!DJsConfig.getInstance().enableMinecartChanges) return;

        boolean hasParent = this.djs$parentUUID != null || (this.level().isClientSide() && this.djs$clientLinkParent != -1);
        if (DJsConfig.getInstance().enableMinecartChanges && hasParent) cir.setReturnValue(false);
    }

    /**
     * Inject code to prevent vanilla physics from applying to follower minecarts.
     */
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 disableFollowerVanillaMovement(Vec3 vec3) {
        //Check if minecart changes are enabled in configs
        if (!DJsConfig.getInstance().enableMinecartChanges) return vec3;

        //If this is a follower, let the vanilla method run, but force it to travel 0 blocks
        if (this.djs$parentUUID != null || (this.level().isClientSide() && this.djs$clientLinkParent != -1)) return Vec3.ZERO;
        return vec3;
    }

    /**
     * Inject code at the end of each tick, applying custom hopping physic.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void applyHopPhysics(CallbackInfo ci) {
        //Check if custom minecart hopping physics are enabled in configs
        if (!DJsConfig.getInstance().enableCustomMinecartHoppingPhysics || this.level().isClientSide()) return;

        boolean currentlyOnRail = this.isOnRails();
        Vec3 delta = this.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        //Check if minecart is still on rails
        if (currentlyOnRail) {
            //Get the rail state
            BlockPos pos = this.blockPosition();
            BlockState state = this.level().getBlockState(pos);
            if (state.getBlock() instanceof BaseRailBlock rail) {
                RailShape shape = state.getValue(rail.getShapeProperty());

                //If on an ascending rail and moving fast enough, prime memory buffer
                if (shape.isSlope() && horizontalSpeed > 0.1) this.djs$rampBuffer = 3;
                else if (this.djs$rampBuffer > 0) this.djs$rampBuffer--;
            }
            else if (this.djs$rampBuffer > 0) this.djs$rampBuffer--;
        }
        //Otherwise minecart is already in the air
        else {
            if (this.djs$rampBuffer > 0) {
                if (horizontalSpeed > 0.1) {
                    //Convert horizontal movement into vertical lift
                    this.setDeltaMovement(delta.x * 1.05, (horizontalSpeed * 0.95) + 0.2, delta.z * 1.05);
                    this.setOnGround(false);
                }
                this.djs$rampBuffer = 0;
            }

            //Pitch minecart in air
            if (!this.onGround() && horizontalSpeed > 0.05) {
                //Calculate true physics angle
                float truePitch = (float) -(Math.atan2(this.getDeltaMovement().y, horizontalSpeed) * 180.0 / Math.PI);

                //Exaggerate the pitch for a more dramatic tilt, capped so it doesn't flip upside down
                float targetPitch = Mth.clamp(truePitch * 1.5F, -90.0F, 90.0F);

                //Set the minecart rotation
                this.setXRot(Mth.lerp(0.4F, this.getXRot(), targetPitch));
            }
        }
    }

    /**
     * Inject code after saving data, saving minecart link data.
     */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveLinkData(ValueOutput output, CallbackInfo ci) {
        //Check if minecart changes are enabled in configs
        if (!DJsConfig.getInstance().enableMinecartChanges) return;

        //Save link data
        if (this.djs$parentUUID != null) output.putString("djs_parent_uuid", this.djs$parentUUID.toString());
        if (this.djs$childUUID != null) output.putString("djs_child_uuid", this.djs$childUUID.toString());
        if (this.djs$chainToChild != null) output.putString("djs_chain_to_child", this.djs$chainToChild);
    }

    /**
     * Inject code after reading saved data, reading minecart link data.
     */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadLinkData(ValueInput input, CallbackInfo ci) {
        //Check if minecart changes are enabled in configs
        if (!DJsConfig.getInstance().enableMinecartChanges) return;

        //Load link data
        input.getString("djs_parent_uuid").ifPresent(id -> this.djs$parentUUID = UUID.fromString(id));
        input.getString("djs_child_uuid").ifPresent(id -> this.djs$childUUID = UUID.fromString(id));
        input.getString("djs_chain_to_child").ifPresent(name -> this.djs$chainToChild = name);
    }

    /**
     * Inject method at the end of each tick, implementing minecart linking, parent and child relationship, and train physics logic.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void processPhysicsModification(CallbackInfo ci) {
        //Call custom physics for everything but furnace minecarts (furnace minecarts handle this automatically)
        if (!this.isFurnace()) this.djs$tickCustomPhysics();

        //Check for pending link
        if (!this.level().isClientSide() && this.getId() == DJsFixedProgression.pendingLinkMinecartId) {
            //Search for player linking minecart
            UUID linkedPlayerId = null;
            for (Map.Entry<UUID, UUID> entry : DJsFixedProgression.PENDING_LINKS.entrySet()) {
                if (entry.getValue().equals(this.getUUID())) {
                    linkedPlayerId = entry.getKey();
                    break;
                }
            }

            if (linkedPlayerId != null) {
                //Get the player and chain status
                Player player = this.level().getPlayerByUUID(linkedPlayerId);
                boolean hasChain = false;

                if (player != null) {
                    //Check if the player is still holding a chain
                    String mainItem = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
                    String offItem = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
                    hasChain = mainItem.contains("_chain") || offItem.contains("_chain");
                }

                //If player disconnected, walked too far away, or swapped off the chain item, cancel the pending link
                if (player == null || player.distanceTo(this) > 6.0D || !hasChain) {
                    DJsFixedProgression.PENDING_LINKS.remove(linkedPlayerId);
                    DJsFixedProgression.pendingLinkMinecartId = -1;

                    if (player != null) player.displayClientMessage(Component.literal("Link cancelled"), true);

                }
            }
        }
    }

    /**
     * Method that applies custom physics for follower and leader minecarts of a train.
     */
    @Override public void djs$tickCustomPhysics() {
        //Check if minecart changes are enabled in configs
        if (!DJsConfig.getInstance().enableMinecartChanges) return;

        //Record movement history for all minecarts in the train
        Vec3 currentPos = this.position();
        Vec3 delta = this.getDeltaMovement();
        double speed = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        //Server side logic
        if (!this.level().isClientSide()) {
            //Destroy minecart when a magma block is under the rail
            if (this.isOnRails()) {
                BlockState blockBelow = this.level().getBlockState(this.blockPosition().below());
                if (blockBelow.is(Blocks.MAGMA_BLOCK)) {
                    //Destroys the minecart, safely dropping its item and any chest/hopper inventory
                    this.hurtServer((ServerLevel) this.level(), this.damageSources().generic(), 1000.0F);
                    return;
                }
            }

            //Keep a 3-chunk radius loaded around the minecart while it is moving (refreshes every second)
            if (!this.level().isClientSide() && speed > 0.01 && this.tickCount % 20 == 0) ((ServerLevel) this.level()).getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, new ChunkPos(this.blockPosition()), 3);
        }

        //Dynamically update the furnace's push vector to match its actual movement
        if (this.isFurnace() && this.djs$isFurnaceFueled() && speed > 0.01) {
            Vec3 moveDir = new Vec3(delta.x, 0, delta.z).normalize();
            this.djs$setFurnaceDirection(moveDir);
        }

        //Get the last step from minecart history
        MinecartStep lastStep = this.djs$history.peekFirst();

        //Add new history step
        if (lastStep == null || lastStep.pos().distanceToSqr(currentPos) > 0.0004) {
            this.djs$history.addFirst(new MinecartStep(currentPos, this.getYRot(), this.getXRot()));
            //Delete the oldest entry in track history if size becomes too large
            if (this.djs$history.size() > 100) this.djs$history.removeLast();
        }

        //Save the train as a list
        List<AbstractMinecart> train = this.djs$getTrainMinecarts(this.level());

        //Single furnace minecart functionality
        if (train.size() <= 1) {
            //Check if the furnace minecart is fueled
            if (this.isFurnace() && this.djs$isFurnaceFueled()) {
                boolean braking = false;

                //Check if the furnace minecart is on rails
                if (this.isOnRails()) {
                    //If the rail is an unpowered rail, pause the furnace minecart
                    BlockState state = this.level().getBlockState(this.blockPosition());
                    if (state.getBlock() instanceof PoweredRailBlock && !state.getValue(PoweredRailBlock.POWERED)) braking = true;
                }
                this.djs$setFurnacePaused(braking);
                //Cancel all furnace minecart movement when braking
                if (braking) this.setDeltaMovement(Vec3.ZERO);
                //Wall bounce reversal
                else if (this.horizontalCollision && speed < 0.05) {
                    Vec3 fPush = this.djs$getFurnaceDirection();
                    this.djs$setFurnaceDirection(fPush.scale(-1));
                    this.setDeltaMovement(delta.scale(-0.5));
                }
                //Ensure train is on rails
                else if (this.isOnRails()) {
                    //Check if train is still moving
                    if (speed > 0.01) {
                        Vec3 railGuidedDir = new Vec3(delta.x, 0, delta.z).normalize();
                        this.setDeltaMovement(railGuidedDir.scale(0.8).add(0, delta.y, 0));
                    }
                    else {
                        Vec3 outwardDir = this.djs$getFurnaceDirection().normalize();
                        if (this.isOnRails()) outwardDir = new Vec3(outwardDir.x, 0, outwardDir.z).normalize();
                        this.setDeltaMovement(outwardDir.scale(0.1).add(0, delta.y, 0));
                    }
                }
            }
            return;
        }

        //Check if this is the absolute parent of the train
        AbstractMinecart absoluteParent = train.getFirst();
        boolean amITheParent = this.getUUID().equals(absoluteParent.getUUID());

        //If this is the parent, follow custom parent physics
        if (amITheParent) {
            AbstractMinecart activeFurnace = null;
            boolean isBraking = false;

            //Check every minecart in the train
            for (AbstractMinecart minecart : train) {
                //Check if the minecart is on rails
                if (minecart.isOnRails()) {
                    //If the rail is an unpowered rail, pause the train
                    BlockState state = minecart.level().getBlockState(minecart.blockPosition());
                    if (state.getBlock() instanceof PoweredRailBlock && !state.getValue(PoweredRailBlock.POWERED)) isBraking = true;
                }
                //Check if the minecart is a furnace minecart
                if (((ILinkableMinecart) minecart).djs$isFurnace() && ((ILinkableMinecart) minecart).djs$isFurnaceFueled()) activeFurnace = minecart;
            }
            //Pause every minecart in train if braking
            for (AbstractMinecart minecart : train) ((ILinkableMinecart) minecart).djs$setFurnacePaused(isBraking);

            //If braking, stop the train's movement
            if (isBraking) this.setDeltaMovement(Vec3.ZERO);
            //Check if a fueled furnace minecart exits in the train
            else if (activeFurnace != null) {
                //Wall bounce reversal
                if (this.horizontalCollision && speed < 0.05) {
                    Vec3 fPush = ((ILinkableMinecart)activeFurnace).djs$getFurnaceDirection();
                    ((ILinkableMinecart)activeFurnace).djs$setFurnaceDirection(fPush.scale(-1));
                    this.setDeltaMovement(delta.scale(-0.5));
                }
                //Ensure train is on rails
                else if (this.isOnRails()) {
                    //Check if the train is moving
                    if (speed > 0.01) {
                        //Maintain the same direction when moving (locked direction)
                        Vec3 railGuidedDir = new Vec3(delta.x, 0, delta.z).normalize();
                        this.setDeltaMovement(railGuidedDir.scale(0.8).add(0, delta.y, 0));
                    }
                    //Check if the train is stopped
                    else {
                        //Determine whether to push parent forward or backward
                        Vec3 trainForward = this.position().subtract(train.get(1).position()).normalize();

                        int fIndex = train.indexOf(activeFurnace);
                        AbstractMinecart fTarget = (fIndex == 0) ? train.get(1) : train.get(fIndex - 1);
                        Vec3 fForward = (fIndex == 0) ? activeFurnace.position().subtract(fTarget.position()).normalize() : fTarget.position().subtract(activeFurnace.position()).normalize();
                        Vec3 fPush = ((ILinkableMinecart) activeFurnace).djs$getFurnaceDirection().normalize();

                        //Fallback
                        if (fPush.lengthSqr() < 0.01) fPush = new Vec3(1, 0, 0);

                        Vec3 kickstartDir;

                        //Push forward
                        if (fPush.dot(fForward) > 0) kickstartDir = trainForward;

                            //Push backward (wall bounce recovery)
                        else kickstartDir = trainForward.scale(-1);

                        if (this.isOnRails()) kickstartDir = new Vec3(kickstartDir.x, 0, kickstartDir.z).normalize();
                        this.setDeltaMovement(kickstartDir.scale(0.3).add(0, delta.y, 0));
                    }
                }
            }
        }
        //Otherwise this is a follower, follow follower physics
        else {
            this.setDeltaMovement(Vec3.ZERO);
            int myIndex = train.indexOf((AbstractMinecart)(Object)this);

            //Follow the target minecart (the minecart ahead in the train)
            AbstractMinecart target = train.get(myIndex - 1);
            double idealTargetDistance = 1.3;

            //Get the target minecart's movement history
            Deque<MinecartStep> history = ((ILinkableMinecart) target).djs$getHistory();
            if (history != null && !history.isEmpty()) {
                double accum = 0;
                MinecartStep prev = null;
                boolean found = false;

                //Search through the minecart history
                for (MinecartStep step : history) {
                    //Find the best step that maintains the desired target distance
                    if (prev != null) {
                        double dist = prev.pos().distanceTo(step.pos());
                        if (accum + dist >= idealTargetDistance) {
                            if (dist > 0.0001) {
                                double fraction = (idealTargetDistance - accum) / dist;
                                Vec3 exactPos = prev.pos().lerp(step.pos(), fraction);
                                this.setPos(exactPos.x, exactPos.y, exactPos.z);
                                this.setYRot(Mth.lerp((float)fraction, prev.yRot(), step.yRot()));
                                this.setXRot(Mth.lerp((float)fraction, prev.xRot(), step.xRot()));
                            } else {
                                this.setPos(step.pos().x, step.pos().y, step.pos().z);
                                this.setYRot(step.yRot());
                                this.setXRot(step.xRot());
                            }
                            //Assign momentum visually so the client renders it smoothly
                            this.setDeltaMovement(target.getDeltaMovement());
                            found = true;
                            break;
                        }
                        accum += dist;
                    }
                    prev = step;
                }

                //If the history is missing
                if (!found) {
                    Vec3 targetVel = target.getDeltaMovement();

                    //Create a rigid directional stick between minecarts
                    Vec3 toMe = this.position().subtract(target.position());

                    //Prevent division by 0 if coordinates are the same
                    if (toMe.lengthSqr() < 0.0001) toMe = new Vec3(0, 0, 1);

                    //Calculate the ideal position
                    Vec3 idealPos = target.position().add(toMe.normalize().scale(idealTargetDistance));

                    //Lerp to the ideal position to aggressively and smoothly close the gap
                    Vec3 newPos = this.position().lerp(idealPos, 0.5);
                    this.setPos(newPos.x, newPos.y, newPos.z);

                    //Assign momentum visually so the client renders it smoothly
                    this.setDeltaMovement(targetVel);

                    //Copy parent rotation
                    this.setYRot(target.getYRot());
                    this.setXRot(target.getXRot());
                }
            }
        }
    }

    /**
     * Inject code when a minecart is destroyed or removed, cleanly severing links and dropping chains.
     */
    @Override
    public void remove(Entity.@NotNull RemovalReason removalReason) {
        if (DJsConfig.getInstance().enableMinecartChanges && !this.level().isClientSide()) {
            if (this.djs$parentUUID != null) {
                Entity parent = this.level().getEntity(this.djs$parentUUID);
                if (parent != null) {
                    String chain = ((ILinkableMinecart) parent).djs$getChainToChild();
                    ((ILinkableMinecart) parent).djs$setChild(null);
                    ((ILinkableMinecart) parent).djs$setChainToChild(null);
                    DJsFixedProgression.syncMinecartLinks(parent, (ServerLevel) this.level());

                    if (chain != null) this.spawnAtLocation((ServerLevel) this.level(), BuiltInRegistries.ITEM.getValue(Identifier.parse(chain)));
                }
            }
            if (this.djs$childUUID != null) {
                Entity child = this.level().getEntity(this.djs$childUUID);
                if (child != null) {
                    ((ILinkableMinecart) child).djs$setParent(null);
                    DJsFixedProgression.syncMinecartLinks(child, (ServerLevel) this.level());
                }
                if (this.djs$chainToChild != null) this.spawnAtLocation((ServerLevel) this.level(), BuiltInRegistries.ITEM.getValue(Identifier.parse(this.djs$chainToChild)));
            }
        }
        super.remove(removalReason);
    }
}