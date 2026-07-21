package djabouty47.djsfixedprogression.procedures_and_util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeaconTracker {
    //Stores a list of active beacons for each dimension
    private static final Map<ResourceKey<@NotNull Level>, List<TrackedBeacon>> ACTIVE_BEACONS = new ConcurrentHashMap<>();

    public record TrackedBeacon(BlockPos pos, int radius, CompoundTag upgrades) {}

    /**
     * Method called by the beacon when it ticks to update its presence and range.
     */
    public static void updateBeacon(Level level, BlockPos pos, int layers, CompoundTag upgrades) {
        if (level.isClientSide()) return;

        //Add new beacon data
        ResourceKey<@NotNull Level> dimension = level.dimension();
        ACTIVE_BEACONS.putIfAbsent(dimension, new ArrayList<>());

        //Remove old beacon data
        List<TrackedBeacon> beacons = ACTIVE_BEACONS.get(dimension);
        beacons.removeIf(beacon -> beacon.pos().equals(pos));

        //Track layers, radius, and upgrades
        if (layers > 0 && !upgrades.isEmpty()) {
            int radius = (layers * 20 + 20);
            beacons.add(new TrackedBeacon(pos, radius, upgrades));
        }
    }

    /**
     * Method called when a beacon is broken, removing it from the tracker.
     */
    public static void removeBeacon(Level level, BlockPos pos) {
        if (level.isClientSide()) return;

        //Remove the beacon data
        List<TrackedBeacon> beacons = ACTIVE_BEACONS.get(level.dimension());
        if (beacons != null) beacons.removeIf(b -> b.pos().equals(pos));
    }

    /**
     * Method that quickly checks if a specific block position is under the influence of a beacon upgrade.
     */
    public static boolean hasUpgrade(Level level, BlockPos targetPos, String upgradeId) {
        //Get tracked beacons
        List<TrackedBeacon> beacons = ACTIVE_BEACONS.get(level.dimension());
        if (beacons == null || beacons.isEmpty()) return false;

        //Loop over all tracked beacons
        for (TrackedBeacon beacon : beacons) {
            //Check if the beacon has the specific upgrade
            if (beacon.upgrades().contains(upgradeId)) {
                //Check if target is within the square AABB range
                int dx = Math.abs(beacon.pos().getX() - targetPos.getX());
                int dz = Math.abs(beacon.pos().getZ() - targetPos.getZ());
                if (Math.max(dx, dz) <= beacon.radius()) return true;
            }
        }

        return false;
    }

    /**
     * Method that quickly checks if a specific block position is under the influence of an active beacon ability.
     */
    public static boolean isAbilityActive(Level level, BlockPos targetPos, String abilityId) {
        if (level.isClientSide()) return false;

        //Get tracked beacons
        List<TrackedBeacon> beacons = ACTIVE_BEACONS.get(level.dimension());
        if (beacons == null || beacons.isEmpty()) return false;

        //Get the current time
        long currentTime = level.getGameTime();

        //Loop over all tracked beacons
        for (TrackedBeacon beacon : beacons) {
            //Check if beacon has ability unlocked
            if (beacon.upgrades().contains(abilityId)) {
                //Get the ability duration
                long durationEnd = beacon.upgrades().getLongOr(abilityId + "_duration_end", 0);

                //If the ability is currently active, check if target is within the square AABB range
                if (currentTime < durationEnd) {
                    int dx = Math.abs(beacon.pos().getX() - targetPos.getX());
                    int dz = Math.abs(beacon.pos().getZ() - targetPos.getZ());
                    if (Math.max(dx, dz) <= beacon.radius()) return true;
                }
            }
        }
        return false;
    }
}
