package io.github.diamondsmp.platform.paper.listener;

import io.github.diamondsmp.platform.paper.service.BrandingService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

public final class BrandingListener implements Listener {
    private final BrandingService brandingService;

    public BrandingListener(BrandingService brandingService) {
        this.brandingService = brandingService;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        event.setMotd(brandingService.motd());
    }
}
