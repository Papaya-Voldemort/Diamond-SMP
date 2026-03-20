package io.github.diamondsmp.platform.paper.placeholder;

import io.github.diamondsmp.platform.paper.event.ServerEventManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DiamondPlaceholderExpansion extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final ServerEventManager eventManager;

    public DiamondPlaceholderExpansion(JavaPlugin plugin, ServerEventManager eventManager) {
        this.plugin = plugin;
        this.eventManager = eventManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "diamondsmp";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return switch (params.toLowerCase(java.util.Locale.ROOT)) {
            case "current_event" -> eventManager.currentEventKey();
            case "current_event_name" -> eventManager.describeActiveEvent();
            case "event_status" -> eventManager.currentEventStatus();
            case "event_reward" -> eventManager.currentEventReward();
            case "event_participants" -> Integer.toString(eventManager.currentEventParticipants());
            case "event_remaining" -> Integer.toString(eventManager.currentEventRemaining());
            default -> null;
        };
    }
}
