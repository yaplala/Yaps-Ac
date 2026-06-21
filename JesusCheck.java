package dk.zai.anticheat.gui;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.ColorUtil;
import dk.zai.anticheat.data.PlayerData;
import dk.zai.anticheat.data.Punishment;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * YAPS AntiCheat admin GUI - Vulcan-style design.
 *
 * Design principles borrowed from Vulcan AntiCheat:
 *   - Yellow primary frame (only border, NOT full background) - keeps the
 *     GUI clean and uncluttered instead of filling every slot.
 *   - Color-coded sections:
 *       Combat    = red    (RED_STAINED_GLASS_PANE)
 *       Movement  = blue   (BLUE_STAINED_GLASS_PANE)
 *       Player    = green  (GREEN_STAINED_GLASS_PANE)
 *       Exploits  = purple (PURPLE_STAINED_GLASS_PANE)
 *   - Each inventory has a yellow border + content slots are EMPTY by default
 *   - Click sound feedback on every interaction
 *   - Player heads show: VL, ping, ban/mute status, IP - all in lore
 *   - Quick-punish buttons arranged in a clean grid
 *
 * Four panels:
 *   1. Main menu       - Players / Checks / Active Bans / Reload
 *   2. Player list     - paginated skull grid of all online players
 *   3. Player detail   - VL info + quick-punish buttons
 *   4. Checks panel    - all 18 checks organized by category, click to toggle
 */
public class YapsGUI {

    // Vulcan-style gradient title - now primarily YELLOW to match the YAPS brand
    public static final String TITLE_MAIN    = "§8» §e§lY§6§lA§e§lP§6§lS §7§lAntiCheat §8«";
    public static final String TITLE_PLAYERS = "§8» §e§lPlayer §7§lManager §8«";
    public static final String TITLE_CHECKS  = "§8» §e§lCheck §7§lManager §8«";
    public static final String TITLE_DETAIL  = "§8» §e§lPlayer §7§lInfo §8«";

    // Border / frame materials
    // Primary frame is now YELLOW - only used for the outer border.
    // Interior slots stay empty for a clean, uncluttered look.
    // ACCENT (the center title pane) is also yellow to match the YAPS brand.
    private static final Material BORDER     = Material.YELLOW_STAINED_GLASS_PANE;
    private static final Material HEADER     = Material.YELLOW_STAINED_GLASS_PANE;
    private static final Material ACCENT     = Material.YELLOW_STAINED_GLASS_PANE;

    // Category colors (Vulcan-style)
    private static final Material COMBAT     = Material.RED_STAINED_GLASS_PANE;
    private static final Material MOVEMENT   = Material.BLUE_STAINED_GLASS_PANE;
    private static final Material PLAYER_CAT = Material.GREEN_STAINED_GLASS_PANE;
    private static final Material EXPLOITS   = Material.PURPLE_STAINED_GLASS_PANE;

    private final AntiCheatPlugin plugin;

