package djabouty47.djsfixedprogression.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import djabouty47.djsfixedprogression.DJsFixedProgression;
import djabouty47.djsfixedprogression.config.DJsConfig;
import djabouty47.djsfixedprogression.procedures_and_util.ILinkableMinecart;
import djabouty47.djsfixedprogression.procedures_and_util.ILinkableMinecartState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecartRenderer.class)
public abstract class AbstractMinecartRendererMixin {

    @Shadow
    private static <S extends MinecartRenderState> void oldRender(S minecartRenderState, PoseStack poseStack) {
        throw new UnsupportedOperationException("Implemented via mixin");
    }

    /**
     * Inject code to safely calculate the parent's position and pass it to the state object.
     */
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;Lnet/minecraft/client/renderer/entity/state/MinecartRenderState;F)V", at = @At("TAIL"))
    private void extractChainState(AbstractMinecart abstractMinecart, MinecartRenderState minecartRenderState, float partialTicks, CallbackInfo ci) {
        //Check if minecart changes are enabled in configs
        if (!DJsConfig.getInstance().enableMinecartChanges) return;

        ILinkableMinecart linkable = (ILinkableMinecart) abstractMinecart;
        ILinkableMinecartState state = (ILinkableMinecartState) minecartRenderState;

        state.djs$setMyId(abstractMinecart.getId());
        state.djs$setMyId(abstractMinecart.getId());
        state.djs$setMyRot(abstractMinecart.getYRot(), abstractMinecart.getXRot());

        //Extract link A
        int idA = linkable.djs$getClientLinkParent();
        if (idA != -1 && abstractMinecart.level() != null) {
            Entity targetA = abstractMinecart.level().getEntity(idA);
            if (targetA != null) state.djs$setLinkParent(idA, Mth.lerp(partialTicks, targetA.xo, targetA.getX()), Mth.lerp(partialTicks, targetA.yo, targetA.getY()), Mth.lerp(partialTicks, targetA.zo, targetA.getZ()));
            else state.djs$setLinkParent(-1, 0, 0, 0);
        }
        else state.djs$setLinkParent(-1, 0, 0, 0);

        //Extract Link B
        int idB = linkable.djs$getClientLinkChild();
        if (idB != -1 && abstractMinecart.level() != null) {
            Entity targetB = abstractMinecart.level().getEntity(idB);
            if (targetB != null) state.djs$setLinkChild(idB, Mth.lerp(partialTicks, targetB.xo, targetB.getX()), Mth.lerp(partialTicks, targetB.yo, targetB.getY()), Mth.lerp(partialTicks, targetB.zo, targetB.getZ()));
            else state.djs$setLinkChild(-1, 0, 0, 0);
        }
        else state.djs$setLinkChild(-1, 0, 0, 0);

        //Pending minecart linking
        if (abstractMinecart.getId() == DJsFixedProgression.pendingLinkMinecartId) {
            LocalPlayer player = Minecraft.getInstance().player;

            if (player != null) {
                //Ensure the player is still holding a chain
                String mainItem = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
                String offItem = BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).toString();
                boolean hasChain = mainItem.contains("chain") || offItem.contains("chain");

                if (hasChain) {
                    //Approximate hand position
                    double handX = Mth.lerp(partialTicks, player.xo, player.getX());
                    double handY = Mth.lerp(partialTicks, player.yo, player.getY()) + 1.0D;
                    double handZ = Mth.lerp(partialTicks, player.zo, player.getZ());
                    state.djs$setHandTarget(true, handX, handY, handZ);
                }
            }
            else state.djs$setHandTarget(false, 0, 0, 0);
        }
        else state.djs$setHandTarget(false, 0, 0, 0);
    }

    /**
     * Inject code to read the state object and push vertices to the GPU.
     */
    @Inject(method = "submit*", at = @At("HEAD"))
    private void submitRigidChain(MinecartRenderState minecartRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        //Check if minecart changes are enabled in configs
        if (!DJsConfig.getInstance().enableMinecartChanges) return;

        ILinkableMinecartState state = (ILinkableMinecartState) minecartRenderState;

        //Draw chains
        drawChainToTarget(state.djs$getMyId(), state.djs$getLinkParentId(), state.djs$getLinkParentX(), state.djs$getLinkParentY(), state.djs$getLinkParentZ(), minecartRenderState, poseStack, submitNodeCollector);
        drawChainToTarget(state.djs$getMyId(), state.djs$getLinkChildId(), state.djs$getLinkChildX(), state.djs$getLinkChildY(), state.djs$getLinkChildZ(), minecartRenderState, poseStack, submitNodeCollector);

        //Draw chain to player's hand
        if (state.djs$hasHandTarget()) drawChainToTarget(state.djs$getMyId(), Integer.MAX_VALUE, state.djs$getHandX(), state.djs$getHandY(), state.djs$getHandZ(), minecartRenderState, poseStack, submitNodeCollector);
    }

    /**
     * Method to draw chain between linked minecarts.
     */
    @Unique private void drawChainToTarget(int myId, int targetId, double tX, double tY, double tZ, MinecartRenderState minecartRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        //Only render if ID is smaller than target ID
        if (targetId != -1 && myId < targetId) {
            double dX = tX - minecartRenderState.x;
            double dY = tY - minecartRenderState.y;
            double dZ = tZ - minecartRenderState.z;

            float dist = (float) Math.sqrt(dX * dX + dY * dY + dZ * dZ);
            //Prevent NaN crash if minecarts overlap
            if (dist < 0.01F) return;

            poseStack.pushPose();

            //Shift up to attach to minecart body
            float offsetY = 0.375F;
            poseStack.translate(0, offsetY, 0);

            //Calculate the absolute global direction to the target
            Vector3f globalDir = new Vector3f((float)dX, (float)dY, (float)dZ).normalize();

            //Point perfectly at the target
            Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), globalDir);

            poseStack.mulPose(rotation);

            //Borrow chain block texture
            Identifier CHAIN_TEXTURE = Identifier.withDefaultNamespace("textures/block/iron_chain.png");

            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(CHAIN_TEXTURE), (pose, vertexConsumer) -> {
                Matrix4f matrix4f = pose.pose();

                //Get light coords and overlay
                int light = minecartRenderState.lightCoords;
                int overlay = OverlayTexture.NO_OVERLAY;

                //Chain dimensions
                float halfWidth = 0.10F;
                float startZ = 0.45F;
                float endZ = dist - 0.45F;

                if (endZ > startZ) {
                    //Multiply length
                    float vMax = endZ - startZ;

                    //Account for chain texture clear pixels
                    float uMax = 3.0F / 16.0F;

                    //Offset plane 2 vertically so links interlock properly
                    float vOffset = 0.5F;

                    //Draw plane 1 (vertical)
                    vertexConsumer.addVertex(matrix4f, -halfWidth, 0, startZ).setColor(255, 255, 255, 255).setUv(0, 0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                    vertexConsumer.addVertex(matrix4f, halfWidth, 0, startZ).setColor(255, 255, 255, 255).setUv(uMax, 0).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                    vertexConsumer.addVertex(matrix4f, halfWidth, 0, endZ).setColor(255, 255, 255, 255).setUv(uMax, vMax).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);
                    vertexConsumer.addVertex(matrix4f, -halfWidth, 0, endZ).setColor(255, 255, 255, 255).setUv(0, vMax).setOverlay(overlay).setLight(light).setNormal(1, 0, 0);

                    //Draw plane 2 (horizontal)
                    vertexConsumer.addVertex(matrix4f, 0, -halfWidth, startZ).setColor(255, 255, 255, 255).setUv(0, vOffset).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                    vertexConsumer.addVertex(matrix4f, 0, halfWidth, startZ).setColor(255, 255, 255, 255).setUv(uMax, vOffset).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                    vertexConsumer.addVertex(matrix4f, 0, halfWidth, endZ).setColor(255, 255, 255, 255).setUv(uMax, vMax + vOffset).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                    vertexConsumer.addVertex(matrix4f, 0, -halfWidth, endZ).setColor(255, 255, 255, 255).setUv(0, vMax + vOffset).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
                }
            });
            poseStack.popPose();
        }
    }
}