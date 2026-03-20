package io.github.diamondsmp.platform.paper.service;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class EndAccessService {
    private final JavaPlugin plugin;
    private final File stateFile;
    private boolean open;

    public EndAccessService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "state.yml");
        this.open = loadState();
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
        saveState();
        if (open) {
            activateLoadedPortals();
        } else {
            deactivateLoadedPortals();
            returnPlayersToOverworld();
        }
    }

    public void activateNearbyPortal(Block block) {
        if (!open || block.getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        for (int x = block.getX() - 2; x <= block.getX() + 2; x++) {
            for (int z = block.getZ() - 2; z <= block.getZ() + 2; z++) {
                activatePortalIfComplete(block.getWorld(), x, block.getY(), z);
            }
        }
    }

    public void enforcePlayerAccess(Player player) {
        if (open || player.getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }
        World overworld = Bukkit.getWorlds().stream()
            .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
            .findFirst()
            .orElse(Bukkit.getWorlds().getFirst());
        player.teleport(overworld.getSpawnLocation());
    }

    private boolean loadState() {
        if (!stateFile.exists()) {
            return false;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(stateFile);
        return config.getBoolean("end-open", false);
    }

    private void saveState() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("end-open", open);
        try {
            config.save(stateFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save state.yml: " + exception.getMessage());
        }
    }

    private void activateLoadedPortals() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                int minY = world.getMinHeight();
                int maxY = world.getMaxHeight();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = minY; y < maxY; y++) {
                            Block block = chunk.getBlock(x, y, z);
                            if (block.getType() == Material.END_PORTAL_FRAME) {
                                activateNearbyPortal(block);
                            }
                        }
                    }
                }
            }
        }
    }

    private void deactivateLoadedPortals() {
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                            Block block = chunk.getBlock(x, y, z);
                            if (block.getType() == Material.END_PORTAL) {
                                block.setType(Material.AIR, false);
                            }
                        }
                    }
                }
            }
        }
    }

    private void returnPlayersToOverworld() {
        Bukkit.getOnlinePlayers().forEach(this::enforcePlayerAccess);
    }

    private void activatePortalIfComplete(World world, int centerX, int y, int centerZ) {
        if (!hasFrame(world.getBlockAt(centerX - 2, y, centerZ - 1), BlockFace.EAST)
            || !hasFrame(world.getBlockAt(centerX - 2, y, centerZ), BlockFace.EAST)
            || !hasFrame(world.getBlockAt(centerX - 2, y, centerZ + 1), BlockFace.EAST)
            || !hasFrame(world.getBlockAt(centerX + 2, y, centerZ - 1), BlockFace.WEST)
            || !hasFrame(world.getBlockAt(centerX + 2, y, centerZ), BlockFace.WEST)
            || !hasFrame(world.getBlockAt(centerX + 2, y, centerZ + 1), BlockFace.WEST)
            || !hasFrame(world.getBlockAt(centerX - 1, y, centerZ - 2), BlockFace.SOUTH)
            || !hasFrame(world.getBlockAt(centerX, y, centerZ - 2), BlockFace.SOUTH)
            || !hasFrame(world.getBlockAt(centerX + 1, y, centerZ - 2), BlockFace.SOUTH)
            || !hasFrame(world.getBlockAt(centerX - 1, y, centerZ + 2), BlockFace.NORTH)
            || !hasFrame(world.getBlockAt(centerX, y, centerZ + 2), BlockFace.NORTH)
            || !hasFrame(world.getBlockAt(centerX + 1, y, centerZ + 2), BlockFace.NORTH)) {
            return;
        }

        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                Block block = world.getBlockAt(x, y, z);
                if (!block.getType().isAir() && block.getType() != Material.END_PORTAL) {
                    return;
                }
            }
        }

        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                world.getBlockAt(x, y, z).setType(Material.END_PORTAL, false);
            }
        }
    }

    private boolean hasFrame(Block block, BlockFace facing) {
        if (block.getType() != Material.END_PORTAL_FRAME) {
            return false;
        }
        if (!(block.getBlockData() instanceof EndPortalFrame frame)) {
            return false;
        }
        return frame.hasEye() && Objects.equals(frame.getFacing(), facing);
    }
}
