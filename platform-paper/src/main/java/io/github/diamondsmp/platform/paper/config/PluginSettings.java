package io.github.diamondsmp.platform.paper.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record PluginSettings(
    boolean debug,
    Cooldowns cooldowns,
    Combat combat,
    WorldRules worldRules,
    OwnerControl ownerControl,
    Villagers villagers,
    Kits kits,
    BorderDefaults borderDefaults,
    Toggles toggles,
    Pvp pvp,
    Branding branding
) {
    public static PluginSettings load(FileConfiguration config) {
        return new PluginSettings(
            config.getBoolean("debug", false),
            new Cooldowns(
                Duration.ofSeconds(config.getLong("cooldowns.infinite-totem-seconds", 180L)),
                Duration.ofSeconds(config.getLong("cooldowns.string-command-seconds", 300L)),
                Duration.ofSeconds(config.getLong("cooldowns.tpa-seconds", 60L))
            ),
            new Combat(
                config.getDouble("combat.god-bow-base-damage", 4.0D),
                config.getDouble("combat.god-bow-headshot-bonus", 2.0D),
                config.getDouble("combat.god-bow-velocity", 3.2D),
                config.getDouble("combat.god-bow-range", 48.0D),
                config.getDouble("combat.axe-armor-durability-multiplier", 3.0D),
                Duration.ofSeconds(config.getLong("combat.bedrock-break-seconds", 30L)),
                Duration.ofSeconds(config.getLong("combat.tag-seconds", 15L))
            ),
            new WorldRules(
                config.getDouble("world.diamond-drop-multiplier", 3.0D),
                config.getDouble("world.exposed-diamond-multiplier", 5.0D),
                config.getDouble("world.diamond-vein-size-multiplier", 2.0D),
                config.getBoolean("world.remove-ancient-debris-from-new-chunks", true),
                config.getBoolean("world.disable-netherite-progression", true),
                config.getBoolean("world.disable-restricted-enchants", true),
                config.getBoolean("world.disable-strength-ii", true)
            ),
            new OwnerControl(
                config.getString("owner-control.owner-name", "SandersonFan"),
                config.getString("owner-control.owner-uuid", ""),
                config.getString("owner-control.owner-uuid-trimmed", "")
            ),
            new Villagers(
                Duration.ofMinutes(config.getLong("villagers.despawn-minutes", 30L)),
                loadTradeCosts(config.getConfigurationSection("villagers.costs"))
            ),
            new Kits(
                config.getBoolean("kits.allow-regular-users", true)
            ),
            new BorderDefaults(
                config.getDouble("world-border.default-size", 4000.0D),
                config.getDouble("world-border.default-center.x", 0.0D),
                config.getDouble("world-border.default-center.z", 0.0D),
                config.getInt("world-border.warning-distance", 64),
                config.getInt("world-border.warning-time", 15)
            ),
            new Toggles(
                config.getBoolean("systems.god-items", true),
                config.getBoolean("systems.teleport", true),
                config.getBoolean("systems.events", true),
                config.getBoolean("systems.rules-gui", true),
                config.getBoolean("systems.border-tools", true)
            ),
            loadPvp(config),
            loadBranding(config)
        );
    }

    private static Pvp loadPvp(FileConfiguration config) {
        ConfigurationSection pvp = config.getConfigurationSection("pvp");
        if (pvp == null) {
            return Pvp.defaults();
        }
        ConfigurationSection modesSection = pvp.getConfigurationSection("modes");
        Map<String, PvpMode> modes = new LinkedHashMap<>();
        if (modesSection != null) {
            for (String key : modesSection.getKeys(false)) {
                ConfigurationSection modeSection = modesSection.getConfigurationSection(key);
                if (modeSection == null) {
                    continue;
                }
                List<Integer> sizes = modeSection.getIntegerList("team-sizes");
                if (sizes.size() != 2) {
                    continue;
                }
                modes.put(key.toLowerCase(Locale.ROOT), new PvpMode(
                    key.toLowerCase(Locale.ROOT),
                    color(modeSection.getString("display-name", key)),
                    sizes,
                    Material.matchMaterial(modeSection.getString("icon", "IRON_SWORD")) == null
                        ? Material.IRON_SWORD
                        : Material.matchMaterial(modeSection.getString("icon", "IRON_SWORD"))
                ));
            }
        }
        if (modes.isEmpty()) {
            modes.putAll(Pvp.defaults().modes());
        }
        return new Pvp(
            pvp.getBoolean("enabled", false),
            pvp.getBoolean("beta", true),
            pvp.getString("world-name", "diamond_pvp_beta"),
            java.time.Duration.ofSeconds(pvp.getLong("party-invite-timeout-seconds", 60L)),
            pvp.getInt("arena-count", 4),
            new PvpArena(
                pvp.getInt("arena.y-level", 90),
                pvp.getInt("arena.half-size", 18),
                pvp.getInt("arena.height", 8),
                pvp.getInt("arena.spacing", 96),
                pvp.getInt("arena.spawn-spread", 4),
                materialOrFallback(pvp.getString("arena.floor-material"), Material.SMOOTH_STONE),
                materialOrFallback(pvp.getString("arena.wall-material"), Material.BARRIER)
            ),
            pvp.getStringList("kits"),
            Map.copyOf(modes),
            loadPvpGui(pvp.getConfigurationSection("gui"))
        );
    }

    private static Branding loadBranding(FileConfiguration config) {
        ConfigurationSection branding = config.getConfigurationSection("branding");
        if (branding == null) {
            return Branding.defaults();
        }
        Branding defaults = Branding.defaults();
        ConfigurationSection motd = branding.getConfigurationSection("motd");
        return new Branding(
            branding.getBoolean("enabled", defaults.enabled()),
            branding.getBoolean("sync-server-icon", defaults.syncServerIcon()),
            branding.getBoolean("sync-companion-plugin-configs", defaults.syncCompanionPluginConfigs()),
            new Motd(
                color(motd == null ? defaults.motd().lineOne() : motd.getString("line-1", defaults.motd().lineOne())),
                color(motd == null ? defaults.motd().lineTwo() : motd.getString("line-2", defaults.motd().lineTwo()))
            ),
            loadCompanionDownloads(branding.getConfigurationSection("companion-downloads"))
        );
    }

    private static CompanionDownloads loadCompanionDownloads(ConfigurationSection section) {
        CompanionDownloads defaults = CompanionDownloads.defaults();
        if (section == null) {
            return defaults;
        }
        return new CompanionDownloads(
            section.getBoolean("enabled", defaults.enabled()),
            loadCompanionDownload(section.getConfigurationSection("placeholderapi"), defaults.placeholderApi()),
            loadCompanionDownload(section.getConfigurationSection("tab"), defaults.tab()),
            loadCompanionDownload(section.getConfigurationSection("custom-join-messages"), defaults.customJoinMessages())
        );
    }

    private static CompanionDownload loadCompanionDownload(ConfigurationSection section, CompanionDownload defaults) {
        if (section == null) {
            return defaults;
        }
        return new CompanionDownload(
            section.getBoolean("enabled", defaults.enabled()),
            section.getString("plugin-name", defaults.pluginName()),
            section.getString("jar-name", defaults.jarName()),
            section.getString("url", defaults.url())
        );
    }

    private static PvpGuiSettings loadPvpGui(ConfigurationSection section) {
        if (section == null) {
            return PvpGuiSettings.defaults();
        }
        PvpGuiSettings defaults = PvpGuiSettings.defaults();
        return new PvpGuiSettings(
            color(section.getString("hub-title", defaults.hubTitle())),
            color(section.getString("setup-title", defaults.setupTitle())),
            color(section.getString("result-title", defaults.resultTitle())),
            inventorySize(section.getInt("hub-size", defaults.hubSize()), defaults.hubSize(), defaults.hubSize()),
            inventorySize(section.getInt("setup-size", defaults.setupSize()), defaults.setupSize(), defaults.setupSize()),
            inventorySize(section.getInt("result-size", defaults.resultSize()), defaults.resultSize(), defaults.resultSize()),
            materialOrFallback(section.getString("hub-filler"), defaults.hubFiller()),
            materialOrFallback(section.getString("setup-filler"), defaults.setupFiller()),
            materialOrFallback(section.getString("result-filler"), defaults.resultFiller()),
            materialOrFallback(section.getString("party-icon"), defaults.partyIcon()),
            materialOrFallback(section.getString("help-icon"), defaults.helpIcon()),
            materialOrFallback(section.getString("available-arenas-icon"), defaults.availableArenasIcon()),
            materialOrFallback(section.getString("return-icon"), defaults.returnIcon()),
            materialOrFallback(section.getString("random-mode-icon"), defaults.randomModeIcon()),
            materialOrFallback(section.getString("no-compatible-mode-icon"), defaults.noCompatibleModeIcon()),
            materialOrFallback(section.getString("rematch-icon"), defaults.rematchIcon()),
            materialOrFallback(section.getString("locked-rematch-icon"), defaults.lockedRematchIcon()),
            materialOrFallback(section.getString("roster-icon"), defaults.rosterIcon()),
            materialOrFallback(section.getString("rules-icon"), defaults.rulesIcon()),
            materialOrFallback(section.getString("checks-icon"), defaults.checksIcon()),
            materialOrFallback(section.getString("selected-kit-icon"), defaults.selectedKitIcon()),
            materialOrFallback(section.getString("previous-kit-icon"), defaults.previousKitIcon()),
            materialOrFallback(section.getString("random-kit-icon"), defaults.randomKitIcon()),
            materialOrFallback(section.getString("next-kit-icon"), defaults.nextKitIcon()),
            materialOrFallback(section.getString("start-icon"), defaults.startIcon()),
            materialOrFallback(section.getString("back-icon"), defaults.backIcon()),
            materialOrFallback(section.getString("result-return-icon"), defaults.resultReturnIcon()),
            materialOrFallback(section.getString("result-rematch-icon"), defaults.resultRematchIcon()),
            materialOrFallback(section.getString("result-new-match-icon"), defaults.resultNewMatchIcon())
        );
    }

    private static int inventorySize(int configured, int fallback, int minimum) {
        if (configured < minimum || configured > 54 || configured % 9 != 0) {
            return fallback;
        }
        return configured;
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    private static Material materialOrFallback(String value, Material fallback) {
        Material material = Material.matchMaterial(value == null ? "" : value);
        return material == null ? fallback : material;
    }

    private static Map<String, Integer> loadTradeCosts(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        return section.getKeys(false).stream().collect(
            java.util.stream.Collectors.toUnmodifiableMap(key -> key.toLowerCase(Locale.ROOT), section::getInt)
        );
    }

    public int costFor(String itemKey, int fallback) {
        return villagers.tradeCosts().getOrDefault(itemKey.toLowerCase(Locale.ROOT), fallback);
    }

    public record Cooldowns(Duration infiniteTotem, Duration stringCommand, Duration teleportRequest) {}

    public record Combat(
        double godBowBaseDamage,
        double godBowHeadshotBonus,
        double godBowVelocity,
        double godBowRange,
        double axeArmorDurabilityMultiplier,
        Duration bedrockBreakTime,
        Duration combatTagTime
    ) {}

    public record WorldRules(
        double diamondDropMultiplier,
        double exposedDiamondMultiplier,
        double diamondVeinSizeMultiplier,
        boolean removeAncientDebrisFromNewChunks,
        boolean disableNetheriteProgression,
        boolean disableRestrictedEnchants,
        boolean disableStrengthTwo
    ) {}

    public record OwnerControl(String ownerName, String ownerUuid, String ownerUuidTrimmed) {}

    public record Villagers(Duration despawnAfter, Map<String, Integer> tradeCosts) {}

    public record Kits(boolean allowRegularUsers) {}

    public record BorderDefaults(
        double size,
        double centerX,
        double centerZ,
        int warningDistance,
        int warningTime
    ) {}

    public record Toggles(
        boolean godItems,
        boolean teleport,
        boolean events,
        boolean rulesGui,
        boolean borderTools
    ) {}

    public record Pvp(
        boolean enabled,
        boolean beta,
        String worldName,
        Duration partyInviteTimeout,
        int arenaCount,
        PvpArena arena,
        List<String> kits,
        Map<String, PvpMode> modes,
        PvpGuiSettings gui
    ) {
        public static Pvp defaults() {
            Map<String, PvpMode> modes = new LinkedHashMap<>();
            modes.put("1v1", new PvpMode("1v1", color("&c1v1 Duel"), List.of(1, 1), Material.IRON_SWORD));
            modes.put("2v2", new PvpMode("2v2", color("&62v2 Clash"), List.of(2, 2), Material.DIAMOND_SWORD));
            modes.put("3v3", new PvpMode("3v3", color("&53v3 Skirmish"), List.of(3, 3), Material.NETHERITE_SWORD));
            modes.put("1v2", new PvpMode("1v2", color("&e1v2 Trial"), List.of(1, 2), Material.SHIELD));
            return new Pvp(
                false,
                true,
                "diamond_pvp_beta",
                Duration.ofSeconds(60L),
                4,
                new PvpArena(90, 18, 8, 96, 4, Material.SMOOTH_STONE, Material.BARRIER),
                List.of("starter", "archer", "tank"),
                Map.copyOf(modes),
                PvpGuiSettings.defaults()
            );
        }
    }

    public record Branding(
        boolean enabled,
        boolean syncServerIcon,
        boolean syncCompanionPluginConfigs,
        Motd motd,
        CompanionDownloads companionDownloads
    ) {
        public static Branding defaults() {
            return new Branding(
                true,
                true,
                true,
                new Motd(
                    color("&b◆ &fDIAMOND SMP &8| &3THE DEEP END &b◆"),
                    color("&7Custom survival, event gear, and clean competitive progression")
                ),
                CompanionDownloads.defaults()
            );
        }
    }

    public record Motd(String lineOne, String lineTwo) {}

    public record CompanionDownloads(
        boolean enabled,
        CompanionDownload placeholderApi,
        CompanionDownload tab,
        CompanionDownload customJoinMessages
    ) {
        public static CompanionDownloads defaults() {
            return new CompanionDownloads(
                true,
                new CompanionDownload(
                    true,
                    "PlaceholderAPI",
                    "PlaceholderAPI-2.12.2.jar",
                    "https://cdn.modrinth.com/data/lKEzGugV/versions/UmbIiI5H/PlaceholderAPI-2.12.2.jar"
                ),
                new CompanionDownload(
                    true,
                    "TAB",
                    "TAB v5.5.0.jar",
                    "https://cdn.modrinth.com/data/gG7VFbG0/versions/eiJUTmjO/TAB%20v5.5.0.jar"
                ),
                new CompanionDownload(
                    true,
                    "CustomJoinMessages",
                    "custom-join-messages-17.9.1.jar",
                    "https://cdn.modrinth.com/data/PJMIw5vh/versions/nOo9275N/custom-join-messages-17.9.1.jar"
                )
            );
        }
    }

    public record CompanionDownload(
        boolean enabled,
        String pluginName,
        String jarName,
        String url
    ) {}

    public record PvpArena(
        int yLevel,
        int halfSize,
        int height,
        int spacing,
        int spawnSpread,
        Material floorMaterial,
        Material wallMaterial
    ) {}

    public record PvpMode(String key, String displayName, List<Integer> teamSizes, Material icon) {
        public int totalPlayers() {
            return teamSizes.stream().mapToInt(Integer::intValue).sum();
        }
    }

    public record PvpGuiSettings(
        String hubTitle,
        String setupTitle,
        String resultTitle,
        int hubSize,
        int setupSize,
        int resultSize,
        Material hubFiller,
        Material setupFiller,
        Material resultFiller,
        Material partyIcon,
        Material helpIcon,
        Material availableArenasIcon,
        Material returnIcon,
        Material randomModeIcon,
        Material noCompatibleModeIcon,
        Material rematchIcon,
        Material lockedRematchIcon,
        Material rosterIcon,
        Material rulesIcon,
        Material checksIcon,
        Material selectedKitIcon,
        Material previousKitIcon,
        Material randomKitIcon,
        Material nextKitIcon,
        Material startIcon,
        Material backIcon,
        Material resultReturnIcon,
        Material resultRematchIcon,
        Material resultNewMatchIcon
    ) {
        public static PvpGuiSettings defaults() {
            return new PvpGuiSettings(
                color("&4PvP Hub"),
                color("&4PvP Setup"),
                color("&3Match Complete"),
                54,
                54,
                27,
                Material.GRAY_STAINED_GLASS_PANE,
                Material.BLACK_STAINED_GLASS_PANE,
                Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                Material.PLAYER_HEAD,
                Material.BOOK,
                Material.COMPASS,
                Material.OAK_DOOR,
                Material.LIGHTNING_ROD,
                Material.BARRIER,
                Material.NETHERITE_SWORD,
                Material.CLOCK,
                Material.PLAYER_HEAD,
                Material.MAP,
                Material.IRON_BARS,
                Material.IRON_SWORD,
                Material.ARROW,
                Material.CHEST,
                Material.SPECTRAL_ARROW,
                Material.EMERALD,
                Material.BARRIER,
                Material.GRASS_BLOCK,
                Material.NETHERITE_SWORD,
                Material.COMPASS
            );
        }
    }

    public record TradeEntry(String itemKey, int emeraldCost, Material currency, int currencyAmount) {
        public static TradeEntry fromSection(ConfigurationSection section, int defaultCost) {
            return new TradeEntry(
                section.getName(),
                section.getInt("emerald-cost", defaultCost),
                Material.matchMaterial(section.getString("currency", "EMERALD")),
                section.getInt("currency-amount", 1)
            );
        }
    }

    public static List<TradeEntry> loadTrades(ConfigurationSection section, PluginSettings settings) {
        if (section == null) {
            return List.of();
        }
        return section.getKeys(false).stream()
            .map(key -> {
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child == null) {
                    return new TradeEntry(key, settings.costFor(key, 64), Material.EMERALD, 1);
                }
                return TradeEntry.fromSection(child, settings.costFor(key, 64));
            })
            .toList();
    }
}
