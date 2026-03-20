package io.github.diamondsmp.platform.paper.service;

import io.github.diamondsmp.platform.paper.config.PluginSettings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BrandingService {
    private static final List<ResourceCopy> COMPANION_PLUGIN_RESOURCES = List.of(
        new ResourceCopy("placeholderapi", "branding/PlaceholderAPI/config.yml", "PlaceholderAPI/config.yml"),
        new ResourceCopy("tab", "branding/TAB/config.yml", "TAB/config.yml"),
        new ResourceCopy("tab", "branding/TAB/groups.yml", "TAB/groups.yml"),
        new ResourceCopy("tab", "branding/TAB/messages.yml", "TAB/messages.yml"),
        new ResourceCopy("tab", "branding/TAB/animations.yml", "TAB/animations.yml"),
        new ResourceCopy("custom-join-messages", "branding/CustomJoinMessages/config.yml", "CustomJoinMessages/config.yml"),
        new ResourceCopy("custom-join-messages", "branding/CustomJoinMessages/messages/chat.yml", "CustomJoinMessages/messages/chat.yml"),
        new ResourceCopy("custom-join-messages", "branding/CustomJoinMessages/messages/title.yml", "CustomJoinMessages/messages/title.yml"),
        new ResourceCopy("custom-join-messages", "branding/CustomJoinMessages/messages/actionbar.yml", "CustomJoinMessages/messages/actionbar.yml"),
        new ResourceCopy("custom-join-messages", "branding/CustomJoinMessages/messages/sound.yml", "CustomJoinMessages/messages/sound.yml"),
        new ResourceCopy("custom-join-messages", "branding/CustomJoinMessages/messages/bossbar.yml", "CustomJoinMessages/messages/bossbar.yml")
    );
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(20);

    private final JavaPlugin plugin;
    private final PluginSettings.Branding settings;
    private final HttpClient httpClient;

    public BrandingService(JavaPlugin plugin, PluginSettings.Branding settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(DOWNLOAD_TIMEOUT)
            .build();
    }

    public void applyStartupBranding() {
        if (!settings.enabled()) {
            return;
        }
        try {
            if (settings.syncServerIcon()) {
                copyBundledResource("branding/server-icon.png", serverRoot().resolve("server-icon.png"));
            }
            downloadCompanionPlugins();
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
            if (!shouldManageCompanion(resource.dependencyKey())) {
                continue;
            }
            Path target = pluginsDirectory.resolve(resource.relativeTargetPath());
            copyBundledResource(resource.resourcePath(), target);
        }
    }

    private void downloadCompanionPlugins() throws IOException {
        PluginSettings.CompanionDownloads downloads = settings.companionDownloads();
        if (!downloads.enabled()) {
            return;
        }
        downloadIfMissing(downloads.placeholderApi());
        downloadIfMissing(downloads.tab());
        downloadIfMissing(downloads.customJoinMessages());
    }

    private void downloadIfMissing(PluginSettings.CompanionDownload dependency) throws IOException {
        if (!dependency.enabled() || dependency.url().isBlank() || dependency.jarName().isBlank()) {
            return;
        }
        if (isPluginPresent(dependency)) {
            return;
        }
        Path target = serverRoot().resolve("plugins").resolve(dependency.jarName());
        Files.createDirectories(target.getParent());
        if (Files.exists(target) && Files.size(target) > 0L) {
            return;
        }
        plugin.getLogger().info("Downloading companion dependency " + dependency.pluginName() + " from " + dependency.url());
        HttpRequest request = HttpRequest.newBuilder(URI.create(dependency.url()))
            .timeout(DOWNLOAD_TIMEOUT)
            .GET()
            .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Download for " + dependency.pluginName() + " failed with HTTP " + response.statusCode());
            }
            try (InputStream body = response.body()) {
                Files.copy(body, target, StandardCopyOption.REPLACE_EXISTING);
            }
            plugin.getLogger().info("Downloaded " + dependency.jarName() + ". Restart the server to load the new plugin.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted for " + dependency.pluginName(), exception);
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

    private boolean shouldManageCompanion(String dependencyKey) {
        return switch (dependencyKey) {
            case "placeholderapi" -> settings.companionDownloads().placeholderApi().enabled();
            case "tab" -> settings.companionDownloads().tab().enabled();
            case "custom-join-messages" -> settings.companionDownloads().customJoinMessages().enabled();
            default -> false;
        };
    }

    private boolean isPluginPresent(PluginSettings.CompanionDownload dependency) throws IOException {
        if (Bukkit.getPluginManager().getPlugin(dependency.pluginName()) != null) {
            return true;
        }
        Path target = serverRoot().resolve("plugins").resolve(dependency.jarName());
        return Files.exists(target) && Files.size(target) > 0L;
    }

    private record ResourceCopy(String dependencyKey, String resourcePath, String relativeTargetPath) {}
}
