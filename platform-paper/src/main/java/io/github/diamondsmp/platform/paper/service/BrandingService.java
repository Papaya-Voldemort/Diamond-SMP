package io.github.diamondsmp.platform.paper.service;

import io.github.diamondsmp.platform.paper.config.PluginSettings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BrandingService {
    private static final List<ResourceCopy> COMPANION_PLUGIN_RESOURCES = List.of(
        new ResourceCopy("branding/TAB/config.yml", "TAB/config.yml"),
        new ResourceCopy("branding/TAB/groups.yml", "TAB/groups.yml"),
        new ResourceCopy("branding/TAB/messages.yml", "TAB/messages.yml"),
        new ResourceCopy("branding/TAB/animations.yml", "TAB/animations.yml"),
        new ResourceCopy("branding/CustomJoinMessages/config.yml", "CustomJoinMessages/config.yml"),
        new ResourceCopy("branding/CustomJoinMessages/messages/chat.yml", "CustomJoinMessages/messages/chat.yml"),
        new ResourceCopy("branding/CustomJoinMessages/messages/title.yml", "CustomJoinMessages/messages/title.yml"),
        new ResourceCopy("branding/CustomJoinMessages/messages/actionbar.yml", "CustomJoinMessages/messages/actionbar.yml"),
        new ResourceCopy("branding/CustomJoinMessages/messages/sound.yml", "CustomJoinMessages/messages/sound.yml"),
        new ResourceCopy("branding/CustomJoinMessages/messages/bossbar.yml", "CustomJoinMessages/messages/bossbar.yml")
    );

    private final JavaPlugin plugin;
    private final PluginSettings.Branding settings;

    public BrandingService(JavaPlugin plugin, PluginSettings.Branding settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void applyStartupBranding() {
        if (!settings.enabled()) {
            return;
        }
        try {
            if (settings.syncServerIcon()) {
                copyBundledResource("branding/server-icon.png", serverRoot().resolve("server-icon.png"));
            }
            if (settings.syncCompanionPluginConfigs()) {
                syncCompanionPluginConfigs();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to sync Diamond SMP branding assets", exception);
        }
    }

    public String motd() {
        return settings.motd().lineOne() + "\n" + settings.motd().lineTwo();
    }

    private void syncCompanionPluginConfigs() throws IOException {
        Path pluginsDirectory = serverRoot().resolve("plugins");
        for (ResourceCopy resource : COMPANION_PLUGIN_RESOURCES) {
            Path target = pluginsDirectory.resolve(resource.relativeTargetPath());
            if (!Files.exists(target.getParent())) {
                continue;
            }
            copyBundledResource(resource.resourcePath(), target);
        }
    }

    private void copyBundledResource(String resourcePath, Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                throw new IOException("Missing bundled resource " + resourcePath);
            }
            Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path serverRoot() {
        return Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
    }

    private record ResourceCopy(String resourcePath, String relativeTargetPath) {}
}
