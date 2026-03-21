package io.github.diamondsmp.platform.paper.service;

import io.github.diamondsmp.platform.paper.config.PluginSettings;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class OwnerControlService {
    private final JavaPlugin plugin;
    private final String ownerName;
    private final Set<UUID> ownerUuids;
    private final File auditLogFile;
    private boolean partyEnabled;
    private boolean pvpEnabled;

    public OwnerControlService(JavaPlugin plugin, PluginSettings settings) {
        this.plugin = plugin;
        this.ownerName = settings.ownerControl().ownerName().trim();
        this.ownerUuids = loadOwnerUuids(settings.ownerControl());
        this.auditLogFile = new File(plugin.getDataFolder(), "owner-audit.log");
        this.partyEnabled = settings.pvp().enabled();
        this.pvpEnabled = settings.pvp().enabled();
    }

    public boolean isOwner(CommandSender sender) {
        return sender instanceof Player player && isOwner(player);
    }

    public boolean isOwner(Player player) {
        boolean nameMatches = ownerName.isBlank() || ownerName.equalsIgnoreCase(player.getName());
        boolean uuidMatches = ownerUuids.isEmpty() || ownerUuids.contains(player.getUniqueId());
        if (nameMatches && uuidMatches) {
            return true;
        }
        return false;
    }

    public boolean partyEnabled() {
        return partyEnabled;
    }

    public void setPartyEnabled(boolean partyEnabled) {
        this.partyEnabled = partyEnabled && pvpEnabled;
    }

    public boolean pvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
        if (!pvpEnabled) {
            this.partyEnabled = false;
        }
    }

    public boolean globalPvpEnabled() {
        return Bukkit.getWorlds().stream().allMatch(World::getPVP);
    }

    public void setGlobalPvp(boolean enabled) {
        Bukkit.getWorlds().forEach(world -> world.setPVP(enabled));
        plugin.getLogger().info("Owner control set global PvP to " + enabled + ".");
    }

    public void audit(Player player, String action) {
        String line = Instant.now() + " | " + player.getName() + " | " + player.getUniqueId() + " | " + action;
        plugin.getLogger().info("[owner-control] " + line);
        try {
            File parent = auditLogFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            Files.writeString(
                auditLogFile.toPath(),
                line + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to write owner audit log: " + exception.getMessage());
        }
    }

    private Set<UUID> loadOwnerUuids(PluginSettings.OwnerControl ownerControl) {
        Set<UUID> uuids = new HashSet<>();
        parseUuid(ownerControl.ownerUuid()).ifPresent(uuids::add);
        parseUuid(ownerControl.ownerUuidTrimmed()).ifPresent(uuids::add);
        return Set.copyOf(uuids);
    }

    private java.util.Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        String normalized = value.trim();
        if (normalized.length() == 32) {
            normalized = normalized.replaceFirst(
                "(?i)^([0-9a-f]{8})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{4})([0-9a-f]{12})$",
                "$1-$2-$3-$4-$5"
            );
        }
        try {
            return java.util.Optional.of(UUID.fromString(normalized));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Ignoring invalid owner-control.owner-uuid value.");
            return java.util.Optional.empty();
        }
    }
}
