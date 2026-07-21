package djabouty47.djsfixedprogression.procedures_and_util;

public interface ILinkableMinecartState {
    void djs$setMyId(int id);
    int djs$getMyId();

    void djs$setLinkParent(int id, double x, double y, double z);
    int djs$getLinkParentId();
    double djs$getLinkParentX();
    double djs$getLinkParentY();
    double djs$getLinkParentZ();

    void djs$setLinkChild(int id, double x, double y, double z);
    int djs$getLinkChildId();
    double djs$getLinkChildX();
    double djs$getLinkChildY();
    double djs$getLinkChildZ();

    void djs$setMyRot(float yRot, float xRot);
    float djs$getMyYRot();
    float djs$getMyXRot();

    void djs$setHandTarget(boolean active, double x, double y, double z);
    boolean djs$hasHandTarget();
    double djs$getHandX();
    double djs$getHandY();
    double djs$getHandZ();
}
