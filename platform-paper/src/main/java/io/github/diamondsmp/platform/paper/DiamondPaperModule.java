package io.github.diamondsmp.platform.paper;

import io.github.diamondsmp.core.api.LifecyclePhase;
import io.github.diamondsmp.core.api.PluginContext;
import io.github.diamondsmp.platform.paper.advancement.MythicalAdvancementService;
import io.github.diamondsmp.platform.paper.command.AdminCommands;
import io.github.diamondsmp.platform.paper.command.PlayerCommands;
import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.config.PluginSettings;
import io.github.diamondsmp.platform.paper.config.RulesBook;
import io.github.diamondsmp.platform.paper.event.ServerEventManager;
import io.github.diamondsmp.platform.paper.gui.PvpGui;
import io.github.diamondsmp.platform.paper.gui.RulesGui;
import io.github.diamondsmp.platform.paper.gui.GodMenuGui;
import io.github.diamondsmp.platform.paper.item.GodItemRegistry;
import io.github.diamondsmp.platform.paper.listener.GameplayListener;
import io.github.diamondsmp.platform.paper.listener.BrandingListener;
import io.github.diamondsmp.platform.paper.listener.DragonEggListener;
import io.github.diamondsmp.platform.paper.listener.GodMenuListener;
import io.github.diamondsmp.platform.paper.listener.GuiAndVillagerListener;
import io.github.diamondsmp.platform.paper.listener.PvpListener;
import io.github.diamondsmp.platform.paper.listener.RestrictionListener;
import io.github.diamondsmp.platform.paper.placeholder.DiamondPlaceholderExpansion;
import io.github.diamondsmp.platform.paper.service.CombatStateService;
import io.github.diamondsmp.platform.paper.service.BrandingService;
import io.github.diamondsmp.platform.paper.service.CooldownService;
import io.github.diamondsmp.platform.paper.service.DragonEggService;
import io.github.diamondsmp.platform.paper.service.EndAccessService;
import io.github.diamondsmp.platform.paper.service.KitService;
import io.github.diamondsmp.platform.paper.service.OwnerControlService;
import io.github.diamondsmp.platform.paper.service.OwnerRecoveryExportService;
import io.github.diamondsmp.platform.paper.service.PartyService;
import io.github.diamondsmp.platform.paper.service.PurchaseHistoryStore;
import io.github.diamondsmp.platform.paper.service.PvpService;
import io.github.diamondsmp.platform.paper.service.PvpTestService;
import io.github.diamondsmp.platform.paper.service.TeleportRequestService;
import io.github.diamondsmp.platform.paper.service.TrustService;
import io.github.diamondsmp.platform.paper.villager.GodVillagerService;
import java.io.File;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiamondPaperModule {
    private final PluginContext context;
    private final JavaPlugin plugin;

    public DiamondPaperModule(PluginContext context) {
        this.context = context;
        this.plugin = context.plugin();
    }

    public void install() {
        saveDefaults();
        context.lifecycle().mark(LifecyclePhase.LOAD_CONFIG);

        PluginSettings settings = PluginSettings.load(plugin.getConfig());
        BrandingService brandingService = new BrandingService(plugin, settings.branding());
        MessageBundle messages = MessageBundle.load(load("messages.yml"));
        RulesBook rulesBook = RulesBook.load(load("rules.yml"));
        YamlConfiguration villagersConfig = load("villagers.yml");
        YamlConfiguration kitsConfig = load("kits.yml");
        brandingService.applyStartupBranding();

        GodItemRegistry godItems = new GodItemRegistry(plugin);
        CooldownService cooldowns = new CooldownService();
        TeleportRequestService teleportRequests = new TeleportRequestService();
        TrustService trustService = new TrustService();
        CombatStateService combatState = new CombatStateService();
        DragonEggService dragonEggService = new DragonEggService(plugin);
        EndAccessService endAccessService = new EndAccessService(plugin);
        OwnerControlService ownerControl = new OwnerControlService(plugin, settings);
        ownerControl.attachDragonEggs(dragonEggService);
        OwnerRecoveryExportService ownerRecoveryExportService = new OwnerRecoveryExportService(plugin);
        KitService kitService = new KitService(plugin, kitsConfig);
        PartyService partyService = new PartyService();
        PvpTestService pvpTestService = new PvpTestService();
        PvpGui pvpGui = new PvpGui(settings.pvp().gui());
        PvpService pvpService = new PvpService(plugin, settings, messages, kitService, partyService, combatState, pvpTestService, ownerControl, pvpGui);
        PurchaseHistoryStore purchaseHistory = new PurchaseHistoryStore(plugin);
        GodVillagerService villagerService = new GodVillagerService(plugin, settings, godItems, purchaseHistory, villagersConfig);
        ServerEventManager eventManager = new ServerEventManager(plugin, messages, villagerService, pvpService::isInPvpSession);
        MythicalAdvancementService mythicalAdvancements = new MythicalAdvancementService(plugin, messages);
        RulesGui rulesGui = new RulesGui(rulesBook);
        GodMenuGui godMenuGui = new GodMenuGui();

        context.lifecycle().mark(LifecyclePhase.REGISTER_LISTENERS);
        registerListeners(
            new BrandingListener(brandingService),
            new GameplayListener(plugin, settings, messages, godItems, cooldowns, combatState, trustService, dragonEggService, endAccessService, eventManager, mythicalAdvancements, pvpService),
            new RestrictionListener(settings, godItems),
            new GuiAndVillagerListener(rulesGui, villagerService, messages),
            new PvpListener(pvpGui, pvpService),
            new DragonEggListener(dragonEggService, messages),
            new GodMenuListener(
                plugin,
                godMenuGui,
                ownerControl,
                ownerRecoveryExportService,
                endAccessService,
                eventManager,
                godItems,
                messages,
                settings.borderDefaults().size(),
                settings.borderDefaults().centerX(),
                settings.borderDefaults().centerZ(),
                settings.borderDefaults().warningDistance(),
                settings.borderDefaults().warningTime()
            )
        );

        context.lifecycle().mark(LifecyclePhase.REGISTER_COMMANDS);
        PlayerCommands playerCommands = new PlayerCommands(
            plugin,
            settings,
            messages,
            cooldowns,
            combatState,
            kitService,
            teleportRequests,
            trustService,
            partyService,
            pvpService,
            ownerControl,
            rulesGui,
            eventManager
        );
        AdminCommands adminCommands = new AdminCommands(
            messages,
            godItems,
            villagerService,
            eventManager,
            endAccessService,
            ownerControl,
            godMenuGui,
            settings.borderDefaults().size(),
            settings.borderDefaults().centerX(),
            settings.borderDefaults().centerZ(),
            settings.borderDefaults().warningDistance(),
            settings.borderDefaults().warningTime()
        );
        registerCommand("string", playerCommands, playerCommands);
        registerCommand("tpa", playerCommands, playerCommands);
        registerCommand("tpaccept", playerCommands, playerCommands);
        registerCommand("tpdeny", playerCommands, playerCommands);
        registerCommand("kit", playerCommands, playerCommands);
        registerCommand("trust", playerCommands, playerCommands);
        registerCommand("untrust", playerCommands, playerCommands);
        registerCommand("spawn", playerCommands, playerCommands);
        registerCommand("rules", playerCommands, playerCommands);
        registerCommand("event", playerCommands, playerCommands);
        registerCommand("p", playerCommands, playerCommands);
        registerCommand("pvp", playerCommands, playerCommands);
        registerCommand("godvillager", adminCommands, adminCommands);
        registerCommand("serverevent", adminCommands, adminCommands);
        registerCommand("dsborder", adminCommands, adminCommands);
        registerCommand("godtest", adminCommands, adminCommands);
        registerCommand("end", adminCommands, adminCommands);
        registerCommand("dragonegg", adminCommands, adminCommands);
        registerCommand("god", adminCommands, adminCommands);

        registerRecipes();
        configureBorders(settings);
        registerPlaceholderExpansion(eventManager);
    }

    private void registerListeners(org.bukkit.event.Listener... listeners) {
        PluginManager manager = Bukkit.getPluginManager();
        for (org.bukkit.event.Listener listener : listeners) {
            manager.registerEvents(listener, plugin);
        }
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter completer) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command " + name + " missing from plugin.yml");
        }
        command.setExecutor(executor);
        command.setTabCompleter(completer);
    }

    private void configureBorders(PluginSettings settings) {
        for (World world : Bukkit.getWorlds()) {
            world.getWorldBorder().setSize(settings.borderDefaults().size());
            world.getWorldBorder().setCenter(settings.borderDefaults().centerX(), settings.borderDefaults().centerZ());
            world.getWorldBorder().setWarningDistance(settings.borderDefaults().warningDistance());
            world.getWorldBorder().setWarningTime(settings.borderDefaults().warningTime());
        }
    }

    private YamlConfiguration load(String fileName) {
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));
    }

    private void registerRecipes() {
        ShapelessRecipe cobwebRecipe = new ShapelessRecipe(new NamespacedKey(plugin, "easy_cobweb"), new ItemStack(Material.COBWEB, 5));
        for (int i = 0; i < 5; i++) {
            cobwebRecipe.addIngredient(Material.STRING);
        }
        Bukkit.addRecipe(cobwebRecipe);

        ShapedRecipe goldenAppleRecipe = new ShapedRecipe(new NamespacedKey(plugin, "easy_gaps"), new ItemStack(Material.GOLDEN_APPLE, 8));
        goldenAppleRecipe.shape("GGG", "GAG", "GGG");
        goldenAppleRecipe.setIngredient('G', Material.GOLD_INGOT);
        goldenAppleRecipe.setIngredient('A', Material.APPLE);
        Bukkit.addRecipe(goldenAppleRecipe);
    }

    private void registerPlaceholderExpansion(ServerEventManager eventManager) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        new DiamondPlaceholderExpansion(plugin, eventManager).register();
    }

    private void saveDefaults() {
        plugin.saveDefaultConfig();
        for (String file : List.of("messages.yml", "rules.yml", "villagers.yml", "events.yml", "kits.yml")) {
            if (!new File(plugin.getDataFolder(), file).exists()) {
                plugin.saveResource(file, false);
            }
        }
    }
}
