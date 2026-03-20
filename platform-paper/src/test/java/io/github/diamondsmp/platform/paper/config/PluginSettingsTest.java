package io.github.diamondsmp.platform.paper.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PluginSettingsTest {
    @Test
    void defaultsKeepPvpBetaOptIn() {
        PluginSettings settings = PluginSettings.load(new YamlConfiguration());

        assertFalse(settings.pvp().enabled());
        assertTrue(settings.pvp().beta());
    }

    @Test
    void brandingDefaultsMatchDiamondSmpTestServerIdentity() {
        PluginSettings settings = PluginSettings.load(new YamlConfiguration());

        assertTrue(settings.branding().enabled());
        assertTrue(settings.branding().syncServerIcon());
        assertTrue(settings.branding().syncCompanionPluginConfigs());
        assertEquals("§b◆ §fDIAMOND SMP §8| §3THE DEEP END §b◆", settings.branding().motd().lineOne());
        assertEquals("§7Custom survival, event gear, and clean competitive progression", settings.branding().motd().lineTwo());
    }
}
