package io.github.diamondsmp.platform.paper.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class OwnerRecoveryExportService {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JavaPlugin plugin;

    public OwnerRecoveryExportService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public File createRecoveryArchive() throws IOException {
        File serverRoot = plugin.getServer().getWorldContainer().getAbsoluteFile();
        File exportDir = new File(plugin.getDataFolder(), "exports");
        exportDir.mkdirs();
        File output = new File(exportDir, "owner-recovery-" + TIMESTAMP.format(LocalDateTime.now()) + ".zip");
        Set<File> roots = collectRoots(serverRoot);
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(output))) {
            for (File root : roots) {
                addPath(zip, root, serverRoot, output);
            }
        }
        return output;
    }

    private Set<File> collectRoots(File serverRoot) {
        Set<File> roots = new LinkedHashSet<>();
        for (String name : new String[] {
            "server.properties",
            "bukkit.yml",
            "spigot.yml",
            "paper-global.yml",
            "paper-world-defaults.yml",
            "permissions.yml",
            "ops.json",
            "whitelist.json",
            "banned-ips.json",
            "banned-players.json",
            "usercache.json",
            "eula.txt"
        }) {
            File candidate = new File(serverRoot, name);
            if (candidate.exists()) {
                roots.add(candidate);
            }
        }
        File pluginsDir = new File(serverRoot, "plugins");
        if (pluginsDir.exists()) {
            roots.add(pluginsDir);
        }
        for (World world : Bukkit.getWorlds()) {
            File folder = world.getWorldFolder();
            if (folder.exists()) {
                roots.add(folder);
            }
        }
        return roots;
    }

    private void addPath(ZipOutputStream zip, File file, File serverRoot, File output) throws IOException {
        if (!file.exists()) {
            return;
        }
        String outputPath = output.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        if (filePath.equals(outputPath) || filePath.startsWith(outputPath + File.separator)) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null || children.length == 0) {
                String entryName = relativize(serverRoot, file) + "/";
                zip.putNextEntry(new ZipEntry(entryName));
                zip.closeEntry();
                return;
            }
            for (File child : children) {
                addPath(zip, child, serverRoot, output);
            }
            return;
        }
        ZipEntry entry = new ZipEntry(relativize(serverRoot, file));
        zip.putNextEntry(entry);
        try (FileInputStream input = new FileInputStream(file)) {
            input.transferTo(zip);
        }
        zip.closeEntry();
    }

    private String relativize(File serverRoot, File file) throws IOException {
        String rootPath = serverRoot.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        if (filePath.startsWith(rootPath + File.separator)) {
            return filePath.substring(rootPath.length() + 1).replace(File.separatorChar, '/');
        }
        return file.getName();
    }
}
