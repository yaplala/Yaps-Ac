package dk.zai.anticheat.checks.player;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * AutoArmor detection.
 * Flags if a player equips armor within max-equip-ms of picking it up,
 * which is a hallmark of AutoArmor modules that instantly swap armor.
 */
public class AutoArmorCheck extends Check {

    private final long maxEquipMs;

    public AutoArmorCheck(AntiCheatPlugin plugin) {
        super(plugin, "AutoArmor", "player.autoarmor");
        this.maxEquipMs = cfg().getLong("max-equip-ms", 50);
    }

    public void onPickup(Player player, PlayerPickupItemEvent event) {
        if (!enabled || bypass(player)) return;
        ItemStack is = event.getItem().getItemStack();
        String name = is.getType().name();
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
            plugin.getDataManager().get(player.getUniqueId()).setLastArmorPickup(System.currentTimeMillis());
        }
    }

    public void onInventoryClick(Player player, InventoryClickEvent event) {
        if (!enabled || bypass(player)) return;
        if (event.getSlot() < 5 || event.getSlot() > 8) return; // armor slots
        var pd = plugin.getDataManager().get(player.getUniqueId());
        long last = pd.getLastArmorPickup();
        if (last <= 0) return;
        long dt = System.currentTimeMillis() - last;
        if (dt < maxEquipMs) {
            flag(player, "armor equip dt=" + dt + "ms after pickup");
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
        }
        pd.setLastArmorPickup(0L);
    }
}
