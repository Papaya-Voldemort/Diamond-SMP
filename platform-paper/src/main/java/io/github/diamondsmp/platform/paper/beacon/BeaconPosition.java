package io.github.diamondsmp.platform.paper.beacon;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public record BeaconPosition(UUID worldId, int x, int y, int z) {
    public static BeaconPosition fromBlock(Block block) {
        return new BeaconPosition(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public static BeaconPosition fromLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("Location has no world");
        }
        return new BeaconPosition(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public String key() {
        return worldId + ":" + x + ":" + y + ":" + z;
    }
}
