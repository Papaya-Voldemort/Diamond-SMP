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
        assertTrue(settings.branding().companionDownloads().enabled());
        assertEquals("PlaceholderAPI", settings.branding().companionDownloads().placeholderApi().pluginName());
        assertEquals("TAB", settings.branding().companionDownloads().tab().pluginName());
        assertEquals("CustomJoinMessages", settings.branding().companionDownloads().customJoinMessages().pluginName());
        assertEquals("§b◆ §fDIAMOND SMP §8| §3THE DEEP END §b◆", settings.branding().motd().lineOne());
        assertEquals("§7Custom survival, event gear, and clean competitive progression", settings.branding().motd().lineTwo());
    }

    @Test
    void worldOreDefaultsFavorIronCoalAndToneDownDiamonds() {
        PluginSettings settings = PluginSettings.load(new YamlConfiguration());

        assertEquals(1.0D, settings.worldRules().diamondDropMultiplier());
        assertTrue(settings.worldRules().diamondOre().veinSizeMultiplier() < settings.worldRules().ironOre().veinSizeMultiplier());
        assertTrue(settings.worldRules().diamondOre().maxAddedBlocksPerVein() < settings.worldRules().coalOre().maxAddedBlocksPerVein());
        assertEquals(1, settings.worldRules().diamondOre().exposedMaxAdditions());
    }

    @Test
    void villagerEconomyDefaultsEnablePersistenceAndDiamondMasterTrades() {
        PluginSettings settings = PluginSettings.load(new YamlConfiguration());

        assertTrue(settings.villagers().persistManaged());
        assertTrue(settings.villagers().dropEggOnKill());
        assertEquals(0.20D, settings.villagers().masterDiamondTradeChance());
    }
}
