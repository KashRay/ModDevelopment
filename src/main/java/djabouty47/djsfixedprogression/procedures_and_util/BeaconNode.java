package djabouty47.djsfixedprogression.procedures_and_util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record BeaconNode(String name, String id, int pathId, int x, int y, String description, List<NodeCost> costs, ItemStack icon,
                         List<String> parents, List<String> mutuallyExclusive, boolean isRepeatable,
                         boolean isActiveAbility) {
    public BeaconNode {
        if (costs.size() > 3) throw new IllegalArgumentException("A Beacon node cannot have more than 3 costs");
    }

    public record NodeCost(Item item, int count) { }
}

