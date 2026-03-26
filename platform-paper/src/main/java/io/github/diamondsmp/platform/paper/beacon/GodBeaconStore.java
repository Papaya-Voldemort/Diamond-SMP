package io.github.diamondsmp.platform.paper.beacon;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class GodBeaconStore {
    private final Path file;

    public GodBeaconStore(JavaPlugin plugin) {
        this.file = plugin.getDataFolder().toPath().resolve("god-beacons.yml");
    }

    public Map<BeaconPosition, GodBeaconRecord> load() {
        Map<BeaconPosition, GodBeaconRecord> records = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return records;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        for (String key : yaml.getConfigurationSection("beacons") == null ? java.util.Set.<String>of() : yaml.getConfigurationSection("beacons").getKeys(false)) {
            String root = "beacons." + key;
            UUID worldId = parseUuid(yaml.getString(root + ".world"));
            if (worldId == null) {
                continue;
            }
            BeaconPosition position = new BeaconPosition(
                worldId,
                yaml.getInt(root + ".x"),
                yaml.getInt(root + ".y"),
                yaml.getInt(root + ".z")
            );
            records.put(position, new GodBeaconRecord(
                position,
                GodBeaconTier.fromKey(yaml.getString(root + ".tier")),
                parseUuid(yaml.getString(root + ".owner")),
                parseInstant(yaml.getString(root + ".created-at")),
                GodBeaconEffect.fromKey(yaml.getString(root + ".primary")),
                GodBeaconEffect.fromKey(yaml.getString(root + ".secondary"))
            ));
        }
        return records;
    }

    public void save(Collection<GodBeaconRecord> records) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (GodBeaconRecord record : records) {
            String root = "beacons." + record.position().key();
            yaml.set(root + ".world", record.position().worldId().toString());
            yaml.set(root + ".x", record.position().x());
            yaml.set(root + ".y", record.position().y());
            yaml.set(root + ".z", record.position().z());
            yaml.set(root + ".tier", record.tier().key());
            yaml.set(root + ".owner", record.ownerId() == null ? null : record.ownerId().toString());
            yaml.set(root + ".created-at", record.createdAt().toString());
            yaml.set(root + ".primary", record.primaryEffect() == null ? null : record.primaryEffect().key());
            yaml.set(root + ".secondary", record.secondaryEffect() == null ? null : record.secondaryEffect().key());
        }
        writeAtomically(yaml.saveToString());
    }

    private void writeAtomically(String content) {
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(
                temp,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )) {
                channel.write(ByteBuffer.wrap(bytes));
                channel.force(true);
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save God Beacon data", exception);
        }
    }

    private static UUID parseUuid(String value) {
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Instant parseInstant(String value) {
        try {
            return value == null || value.isBlank() ? Instant.now() : Instant.parse(value);
        } catch (Exception exception) {
            return Instant.now();
        }
    }
}