    public YapsGUI(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    // Main menu (27 slots = 3 rows)
    // Layout:
    //   Row 0: BORDER BORDER HEADER HEADER HEADER HEADER HEADER BORDER BORDER
    //   Row 1: BORDER [PLAYERS] [CHECKS] [ACTIVE] [RELOAD] BORDER BORDER BORDER
    //   Row 2: BORDER BORDER BORDER BORDER BORDER BORDER BORDER BORDER BORDER
    // =====================================================================

    public void openMain(Player viewer) {
        Inventory inv = Bukkit.createInventory(new GUIHolder("main"), 27,
                ColorUtil.toComponent(TITLE_MAIN));

        // Only draw the outer border - interior stays clean/empty
        fillBorder(inv);

        // Center title at slot 4 (top row middle)
        inv.setItem(4, createItem(ACCENT,
                "§e§lY§6§lA§e§lP§6§lS §7§lAntiCheat",
                "§7v" + plugin.getDescription().getVersion(),
                "",
                "§7Manage players, checks, and",
                "§7punishments from one place."));

        // Main buttons (row 1, middle area)
        inv.setItem(11, createItem(Material.PLAYER_HEAD,
                "§d§lPlayers",
                "§7View all online players",
                "§7with their violation levels.",
                "",
                "§e» Click to open"));

        long enabled = plugin.getChecks().all().stream().filter(c -> c.isEnabled()).count();
        long total = plugin.getChecks().all().size();
        inv.setItem(13, createItem(Material.COMPARATOR,
                "§b§lChecks",
                "§7Toggle individual anti-cheat",
                "§7checks on or off.",
                "",
                "§7Enabled: §a" + enabled + " §8/ §f" + total,
                "§e» Click to open"));

        int bans = plugin.getDataManager().getActiveBans().size();
        inv.setItem(15, createItem(Material.IRON_BARS,
                "§c§lActive Bans",
                "§7View all currently active bans.",
                "",
                "§7Currently banned: §f" + bans,
                "§e» Click to view"));

        // Reload at slot 22 (bottom row middle)
        inv.setItem(22, createItem(Material.EMERALD,
                "§a§lReload",
                "§7Reload the configuration",
                "§7and message files.",
                "",
                "§e» Click to reload"));

        viewer.openInventory(inv);
        viewer.playSound(viewer.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    // =====================================================================
    // Player list (54 slots = 6 rows)
    // Layout:
    //   Row 0: header border
    //   Row 1-4: 36 player skull slots
    //   Row 5: [PREV] [filler...] [BACK] [filler...] [NEXT]
    // =====================================================================

    public void openPlayers(Player viewer, int page) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        int perPage = 36;
        int pages = Math.max(1, (online.size() + perPage - 1) / perPage);
        page = Math.max(1, Math.min(page, pages));

        Inventory inv = Bukkit.createInventory(new GUIHolder("players", page),
                54, ColorUtil.toComponent(TITLE_PLAYERS + " §8[§f" + page + "§8/§f" + pages + "§8]"));

        // Only yellow border - interior empty
        fillBorder(inv);

        // Header center title only (slot 4)
        inv.setItem(4, createItem(ACCENT,
                "§d§lPlayer §7§lManager",
                "§7Page §f" + page + " §7of §f" + pages,
                "§7Online: §f" + online.size()));

        // Player heads (slots 9-44, but only fill what's needed - rest stays empty)
        int from = (page - 1) * perPage;
        int to = Math.min(online.size(), from + perPage);
        for (int i = from; i < to; i++) {
            inv.setItem(9 + (i - from), createPlayerHead(online.get(i)));
        }

        // Bottom row navigation
        inv.setItem(45, createItem(Material.ARROW, "§e§l« Previous", "§7Go to page §f" + Math.max(1, page - 1)));
        inv.setItem(49, createItem(Material.BARRIER, "§c§l« Back", "§7Return to main menu"));
        inv.setItem(53, createItem(Material.ARROW, "§e§lNext »", "§7Go to page §f" + Math.min(pages, page + 1)));

        viewer.openInventory(inv);
        viewer.playSound(viewer.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private ItemStack createPlayerHead(Player target) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(ColorUtil.toComponent("§f" + target.getName()));

        PlayerData pd = plugin.getDataManager().get(target.getUniqueId());
        Punishment ban = plugin.getDataManager().getActiveBan(target.getUniqueId());
        Punishment mute = plugin.getDataManager().getActiveMute(target.getUniqueId());
        int warns = plugin.getDataManager().getActiveWarns(target.getUniqueId()).size();

        int ping = target.getPing();
        String pingColor = ping > 250 ? "§c" : ping > 150 ? "§e" : "§a";

        // VL color coding - Vulcan-style
        String vlColor;
        if (pd.getViolationLevel() == 0) vlColor = "§a";
        else if (pd.getViolationLevel() < 10) vlColor = "§e";
        else if (pd.getViolationLevel() < 30) vlColor = "§6";
        else vlColor = "§c";

        List<Component> lore = new ArrayList<>();
        lore.add(ColorUtil.toComponent("§8§m                  "));
        lore.add(ColorUtil.toComponent("§7 Violation Level: " + vlColor + pd.getViolationLevel()));
        lore.add(ColorUtil.toComponent("§7 Total Flags: §f" + pd.getTotalFlags()));
        lore.add(ColorUtil.toComponent("§7 Ping: " + pingColor + ping + "ms"));
        lore.add(ColorUtil.toComponent("§8§m                  "));
        lore.add(ColorUtil.toComponent("§7 Banned: " + (ban != null ? "§c§l✗ YES" : "§a§l✓ NO")));
        lore.add(ColorUtil.toComponent("§7 Muted:  " + (mute != null ? "§c§l✗ YES" : "§a§l✓ NO")));
        lore.add(ColorUtil.toComponent("§7 Warns:  §e" + warns));
        lore.add(ColorUtil.toComponent("§8§m                  "));
        lore.add(ColorUtil.toComponent("§e» Click to manage"));
        meta.lore(lore);

        meta.addItemFlags(ItemFlag.values());
        head.setItemMeta(meta);
        return head;
    }

    // =====================================================================
    // Player detail (54 slots = 6 rows)
    // Layout:
    //   Row 0: header border
    //   Row 1: [player head] [ban info] [mute info] [warn info]
    //   Row 2: fillers
    //   Row 3: [ban] [tempban] [warn] [kick] [mute] [tempmute] [note]
    //   Row 4: fillers + [reset VL]
    //   Row 5: [unban] [history] [back] [unmute]
    // =====================================================================

    public void openDetail(Player viewer, UUID targetUuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(targetUuid);
        Player target = offline.getPlayer();
        PlayerData pd = plugin.getDataManager().get(targetUuid);
        Punishment ban = plugin.getDataManager().getActiveBan(targetUuid);
        Punishment mute = plugin.getDataManager().getActiveMute(targetUuid);
        int warns = plugin.getDataManager().getActiveWarns(targetUuid).size();
        int histCount = plugin.getDataManager().getHistory(targetUuid).size();

        Inventory inv = Bukkit.createInventory(new GUIHolder("detail", targetUuid),
                54, ColorUtil.toComponent(TITLE_DETAIL + " §7- §f" + pd.getLastName()));

        // Only yellow border - interior empty
        fillBorder(inv);

        // Header center title only (slot 4)
        inv.setItem(4, createItem(ACCENT,
                "§d§lPlayer §7§lInfo",
                "§7" + pd.getLastName()));

        // Player head (slot 10)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (offline.getName() != null) meta.setOwningPlayer(offline);
        meta.displayName(ColorUtil.toComponent("§f§l" + pd.getLastName()));
        List<Component> headLore = new ArrayList<>();
        headLore.add(ColorUtil.toComponent("§8§m                  "));
        headLore.add(ColorUtil.toComponent("§7 Violation Level: §f" + pd.getViolationLevel()));
        headLore.add(ColorUtil.toComponent("§7 Total Flags: §f" + pd.getTotalFlags()));
        headLore.add(ColorUtil.toComponent("§7 Online: " + (target != null ? "§a§l✓ YES" : "§c§l✗ NO")));
        headLore.add(ColorUtil.toComponent("§7 IP: §f" + (pd.getLastIp().isEmpty() ? "-" : pd.getLastIp())));
        if (target != null) {
            int ping = target.getPing();
            String pingColor = ping > 250 ? "§c" : ping > 150 ? "§e" : "§a";
            headLore.add(ColorUtil.toComponent("§7 Ping: " + pingColor + ping + "ms"));
        }
        headLore.add(ColorUtil.toComponent("§8§m                  "));
        meta.lore(headLore);
        meta.addItemFlags(ItemFlag.values());
        head.setItemMeta(meta);
        inv.setItem(10, head);

        // Status panels (slots 12, 14, 16)
        inv.setItem(12, createItem(Material.REDSTONE_BLOCK,
                "§c§lBan Status",
                "§8§m                  ",
                "§7 Banned: " + (ban != null ? "§c§l✗ YES" : "§a§l✓ NO"),
                ban != null ? "§7 Reason: §f" + truncate(ban.getReason(), 30) : "",
                ban != null ? "§7 By: §f" + ban.getOperator() : "",
                ban != null ? "§7 Expires: §f" + (ban.isPermanent() ? "§4NEVER" :
                        plugin.getLayoutsManager().getDateFormat().format(new java.util.Date(ban.getEnd()))) : "",
                "§8§m                  "));

        inv.setItem(14, createItem(Material.WRITABLE_BOOK,
                "§6§lMute Status",
                "§8§m                  ",
                "§7 Muted: " + (mute != null ? "§c§l✗ YES" : "§a§l✓ NO"),
                mute != null ? "§7 Reason: §f" + truncate(mute.getReason(), 30) : "",
                mute != null ? "§7 By: §f" + mute.getOperator() : "",
                mute != null ? "§7 Expires: §f" + (mute.isPermanent() ? "§4NEVER" :
                        plugin.getLayoutsManager().getDateFormat().format(new java.util.Date(mute.getEnd()))) : "",
                "§8§m                  "));

        inv.setItem(16, createItem(Material.PAPER,
                "§e§lWarn Status",
                "§8§m                  ",
                "§7 Active warns: §f" + warns,
                "§7 History entries: §f" + histCount,
                "§8§m                  "));

        // Quick punish buttons (row 3 = slots 28-34)
        inv.setItem(28, createItem(Material.RED_WOOL,
                "§c§lBan",
                "§7Permanently ban this player.",
                "",
                "§e» Click to ban"));

        inv.setItem(29, createItem(Material.ORANGE_WOOL,
                "§6§lTempban",
                "§7Temporary ban for 1 day.",
                "",
                "§e» Click to tempban"));

        inv.setItem(30, createItem(Material.YELLOW_WOOL,
                "§e§lWarn",
                "§7Issue a formal warning.",
                "",
                "§e» Click to warn"));

        inv.setItem(31, createItem(Material.TNT,
                "§4§lKick",
                "§7Kick from the server.",
                "",
                "§e» Click to kick"));

        inv.setItem(32, createItem(Material.GRAY_WOOL,
                "§7§lMute",
                "§7Permanently mute this player.",
                "",
                "§e» Click to mute"));

        inv.setItem(33, createItem(Material.LIGHT_GRAY_WOOL,
                "§8§lTempmute",
                "§7Temp mute for 30 minutes.",
                "",
                "§e» Click to tempmute"));

        inv.setItem(34, createItem(Material.BOOK,
                "§d§lNote",
                "§7Add a private staff note.",
                "",
                "§e» Click to add note"));

        // Reset VL (slot 40)
        inv.setItem(40, createItem(Material.TNT,
                "§4§lReset VL",
                "§7Reset violation level to 0.",
                "",
                "§e» Click to reset"));

        // Unban / Unmute (if active)
        if (ban != null) {
            inv.setItem(46, createItem(Material.EMERALD,
                    "§a§lUnban",
                    "§7Remove the active ban.",
                    "",
                    "§e» Click to unban"));
        }
        if (mute != null) {
            inv.setItem(52, createItem(Material.EMERALD,
                    "§a§lUnmute",
                    "§7Remove the active mute.",
                    "",
                    "§e» Click to unmute"));
        }

        // History button (slot 49)
        inv.setItem(49, createItem(Material.CHEST,
                "§b§lHistory",
                "§7View punishment history in chat.",
                "",
                "§7Entries: §f" + histCount,
                "§e» Click to view"));

        // Back button (slot 45)
        inv.setItem(45, createItem(Material.ARROW,
                "§e§l« Back",
                "§7Return to player list"));

        viewer.openInventory(inv);
        viewer.playSound(viewer.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    // =====================================================================
    // Checks manager (54 slots = 6 rows, organized by category)
    // Layout:
    //   Row 0: header border
    //   Row 1: [COMBAT label] + 4 combat checks
    //   Row 2: [MOVEMENT label] + 7 movement checks
    //   Row 3-4: [PLAYER label] + 5 player checks  /  [EXPLOITS label] + 2 exploits
    //   Row 5: back button
    // =====================================================================

    public void openChecks(Player viewer) {
        Inventory inv = Bukkit.createInventory(new GUIHolder("checks"),
                54, ColorUtil.toComponent(TITLE_CHECKS));

        // Map from slot -> check name (used by listener to find the clicked check
        // without having to parse the display name - which was unreliable)
        java.util.Map<Integer, String> slotToCheck = new java.util.HashMap<>();

        // Only yellow border - interior empty
        fillBorder(inv);

        // Header center title only (slot 4)
        inv.setItem(4, createItem(ACCENT,
                "§d§lCheck §7§lManager",
                "§7Click any check to toggle it."));

        int slot = 9; // start of row 2

        // Combat category (red)
        inv.setItem(slot++, createItem(COMBAT, "§c§lCOMBAT", "§7Combat detection checks"));
        for (var c : plugin.getChecks().all()) {
            if (isInCategory(c, "combat")) {
                inv.setItem(slot, createCheckItem(c));
                slotToCheck.put(slot, c.getName());
                slot++;
            }
        }

        // Movement category (blue)
        inv.setItem(slot++, createItem(MOVEMENT, "§9§lMOVEMENT", "§7Movement detection checks"));
        for (var c : plugin.getChecks().all()) {
            if (isInCategory(c, "movement")) {
                inv.setItem(slot, createCheckItem(c));
                slotToCheck.put(slot, c.getName());
                slot++;
            }
        }

        // Player category (green)
        inv.setItem(slot++, createItem(PLAYER_CAT, "§a§lPLAYER", "§7Player & world detection checks"));
        for (var c : plugin.getChecks().all()) {
            if (isInCategory(c, "player")) {
                inv.setItem(slot, createCheckItem(c));
                slotToCheck.put(slot, c.getName());
                slot++;
            }
        }

        // Exploits category (purple)
        inv.setItem(slot++, createItem(EXPLOITS, "§5§lEXPLOITS", "§7Exploit & packet detection checks"));
        for (var c : plugin.getChecks().all()) {
            if (isInCategory(c, "exploits")) {
                inv.setItem(slot, createCheckItem(c));
                slotToCheck.put(slot, c.getName());
                slot++;
            }
        }

        // Back button (slot 49)
        inv.setItem(49, createItem(Material.BARRIER,
                "§c§l« Back",
                "§7Return to main menu"));

        // Attach the slot->check map to the GUIHolder so the listener can use it
        ((GUIHolder) inv.getHolder()).setDataMap(slotToCheck);

        viewer.openInventory(inv);
        viewer.playSound(viewer.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    private boolean isInCategory(dk.zai.anticheat.checks.Check check, String category) {
        // Determine category by package name
        String pkg = check.getClass().getPackage().getName();
        return pkg.endsWith("." + category);
    }

    private ItemStack createCheckItem(dk.zai.anticheat.checks.Check check) {
        Material mat = check.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE;
        String state = check.isEnabled() ? "§a§lENABLED" : "§c§lDISABLED";
        return createItem(mat,
                "§f" + check.getName(),
                "§8§m                  ",
                "§7 Status: " + state,
                "§8§m                  ",
                "§e» Click to toggle");
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /**
     * Draws only the OUTER border of the inventory with yellow panes.
     * Interior slots stay empty - this keeps the GUI clean and
     * uncluttered (Vulcan-style minimalism).
     */
    private void fillBorder(Inventory inv) {
        ItemStack border = createItem(BORDER, "§e§lYAPS");
        int size = inv.getSize();
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            // Top row, bottom row, left column, right column
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inv.setItem(i, border);
            }
        }
    }

    /** Legacy full-fill helper - kept but no longer used by default. */
    private void fillAll(Inventory inv, Material mat) {
        ItemStack filler = createItem(mat, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    public ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.toComponent(name));
            if (lore.length > 0) {
                List<Component> loreList = new ArrayList<>();
                for (String l : lore) {
                    if (l.isEmpty()) {
                        loreList.add(Component.empty());
                    } else {
                        loreList.add(ColorUtil.toComponent(l));
                    }
                }
                meta.lore(loreList);
            }
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    /** Holder used to tag each inventory with its GUI type + optional UUID/page.
     *  Also supports a slot->check-name map for the checks panel. */
    public static class GUIHolder implements InventoryHolder {
        private final String type;
        private final Object data;
        private java.util.Map<Integer, String> dataMap;

        public GUIHolder(String type) { this(type, null); }
        public GUIHolder(String type, Object data) { this.type = type; this.data = data; }

        public String getType() { return type; }
        @SuppressWarnings("unchecked")
        public <T> T getData() { return (T) data; }

        public java.util.Map<Integer, String> getDataMap() { return dataMap; }
        public void setDataMap(java.util.Map<Integer, String> map) { this.dataMap = map; }

        private Inventory inventory;
        @Override public Inventory getInventory() { return inventory; }
        public void setInventory(Inventory inv) { this.inventory = inv; }
    }
}
