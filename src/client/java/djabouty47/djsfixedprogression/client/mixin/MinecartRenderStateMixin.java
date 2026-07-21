package djabouty47.djsfixedprogression.client.mixin;

import djabouty47.djsfixedprogression.procedures_and_util.ILinkableMinecartState;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecartRenderState.class)
public class MinecartRenderStateMixin implements ILinkableMinecartState {
    @Unique private int djs$myId = -1;
    @Unique private int djs$linkAId = -1;
    @Unique private double djs$linkAX = 0, djs$linkAY = 0, djs$linkAZ = 0;
    @Unique private int djs$linkBId = -1;
    @Unique private double djs$linkBX = 0, djs$linkBY = 0, djs$linkBZ = 0;
    @Unique private float djs$myYRot = 0, djs$myXRot = 0;
    @Unique private boolean djs$hasHandTarget = false;
    @Unique private double djs$handX = 0, djs$handY = 0, djs$handZ = 0;

    @Override public void djs$setMyId(int id) { this.djs$myId = id; }
    @Override public int djs$getMyId() { return this.djs$myId; }

    @Override public void djs$setLinkParent(int id, double x, double y, double z) { this.djs$linkAId = id; this.djs$linkAX = x; this.djs$linkAY = y; this.djs$linkAZ = z; }
    @Override public int djs$getLinkParentId() { return this.djs$linkAId; }
    @Override public double djs$getLinkParentX() { return this.djs$linkAX; }
    @Override public double djs$getLinkParentY() { return this.djs$linkAY; }
    @Override public double djs$getLinkParentZ() { return this.djs$linkAZ; }

    @Override public void djs$setLinkChild(int id, double x, double y, double z) { this.djs$linkBId = id; this.djs$linkBX = x; this.djs$linkBY = y; this.djs$linkBZ = z; }
    @Override public int djs$getLinkChildId() { return this.djs$linkBId; }
    @Override public double djs$getLinkChildX() { return this.djs$linkBX; }
    @Override public double djs$getLinkChildY() { return this.djs$linkBY; }
    @Override public double djs$getLinkChildZ() { return this.djs$linkBZ; }
    @Override public void djs$setMyRot(float yRot, float xRot) { this.djs$myYRot = yRot; this.djs$myXRot = xRot; }
    @Override public float djs$getMyYRot() { return this.djs$myYRot; }
    @Override public float djs$getMyXRot() { return this.djs$myXRot; }

    @Override public void djs$setHandTarget(boolean active, double x, double y, double z) { this.djs$hasHandTarget = active; this.djs$handX = x; this.djs$handY = y; this.djs$handZ = z; }
    @Override public boolean djs$hasHandTarget() { return this.djs$hasHandTarget; }
    @Override public double djs$getHandX() { return this.djs$handX; }
    @Override public double djs$getHandY() { return this.djs$handY; }
    @Override public double djs$getHandZ() { return this.djs$handZ; }
}
