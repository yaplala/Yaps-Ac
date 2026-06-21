package dk.zai.anticheat.gui;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.ColorUtil;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Routes clicks in any YAPS GUI to the right action.
 * Uses the GUIHolder tag on the inventory to know which panel is open.
 *
 * All clicks are cancelled by default (locked inventory) and we play
 * a click sound on every interaction for Vulcan-style feedback.
 */
public class GUIListener implements Listener {

    private final AntiCheatPlugin plugin;
    private final YapsGUI gui;

    public GUIListener(AntiCheatPlugin plugin, YapsGUI gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof YapsGUI.GUIHolder gh)) return;

        event.setCancelled(true); // lock inventory

        if (event.getSlotType() != InventoryType.SlotType.CONTAINER) return;
        if (event.getCurrentItem() == null) return;
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        String type = gh.getType();
        int slot = event.getRawSlot();

        switch (type) {
            case "main"    -> handleMain(viewer, slot);
            case "players" -> handlePlayers(viewer, slot, gh.getData());
            case "detail"  -> handleDetail(viewer, slot, gh.getData());
            case "checks"  -> handleChecks(viewer, slot, event.getCurrentItem());
        }
    }

    private void handleMain(Player viewer, int slot) {
        // Updated slots matching YapsGUI.openMain layout (v1.3.1+):
        //   4  = title (no action)
        //   11 = Players
        //   13 = Checks
        //   15 = Active Bans
        //   22 = Reload
        switch (slot) {
            case 11 -> gui.openPlayers(viewer, 1);
            case 13 -> gui.openChecks(viewer);
            case 15 -> {
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "banlist");
            }
            case 22 -> {
                // Reload config from disk + refresh all in-memory state
                plugin.reloadAll();
                viewer.playSound(viewer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                viewer.sendMessage(ColorUtil.toComponent(
                        plugin.getConfig().getString("messages.prefix", "") +
                        plugin.getConfig().getString("messages.reload-success", "&aReloaded.")));
                // Reopen main menu so button states (e.g. enabled-check count) refresh
                gui.openMain(viewer);
            }
        }
    }

    private void handlePlayers(Player viewer, int slot, Object data) {
        int page = (data instanceof Integer) ? (Integer) data : 1;

        if (slot == 45) { gui.openPlayers(viewer, Math.max(1, page - 1)); return; }
        if (slot == 49) { gui.openMain(viewer); return; }
        if (slot == 53) {
            int online = Bukkit.getOnlinePlayers().size();
            int pages = Math.max(1, (online + 35) / 36);
            if (page < pages) gui.openPlayers(viewer, page + 1);
            return;
        }

        // Player head clicked (slots 9-44)
        if (slot >= 9 && slot <= 44) {
            var online = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());
            int from = (page - 1) * 36;
            int idx = from + (slot - 9);
            if (idx >= 0 && idx < online.size()) {
                UUID uuid = online.get(idx).getUniqueId();
                gui.openDetail(viewer, uuid);
            }
        }
    }

    private void handleDetail(Player viewer, int slot, Object data) {
        if (!(data instanceof UUID targetUuid)) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        String name = target.getName() != null ? target.getName()
                : plugin.getDataManager().get(targetUuid).getLastName();

        switch (slot) {
            case 28 -> { // Ban
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "ban " + name + " Banned via YAPS GUI");
            }
            case 29 -> { // Tempban 1d
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "tempban " + name + " 1d Banned via YAPS GUI");
            }
            case 30 -> { // Warn
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "warn " + name + " Warned via YAPS GUI");
            }
            case 31 -> { // Kick
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "kick " + name + " Kicked via YAPS GUI");
            }
            case 32 -> { // Mute
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "mute " + name + " Muted via YAPS GUI");
            }
            case 33 -> { // Tempmute 30m
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "tempmute " + name + " 30m Muted via YAPS GUI");
            }
            case 34 -> { // Note
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "note " + name + " Noted via YAPS GUI");
            }
            case 40 -> { // Reset VL
                plugin.getDataManager().resetPlayerVL(targetUuid);
                viewer.playSound(viewer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                gui.openDetail(viewer, targetUuid);
            }
            case 45 -> { // Back
                gui.openPlayers(viewer, 1);
            }
            case 46 -> { // Unban
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "unban " + name);
            }
            case 49 -> { // History in chat
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "history " + name);
            }
            case 52 -> { // Unmute
                viewer.closeInventory();
                Bukkit.dispatchCommand(viewer, "unmute " + name);
            }
        }
    }

    private void handleChecks(Player viewer, int slot, ItemStack clicked) {
        if (clicked == null) return;

        // Back button (barrier at slot 49)
        if (clicked.getType() == Material.BARRIER) {
            gui.openMain(viewer);
            return;
        }

        // Look up the check name from the slot->check map stored in the GUIHolder.
        // This is far more reliable than parsing the ItemStack display name.
        InventoryHolder holder = viewer.getOpenInventory().getTopInventory().getHolder();
        if (!(holder instanceof YapsGUI.GUIHolder gh)) return;
        java.util.Map<Integer, String> slotMap = gh.getDataMap();
        if (slotMap == null) return;
        String checkName = slotMap.get(slot);
        if (checkName == null) return;

        // Find matching check by name
        Check target = null;
        for (Check c : plugin.getChecks().all()) {
            if (c.getName().equals(checkName)) {
                target = c;
                break;
            }
        }
        if (target == null) {
            viewer.sendMessage(ColorUtil.toComponent("§cCould not find check: " + checkName));
            return;
        }

        // Toggle the check using the new Check.toggle() method.
        // This updates BOTH in-memory state and config.yml on disk
        // (saveConfig only - we do NOT reloadConfig, so the toggle persists).
        boolean newState = !target.isEnabled();
        target.toggle();
        // Refresh dependent managers (no reloadConfig - just refresh in-memory)
        plugin.refreshChecks();

        viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 1f,
                newState ? 1.5f : 0.5f);
        viewer.sendMessage(ColorUtil.toComponent(
                plugin.getConfig().getString("messages.prefix", "")
                + "§7Check §f" + target.getName() + " §7is now "
                + (newState ? "§a§lENABLED" : "§c§lDISABLED")));

        // Reopen checks panel to reflect the new state
        gui.openChecks(viewer);
    }
}
