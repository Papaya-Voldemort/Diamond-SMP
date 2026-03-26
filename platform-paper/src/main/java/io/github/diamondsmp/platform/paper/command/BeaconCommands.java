package io.github.diamondsmp.platform.paper.command;

import io.github.diamondsmp.platform.paper.beacon.GodBeaconCoreType;
import io.github.diamondsmp.platform.paper.beacon.GodBeaconRecord;
import io.github.diamondsmp.platform.paper.beacon.GodBeaconService;
import io.github.diamondsmp.platform.paper.beacon.GodBeaconTier;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class BeaconCommands implements CommandExecutor, TabCompleter {
    private final GodBeaconService beacons;

    public BeaconCommands(GodBeaconService beacons) {
        this.beacons = beacons;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            return showInfo(player);
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> showHelp(player);
            case "stats" -> showStats(player);
            case "settier" -> handleSetTier(player, args);
            case "givecore" -> handleGiveCore(player, args);
            case "debug" -> showDebug(player);
            default -> showInfo(player);
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help", "stats", "setTier", "giveCore", "debug").stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setTier")) {
            return List.of("0", "1", "2", "3", "4").stream().filter(value -> value.startsWith(args[1])).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("giveCore")) {
            return List.of("infusion", "resonance", "ascension", "god").stream()
                .filter(value -> value.startsWith(args[1].toLowerCase(Locale.ROOT)))
                .toList();
        }
        return List.of();
    }

    private boolean showInfo(Player player) {
        Optional<GodBeaconService.TargetedBeacon> target = beacons.findTarget(player, 10);
        if (target.isEmpty()) {
            player.sendMessage(color("&cNo beacon found within 10 blocks."));
            return true;
        }
        GodBeaconService.TargetedBeacon beacon = target.get();
        GodBeaconTier tier = beacon.tier();
        GodBeaconRecord record = beacon.record();
        player.sendMessage(color("&6Beacon &7@ &f" + beacon.block().getX() + ", " + beacon.block().getY() + ", " + beacon.block().getZ()));
        player.sendMessage(color("&eTier: &f" + tier.displayName() + " &7| &eState: &f" + (beacon.structure().customHandlingActive() ? "Active" : "Dormant")));
        player.sendMessage(color("&ePrimary: &f" + (record == null ? "Vanilla" : beacons.displayEffect(record.primaryEffect()))
            + " &7| &eSecondary: &f" + (record == null ? "None" : beacons.displayEffect(record.secondaryEffect()))));
        player.sendMessage(color("&eRadius: &f" + formatDouble(beacon.effectiveRadius()) + " blocks"));
        player.sendMessage(color("&eBonuses: &f" + describeTier(tier)));
        GodBeaconTier next = tier.next();
        if (next == null) {
            player.sendMessage(color("&eNext Upgrade: &6Max Tier"));
        } else {
            GodBeaconCoreType type = coreFor(next);
            GodBeaconService.PaymentPlan plan = beacons.buildPaymentPlan(player, beacon.block(), type, false);
            player.sendMessage(color("&eNext Upgrade: &f" + next.displayName()));
            player.sendMessage(color("&eCost: &f" + type.diamondBlockCost() + " Diamond Blocks, "
                + type.netherStarCost() + " Nether Star" + (type.netherStarCost() == 1 ? "" : "s") + ", 1 " + type.displayName()));
            player.sendMessage(color("&eAffordable Now: &f" + (plan.availableDiamondBlocks() >= type.diamondBlockCost()
                && plan.availableNetherStars() >= type.netherStarCost() ? "Yes" : "No")));
        }
        return true;
    }

    private boolean showHelp(Player player) {
        player.sendMessage(color("&6God Beacon Tiers"));
        for (GodBeaconTier tier : GodBeaconTier.values()) {
            player.sendMessage(color("&e" + tier.level() + ". &f" + tier.displayName() + " &7- " + describeTier(tier)));
        }
        player.sendMessage(color("&6Upgrade Path"));
        for (GodBeaconCoreType type : GodBeaconCoreType.values()) {
            player.sendMessage(color("&e" + type.displayName() + ": &f" + type.requiredTier().displayName() + " -> " + type.resultTier().displayName()
                + " &7(" + type.diamondBlockCost() + " Diamond Blocks, " + type.netherStarCost() + " Nether Stars)"));
        }
        player.sendMessage(color("&cTier 4 requires a full 4-layer pyramid."));
        return true;
    }

    private boolean showStats(Player player) {
        Optional<GodBeaconService.TargetedBeacon> target = beacons.findTarget(player, 10);
        if (target.isEmpty()) {
            player.sendMessage(color("&cNo beacon found within 10 blocks."));
            return true;
        }
        GodBeaconService.TargetedBeacon beacon = target.get();
        GodBeaconTier tier = beacon.tier();
        player.sendMessage(color("&6Beacon Stats: &f" + tier.displayName()));
        player.sendMessage(color("&eRadius Multiplier: &f" + formatPercent(tier.radiusMultiplier() - 1.0D)));
        player.sendMessage(color("&eReapply Interval: &f" + tier.reapplyTicks() + " ticks"));
        player.sendMessage(color("&eLinger: &f" + tier.lingerSeconds() + "s"));
        player.sendMessage(color("&eDurability Efficiency: &f" + formatPercent(tier.durabilityEfficiency())));
        player.sendMessage(color("&eFood Efficiency: &f" + formatPercent(tier.hungerReduction())));
        player.sendMessage(color("&eRegeneration Bonus: &f" + formatPercent(tier.regenerationBonus())));
        player.sendMessage(color("&eHaste Bonus: &f" + formatPercent(tier.hasteBonus())));
        player.sendMessage(color("&eSmelting Aura: &f" + formatPercent(tier.smeltingChance())));
        player.sendMessage(color("&eActivation Rule: &f" + (tier.requiresFullPyramid() ? "Requires full 4-layer pyramid" : "Any valid vanilla pyramid")));
        return true;
    }

    private boolean handleSetTier(Player player, String[] args) {
        if (!player.hasPermission("diamondsmp.admin.beacon")) {
            player.sendMessage(color("&cYou do not have permission to use that subcommand."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /beacon setTier <0-4>"));
            return true;
        }
        Optional<GodBeaconService.TargetedBeacon> target = beacons.findTarget(player, 10);
        if (target.isEmpty()) {
            player.sendMessage(color("&cNo beacon found within 10 blocks."));
            return true;
        }
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            player.sendMessage(color("&cTier must be 0-4."));
            return true;
        }
        if (level < 0 || level > 4) {
            player.sendMessage(color("&cTier must be 0-4."));
            return true;
        }
        GodBeaconTier tier = GodBeaconTier.fromLevel(level);
        beacons.setTier(target.get().block(), tier, player.getUniqueId());
        player.sendMessage(color("&aBeacon tier set to &f" + tier.displayName() + "&a."));
        return true;
    }

    private boolean handleGiveCore(Player player, String[] args) {
        if (!player.hasPermission("diamondsmp.admin.beacon")) {
            player.sendMessage(color("&cYou do not have permission to use that subcommand."));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(color("&cUsage: /beacon giveCore <infusion|resonance|ascension|god>"));
            return true;
        }
        GodBeaconCoreType type = GodBeaconCoreType.fromKey(args[1]);
        if (type == null) {
            player.sendMessage(color("&cUnknown core type."));
            return true;
        }
        player.getInventory().addItem(beacons.cores().createItem(type));
        player.sendMessage(color("&aGiven 1 " + type.displayName() + "."));
        return true;
    }

    private boolean showDebug(Player player) {
        if (!player.hasPermission("diamondsmp.admin.beacon")) {
            player.sendMessage(color("&cYou do not have permission to use that subcommand."));
            return true;
        }
        Optional<GodBeaconService.TargetedBeacon> target = beacons.findTarget(player, 10);
        if (target.isEmpty()) {
            player.sendMessage(color("&cNo beacon found within 10 blocks."));
            return true;
        }
        GodBeaconService.TargetedBeacon beacon = target.get();
        GodBeaconRecord record = beacon.record();
        player.sendMessage(color("&6Beacon Debug"));
        player.sendMessage(color("&eCoords: &f" + beacon.block().getLocation().toVector()));
        player.sendMessage(color("&eTracked: &f" + (record != null)));
        player.sendMessage(color("&eTier: &f" + beacon.tier().displayName()));
        player.sendMessage(color("&eState: &f" + (beacon.structure().customHandlingActive() ? "Active" : "Dormant")));
        player.sendMessage(color("&eVanilla Tier: &f" + beacon.structure().vanillaTier()));
        player.sendMessage(color("&eRadius: &f" + formatDouble(beacon.effectiveRadius())));
        if (record != null) {
            player.sendMessage(color("&eOwner: &f" + record.ownerId()));
            player.sendMessage(color("&eCreated: &f" + record.createdAt()));
            player.sendMessage(color("&ePrimary: &f" + beacons.displayEffect(record.primaryEffect())));
            player.sendMessage(color("&eSecondary: &f" + beacons.displayEffect(record.secondaryEffect())));
        }
        return true;
    }

    private GodBeaconCoreType coreFor(GodBeaconTier next) {
        return switch (next) {
            case INFUSED -> GodBeaconCoreType.INFUSION;
            case RESONANT -> GodBeaconCoreType.RESONANCE;
            case ASCENDED -> GodBeaconCoreType.ASCENSION;
            case GOD -> GodBeaconCoreType.GOD;
            default -> throw new IllegalArgumentException("No core for " + next);
        };
    }

    private String describeTier(GodBeaconTier tier) {
        return switch (tier) {
            case VANILLA -> "Vanilla behavior";
            case INFUSED -> "+10% radius, 3s reapply";
            case RESONANT -> "+20% radius, secondary effect, 2s reapply";
            case ASCENDED -> "+30% radius, 5s linger, +20% beacon regeneration, -10% hunger";
            case GOD -> "+40% radius, 8s linger, 15% durability save, +10% haste mining, +25% beacon regeneration, -20% hunger, 10% smelting";
        };
    }

    private String formatDouble(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private String formatPercent(double value) {
        return value <= 0.0D ? "0%" : String.format(Locale.US, "%.0f%%", value * 100.0D);
    }

    private String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
