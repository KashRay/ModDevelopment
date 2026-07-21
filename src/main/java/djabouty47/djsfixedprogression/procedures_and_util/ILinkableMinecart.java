package djabouty47.djsfixedprogression.procedures_and_util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public interface ILinkableMinecart {
    //Strict path history recorded only by the absolute parent
    record MinecartStep(Vec3 pos, float yRot, float xRot) {}
    Deque<MinecartStep> djs$getHistory();

    //Strict directed graph topology
    void djs$setParent(UUID uuid);
    UUID djs$getParent();
    void djs$setChild(UUID uuid);
    UUID djs$getChild();
    void djs$setChainToChild(String name);
    String djs$getChainToChild();

    //Furnace minecart communication
    default boolean djs$isFurnace() { return false; }
    default boolean djs$isFurnaceFueled() { return false; }
    default Vec3 djs$getFurnaceDirection() { return Vec3.ZERO; }
    default void djs$setFurnaceDirection(Vec3 dir) {}
    default void djs$setFurnacePaused(boolean paused) {}
    default void djs$extinguish() {}
    default void djs$addFuel(Vec3 direction) {}

    //Custom engine execution
    void djs$tickCustomPhysics();

    //Client-side tracking for the renderer
    void djs$setClientLinkParent(int id);
    int djs$getClientLinkParent();
    void djs$setClientLinkChild(int id);
    int djs$getClientLinkChild();


    /**
     * Method to build the definitive train array. Always starts with the absolute parent.
     */
    default List<AbstractMinecart> djs$getTrainMinecarts(Level level) {
        //Walk up the train to find the train leader
        AbstractMinecart current = (AbstractMinecart) this;
        while (true) {
            Entity parentEntity = null;
            if (!level.isClientSide() && ((ILinkableMinecart) current).djs$getParent() != null) parentEntity = level.getEntity(((ILinkableMinecart) current).djs$getParent());
            else if (level.isClientSide() && ((ILinkableMinecart) current).djs$getClientLinkParent() != -1) parentEntity = level.getEntity(((ILinkableMinecart) current).djs$getClientLinkParent());

            if (parentEntity instanceof AbstractMinecart) current = (AbstractMinecart) parentEntity;
            else break;
        }

        //Walk down the train to compile the rigid list
        List<AbstractMinecart> train = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        while (current != null && !visited.contains(current.getId())) {
            train.add(current);
            visited.add(current.getId());

            Entity childEntity = null;
            if (!level.isClientSide() && ((ILinkableMinecart) current).djs$getChild() != null) childEntity = level.getEntity(((ILinkableMinecart) current).djs$getChild());
            else if (level.isClientSide() && ((ILinkableMinecart) current).djs$getClientLinkChild() != -1) childEntity = level.getEntity(((ILinkableMinecart) current).djs$getClientLinkChild());

            if (childEntity instanceof AbstractMinecart) current = (AbstractMinecart) childEntity;
            else break;
        }
        return train;
    }
}