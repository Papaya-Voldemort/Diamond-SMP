package io.github.diamondsmp.platform.paper.gui;

import io.github.diamondsmp.platform.paper.config.PluginSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class PvpGui {
    private static final List<Integer> KIT_PICKER_SLOTS = List.of(
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    );

    private final PluginSettings.PvpGuiSettings settings;

    public PvpGui(PluginSettings.PvpGuiSettings settings) {
        this.settings = settings;
    }

    public void openHubMenu(Player player, HubView view) {
        HubMenuHolder holder = new HubMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, settings.hubSize(), settings.hubTitle());
        fill(inventory, settings.hubFiller(), " ");

        inventory.setItem(4, create(Material.NETHER_STAR, ChatColor.GOLD + "Match Browser", List.of(
            ChatColor.GRAY + "Pick a mode, review your party,",
            ChatColor.GRAY + "and launch without losing SMP progress."
        )));
        inventory.setItem(46, create(settings.partyIcon(), ChatColor.AQUA + "Party Status", view.partyLore()));
        inventory.setItem(49, create(settings.helpIcon(), ChatColor.YELLOW + "Quick Help", List.of(
            ChatColor.GRAY + "/pvp status",
            ChatColor.GRAY + "/pvp modes",
            ChatColor.GRAY + "/pvp kits",
            ChatColor.GRAY + "/pvp start <mode> <kit>"
        )));
        inventory.setItem(48, create(Material.TARGET, ChatColor.GOLD + "Queue Snapshot", List.of(
            ChatColor.GRAY + "Open arenas: " + ChatColor.WHITE + view.availableArenas(),
            ChatColor.GRAY + "Busy arenas: " + ChatColor.WHITE + view.busyArenas(),
            ChatColor.GRAY + "Configured modes: " + ChatColor.WHITE + view.modes().size()
        )));

        if (view.canReturn()) {
            holder.returnSlot = 45;
            inventory.setItem(45, create(settings.returnIcon(), ChatColor.GREEN + "Return to SMP", List.of(
                ChatColor.GRAY + "Restore your saved inventory",
                ChatColor.GRAY + "and your previous location"
            )));
        } else {
            inventory.setItem(45, create(settings.availableArenasIcon(), ChatColor.GREEN + "Open Arenas", List.of(
                ChatColor.GRAY + "Available: " + ChatColor.WHITE + view.availableArenas(),
                ChatColor.GRAY + "Busy: " + ChatColor.WHITE + view.busyArenas()
            )));
        }

        if (view.canRematch()) {
            holder.rematchSlot = 53;
            inventory.setItem(53, create(settings.rematchIcon(), ChatColor.RED + "Rematch", List.of(
                ChatColor.GRAY + "Replay your most recent",
                ChatColor.GRAY + "completed setup instantly"
            )));
        } else {
            inventory.setItem(53, create(settings.lockedRematchIcon(), ChatColor.DARK_GRAY + "No Rematch Ready", List.of(
                ChatColor.GRAY + "Complete a match to unlock rematch"
            )));
        }

        if (view.randomModeKey() != null) {
            holder.randomSlot = 47;
            inventory.setItem(47, create(settings.randomModeIcon(), ChatColor.YELLOW + "Random Compatible Mode", List.of(
                ChatColor.GRAY + "Party size filter applied",
                ChatColor.GRAY + "Click for a random setup menu"
            )));
        } else {
            inventory.setItem(47, create(settings.noCompatibleModeIcon(), ChatColor.RED + "No Compatible Modes", List.of(
                ChatColor.GRAY + "Your current party size does not fit",
                ChatColor.GRAY + "any configured mode"
            )));
        }

        List<Integer> slots = interactiveSlots(inventory.getSize());
        for (int index = 0; index < Math.min(slots.size(), view.modes().size()); index++) {
            ModeOption mode = view.modes().get(index);
            int slot = slots.get(index);
            holder.modeBySlot.put(slot, mode.key());
            inventory.setItem(slot, create(mode.icon(), mode.displayName(), List.of(
                ChatColor.GRAY + "Team sizes: " + ChatColor.WHITE + mode.teamSizes(),
                ChatColor.GRAY + "Party fit: " + mode.partyFit(),
                ChatColor.YELLOW + "Click to configure kit and launch"
            )));
        }
        player.openInventory(inventory);
    }

    public void openSetupMenu(Player player, SetupView view) {
        SetupMenuHolder holder = new SetupMenuHolder(player.getUniqueId(), view.modeKey(), view.kitName());
        Inventory inventory = Bukkit.createInventory(holder, settings.setupSize(), settings.setupTitle());
        fill(inventory, settings.setupFiller(), " ");

        inventory.setItem(4, create(view.modeIcon(), view.modeDisplayName(), List.of(
            ChatColor.GRAY + "Team sizes: " + ChatColor.WHITE + view.teamSizes(),
            ChatColor.GRAY + "Total players: " + ChatColor.WHITE + view.totalPlayers(),
            ChatColor.GRAY + "Arena slots online: " + ChatColor.WHITE + view.availableArenas()
        )));
        inventory.setItem(11, create(settings.rosterIcon(), ChatColor.AQUA + "Party Roster", view.partyLore()));
        inventory.setItem(13, create(settings.rulesIcon(), ChatColor.GOLD + "Arena Rules", List.of(
            ChatColor.GRAY + "Friendly fire is disabled",
            ChatColor.GRAY + "Players keep SMP inventories via snapshots",
            ChatColor.GRAY + "Losing players respawn into the results menu"
        )));
        inventory.setItem(15, create(settings.checksIcon(), ChatColor.RED + "Launch Checks", view.requirementLore()));

        holder.selectKitSlot = 28;
        holder.startSlot = 31;
        holder.backSlot = 49;

        inventory.setItem(28, create(settings.selectedKitIcon(), ChatColor.YELLOW + "Change Kit", List.of(
            ChatColor.GRAY + "Open the kit menu and",
            ChatColor.GRAY + "choose from the full list"
        )));
        inventory.setItem(31, create(settings.startIcon(), ChatColor.GREEN + "Start Match", List.of(
            ChatColor.GRAY + "Mode: " + ChatColor.WHITE + ChatColor.stripColor(view.modeDisplayName()),
            ChatColor.GRAY + "Kit: " + ChatColor.WHITE + view.kitName(),
            ChatColor.YELLOW + "Click to launch"
        )));
        inventory.setItem(49, create(settings.backIcon(), ChatColor.RED + "Back", List.of(
            ChatColor.GRAY + "Return to the PvP hub"
        )));
        inventory.setItem(20, create(settings.selectedKitIcon(), ChatColor.RED + "Selected Kit", List.of(
            ChatColor.GRAY + "Current choice: " + ChatColor.WHITE + view.kitName(),
            ChatColor.GRAY + "Cycle or randomize before launch"
        )));
        inventory.setItem(22, create(settings.selectedKitIcon(), ChatColor.RED + view.kitName(), view.kitLore()));
        inventory.setItem(24, create(Material.CLOCK, ChatColor.GOLD + "Session Flow", List.of(
            ChatColor.GRAY + "1. Snapshot everyone",
            ChatColor.GRAY + "2. Teleport into the arena",
            ChatColor.GRAY + "3. Return with one click after results"
        )));
        player.openInventory(inventory);
    }

    public void openKitMenu(Player player, KitPickerView view) {
        KitMenuHolder holder = new KitMenuHolder(player.getUniqueId(), view.modeKey(), view.selectedKitName(), view.page(), view.totalPages());
        Inventory inventory = Bukkit.createInventory(holder, settings.setupSize(), ChatColor.DARK_RED + "Select PvP Kit");
        fill(inventory, settings.setupFiller(), " ");

        inventory.setItem(4, create(settings.selectedKitIcon(), ChatColor.GOLD + "Kit Selection", List.of(
            ChatColor.GRAY + "Mode: " + ChatColor.WHITE + ChatColor.stripColor(view.modeDisplayName()),
            ChatColor.GRAY + "Page: " + ChatColor.WHITE + (view.page() + 1) + "/" + view.totalPages(),
            ChatColor.GRAY + "Selected: " + ChatColor.WHITE + view.selectedKitName()
        )));
        holder.backSlot = 49;
        inventory.setItem(49, create(settings.backIcon(), ChatColor.RED + "Back", List.of(
            ChatColor.GRAY + "Return to the mode setup menu"
        )));

        if (view.totalPages() > 1) {
            holder.previousPageSlot = 45;
            holder.nextPageSlot = 53;
            inventory.setItem(45, create(settings.previousKitIcon(), ChatColor.YELLOW + "Previous Page", List.of(
                ChatColor.GRAY + "Go to page " + (Math.floorMod(view.page() - 1, view.totalPages()) + 1)
            )));
            inventory.setItem(53, create(settings.nextKitIcon(), ChatColor.YELLOW + "Next Page", List.of(
                ChatColor.GRAY + "Go to page " + (Math.floorMod(view.page() + 1, view.totalPages()) + 1)
            )));
        }

        for (int index = 0; index < Math.min(KIT_PICKER_SLOTS.size(), view.kits().size()); index++) {
            KitOption option = view.kits().get(index);
            int slot = KIT_PICKER_SLOTS.get(index);
            holder.kitNameBySlot.put(slot, option.name());
            inventory.setItem(slot, create(
                option.selected() ? settings.startIcon() : settings.randomKitIcon(),
                (option.selected() ? ChatColor.GREEN : ChatColor.AQUA) + option.name(),
                option.lore()
            ));
        }
        player.openInventory(inventory);
    }

    public int kitPageSize() {
        return KIT_PICKER_SLOTS.size();
    }

    public void openPostMatchMenu(Player player, String winnerLine) {
        PostMatchMenuHolder holder = new PostMatchMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, settings.resultSize(), settings.resultTitle());
        fill(inventory, settings.resultFiller(), " ");
        inventory.setItem(11, create(settings.resultReturnIcon(), ChatColor.GREEN + "Return to SMP", List.of(
            ChatColor.GRAY + "Restore your SMP location",
            ChatColor.GRAY + "and your original inventory"
        )));
        inventory.setItem(13, create(settings.resultRematchIcon(), ChatColor.RED + "Rematch", List.of(
            ChatColor.GRAY + winnerLine,
            ChatColor.YELLOW + "Start the same PvP again"
        )));
        inventory.setItem(15, create(settings.resultNewMatchIcon(), ChatColor.GOLD + "New PvP", List.of(
            ChatColor.GRAY + "Pick a new mode and kit"
        )));
        holder.returnSlot = 11;
        holder.rematchSlot = 13;
        holder.newMatchSlot = 15;
        player.openInventory(inventory);
    }

    private void fill(Inventory inventory, Material material, String name) {
        ItemStack filler = create(material, name, List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack create(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private List<Integer> interactiveSlots(int inventorySize) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 10; slot < inventorySize - 18; slot++) {
            int column = slot % 9;
            if (column == 0 || column == 8) {
                continue;
            }
            slots.add(slot);
        }
        return slots;
    }

    public record ModeOption(String key, String displayName, String teamSizes, String partyFit, Material icon) {}

    public record HubView(
        List<ModeOption> modes,
        List<String> partyLore,
        boolean canRematch,
        boolean canReturn,
        int availableArenas,
        int busyArenas,
        String randomModeKey
    ) {}

    public record SetupView(
        String modeKey,
        String modeDisplayName,
        String teamSizes,
        int totalPlayers,
        Material modeIcon,
        int availableArenas,
        List<String> partyLore,
        List<String> requirementLore,
        String kitName,
        List<String> kitLore
    ) {}

    public record KitPickerView(
        String modeKey,
        String modeDisplayName,
        String selectedKitName,
        int page,
        int totalPages,
        List<KitOption> kits
    ) {}

    public record KitOption(String name, List<String> lore, boolean selected) {}

    public static final class HubMenuHolder implements InventoryHolder {
        private final UUID ownerId;
        private final Map<Integer, String> modeBySlot = new HashMap<>();
        private int randomSlot = -1;
        private int rematchSlot = -1;
        private int returnSlot = -1;

        private HubMenuHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        public UUID ownerId() {
            return ownerId;
        }

        public String modeAt(int slot) {
            return modeBySlot.get(slot);
        }

        public boolean isRandomSlot(int slot) {
            return slot == randomSlot;
        }

        public boolean isRematchSlot(int slot) {
            return slot == rematchSlot;
        }

        public boolean isReturnSlot(int slot) {
            return slot == returnSlot;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static final class SetupMenuHolder implements InventoryHolder {
        private final UUID ownerId;
        private final String modeKey;
        private final String selectedKitName;
        private int selectKitSlot = -1;
        private int startSlot = -1;
        private int backSlot = -1;

        private SetupMenuHolder(UUID ownerId, String modeKey, String selectedKitName) {
            this.ownerId = ownerId;
            this.modeKey = modeKey;
            this.selectedKitName = selectedKitName;
        }

        public UUID ownerId() {
            return ownerId;
        }

        public String modeKey() {
            return modeKey;
        }

        public String selectedKitName() {
            return selectedKitName;
        }

        public boolean isSelectKitSlot(int slot) {
            return slot == selectKitSlot;
        }

        public boolean isStartSlot(int slot) {
            return slot == startSlot;
        }

        public boolean isBackSlot(int slot) {
            return slot == backSlot;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static final class KitMenuHolder implements InventoryHolder {
        private final UUID ownerId;
        private final String modeKey;
        private final String selectedKitName;
        private final int page;
        private final int totalPages;
        private final Map<Integer, String> kitNameBySlot = new HashMap<>();
        private int backSlot = -1;
        private int previousPageSlot = -1;
        private int nextPageSlot = -1;

        private KitMenuHolder(UUID ownerId, String modeKey, String selectedKitName, int page, int totalPages) {
            this.ownerId = ownerId;
            this.modeKey = modeKey;
            this.selectedKitName = selectedKitName;
            this.page = page;
            this.totalPages = totalPages;
        }

        public UUID ownerId() {
            return ownerId;
        }

        public String modeKey() {
            return modeKey;
        }

        public String selectedKitName() {
            return selectedKitName;
        }

        public int page() {
            return page;
        }

        public int totalPages() {
            return totalPages;
        }

        public String kitNameAt(int slot) {
            return kitNameBySlot.get(slot);
        }

        public boolean isBackSlot(int slot) {
            return slot == backSlot;
        }

        public boolean isPreviousPageSlot(int slot) {
            return slot == previousPageSlot;
        }

        public boolean isNextPageSlot(int slot) {
            return slot == nextPageSlot;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static final class PostMatchMenuHolder implements InventoryHolder {
        private final UUID ownerId;
        private int returnSlot = -1;
        private int rematchSlot = -1;
        private int newMatchSlot = -1;

        private PostMatchMenuHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        public UUID ownerId() {
            return ownerId;
        }

        public boolean isReturnSlot(int slot) {
            return slot == returnSlot;
        }

        public boolean isRematchSlot(int slot) {
            return slot == rematchSlot;
        }

        public boolean isNewMatchSlot(int slot) {
            return slot == newMatchSlot;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
