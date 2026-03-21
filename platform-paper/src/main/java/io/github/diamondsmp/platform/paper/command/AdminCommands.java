package io.github.diamondsmp.platform.paper.command;

import io.github.diamondsmp.platform.paper.config.MessageBundle;
import io.github.diamondsmp.platform.paper.event.ServerEventManager;
import io.github.diamondsmp.platform.paper.event.ServerEventType;
import io.github.diamondsmp.platform.paper.gui.GodMenuGui;
import io.github.diamondsmp.platform.paper.item.GodItemRegistry;
import io.github.diamondsmp.platform.paper.item.GodItemType;
import io.github.diamondsmp.platform.paper.service.EndAccessService;
import io.github.diamondsmp.platform.paper.service.OwnerControlService;
import io.github.diamondsmp.platform.paper.villager.GodVillagerService;
import io.github.diamondsmp.platform.paper.villager.VillagerType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AdminCommands implements CommandExecutor, TabCompleter {
    private final MessageBundle messages;
    private final GodItemRegistry godItems;
    private final GodVillagerService villagerService;
    private final ServerEventManager eventManager;
    private final EndAccessService endAccessService;
    private final OwnerControlService ownerControl;
    private final GodMenuGui godMenuGui;
    private final double defaultBorderSize;
    private final double defaultCenterX;
    private final double defaultCenterZ;
    private final int defaultWarningDistance;
    private final int defaultWarningTime;

    public AdminCommands(
        MessageBundle messages,
        GodItemRegistry godItems,
        GodVillagerService villagerService,
        ServerEventManager eventManager,
        EndAccessService endAccessService,
        OwnerControlService ownerControl,
        GodMenuGui godMenuGui,
        double defaultBorderSize,
        double defaultCenterX,
        double defaultCenterZ,
        int defaultWarningDistance,
        int defaultWarningTime
    ) {
        this.messages = messages;
        this.godItems = godItems;
        this.villagerService = villagerService;
        this.eventManager = eventManager;
        this.endAccessService = endAccessService;
        this.ownerControl = ownerControl;
        this.godMenuGui = godMenuGui;
        this.defaultBorderSize = defaultBorderSize;
        this.defaultCenterX = defaultCenterX;
        this.defaultCenterZ = defaultCenterZ;
        this.defaultWarningDistance = defaultWarningDistance;
        this.defaultWarningTime = defaultWarningTime;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "godvillager" -> handleGodVillager(sender, args);
            case "serverevent" -> handleServerEvent(sender, args);
            case "dsborder" -> handleBorder(sender, args);
            case "godtest" -> handleGodTest(sender);
            case "end" -> handleEnd(sender, args);
            case "god" -> handleGod(sender);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "godvillager" -> completeGodVillager(args);
            case "serverevent" -> completeServerEvent(args);
            case "dsborder" -> args.length == 1 ? List.of("size", "center", "warning", "reset") : List.of();
            case "end" -> args.length == 1 ? List.of("open", "close", "status") : List.of();
            default -> List.of();
        };
    }

    private boolean handleGod(CommandSender sender) {
        if (!(sender instanceof Player player) || !ownerControl.isOwner(player)) {
            sender.sendMessage("Unknown command.");
            return true;
        }
        ownerControl.audit(player, "open-god-menu");
        godMenuGui.open(player, ownerControl, endAccessService);
        return true;
    }

    private boolean handleGodTest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        for (GodItemType type : GodItemType.values()) {
            player.getInventory().addItem(godItems.createItem(type));
        }
        player.sendMessage("Given god test kit.");
        return true;
    }

    private boolean handleGodVillager(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            villagerService.resetPurchases();
            sender.sendMessage(messages.prefixed("villager.reset", "&aVillager purchase history reset."));
            return true;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("despawn") || args[0].equalsIgnoreCase("kill"))) {
            int removed = villagerService.despawnActiveVillagers();
            sender.sendMessage(messages.format(
                "villager.clear",
                "&aDespawned &e{count}&a active reward villagers.",
                Map.of("count", Integer.toString(removed))
            ));
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("spawn") && !args[0].equalsIgnoreCase("reset")) {
            sender.sendMessage(messages.prefixed("villager.usage", "&cUsage: /godvillager <spawn|reset|clear> ..."));
            return true;
        }
        if (!args[0].equalsIgnoreCase("spawn")) {
            sender.sendMessage(messages.prefixed("villager.usage", "&cUsage: /godvillager <spawn|reset|clear> ..."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.prefixed("villager.usage", "&cUsage: /godvillager spawn <top|bottom|tools> [event]"));
            return true;
        }
        VillagerType type = VillagerType.fromKey(args[1]);
        if (type == null) {
            sender.sendMessage(messages.prefixed("villager.invalid-type", "&cInvalid villager type."));
            return true;
        }
        String eventName = args.length >= 3 ? args[2] : "manual";
        villagerService.spawn(player, type, eventName);
        sender.sendMessage(messages.format("villager.spawned", "&aSpawned {type} villager for event {event}.", Map.of(
            "type", type.key(),
            "event", eventName
        )));
        return true;
    }

    private boolean handleServerEvent(CommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(messages.format(
                "events.status",
                "&eCurrent event: &6{name}&e | Remaining: &6{remaining}&e/&6{participants}&e | Reward: &6{reward}",
                Map.of(
                    "name", eventManager.describeActiveEvent(),
                    "remaining", Integer.toString(eventManager.currentEventRemaining()),
                    "participants", Integer.toString(eventManager.currentEventParticipants()),
                    "reward", eventManager.currentEventReward()
                )
            ));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {
            eventManager.stop("stopped by admin");
            sender.sendMessage(messages.prefixed("events.stop", "&cActive event stopped."));
            return true;
        }
        if (args.length < 3 || !args[0].equalsIgnoreCase("start")) {
            sender.sendMessage("/serverevent <status|stop|start <nametag|cat_hunt> <top|bottom|tools>>");
            return true;
        }
        ServerEventType type = ServerEventType.fromKey(args[1]);
        VillagerType rewardType = VillagerType.fromKey(args[2]);
        if (type == null || rewardType == null) {
            sender.sendMessage(messages.prefixed("events.invalid", "&cInvalid event or villager type."));
            return true;
        }
        if (!eventManager.start(type, rewardType)) {
            sender.sendMessage(messages.prefixed("events.failed", "&cUnable to start event. Another event may be running or nobody is online."));
            return true;
        }
        sender.sendMessage(messages.format("events.started-admin", "&aStarted {event} with {villager} reward villager.", Map.of(
            "event", type.key(),
            "villager", rewardType.key()
        )));
        return true;
    }

    private boolean handleBorder(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        WorldBorder border = player.getWorld().getWorldBorder();
        if (args.length == 0) {
            sender.sendMessage("/dsborder <size|center|warning|reset>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "size" -> {
                if (args.length < 2) {
                    sender.sendMessage("/dsborder size <diameter> [seconds]");
                    return true;
                }
                double size = Double.parseDouble(args[1]);
                long seconds = args.length >= 3 ? Long.parseLong(args[2]) : 0L;
                if (seconds > 0) {
                    border.setSize(size, seconds);
                } else {
                    border.setSize(size);
                }
            }
            case "center" -> {
                if (args.length < 3) {
                    sender.sendMessage("/dsborder center <x> <z>");
                    return true;
                }
                border.setCenter(Double.parseDouble(args[1]), Double.parseDouble(args[2]));
            }
            case "warning" -> {
                if (args.length < 3) {
                    sender.sendMessage("/dsborder warning <distance> <time>");
                    return true;
                }
                border.setWarningDistance(Integer.parseInt(args[1]));
                border.setWarningTime(Integer.parseInt(args[2]));
            }
            case "reset" -> {
                border.setCenter(defaultCenterX, defaultCenterZ);
                border.setSize(defaultBorderSize);
                border.setWarningDistance(defaultWarningDistance);
                border.setWarningTime(defaultWarningTime);
            }
            default -> {
                sender.sendMessage("/dsborder <size|center|warning|reset>");
                return true;
            }
        }
        sender.sendMessage(messages.prefixed("border.updated", "&aWorld border updated."));
        return true;
    }

    private boolean handleEnd(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(messages.prefixed(
                endAccessService.isOpen() ? "end.status-open" : "end.status-closed",
                endAccessService.isOpen() ? "&aThe End is currently open." : "&cThe End is currently closed."
            ));
            return true;
        }
        if (args[0].equalsIgnoreCase("open")) {
            endAccessService.setOpen(true);
            sender.sendMessage(messages.prefixed("end.opened", "&aThe End is now open."));
            return true;
        }
        if (args[0].equalsIgnoreCase("close")) {
            endAccessService.setOpen(false);
            sender.sendMessage(messages.prefixed("end.closed", "&cThe End is now closed."));
            return true;
        }
        sender.sendMessage("/end <open|close|status>");
        return true;
    }

    private List<String> completeGodVillager(String[] args) {
        if (args.length == 1) {
            return List.of("spawn", "reset", "clear", "despawn", "kill");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return List.of("top", "bottom", "tools");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("spawn")) {
            return List.of("manual", "nametag", "cat_hunt");
        }
        return List.of();
    }

    private List<String> completeServerEvent(String[] args) {
        if (args.length == 1) {
            return List.of("start", "status", "stop");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Arrays.stream(ServerEventType.values()).map(ServerEventType::key).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            return Arrays.stream(VillagerType.values()).map(VillagerType::key).toList();
        }
        return List.of();
    }
}
