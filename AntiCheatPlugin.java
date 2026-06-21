package dk.zai.anticheat.listeners;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.player.*;
import dk.zai.anticheat.checks.exploits.InvalidPacketsCheck;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

/**
 * Aggregates all player/world checks: FastPlace, FastBreak, AutoEat,
 * AutoArmor. Each check keeps its own per-player timing state.
 */
public class PlayerWorldListener implements Listener {

    private final AntiCheatPlugin plugin;
    private final FastPlaceCheck fastPlace;
    private final FastBreakCheck fastBreak;
    private final AutoEatCheck autoEat;
    private final AutoArmorCheck autoArmor;
    private final InvalidPacketsCheck invalidPackets;

    public PlayerWorldListener(AntiCheatPlugin plugin) {
        this.plugin = plugin;
        this.fastPlace = plugin.getChecks().fastPlace;
        this.fastBreak = plugin.getChecks().fastBreak;
        this.autoEat = plugin.getChecks().autoEat;
        this.autoArmor = plugin.getChecks().autoArmor;
        this.invalidPackets = plugin.getChecks().invalidPackets;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        invalidPackets.onPacket(event.getPlayer());
        fastPlace.onPlace(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        invalidPackets.onPacket(event.getPlayer());
        fastBreak.onBreak(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction().isRightClick()
                && event.getPlayer().isHandRaised()) {
            autoEat.onStartEating(event.getPlayer());
        }
        invalidPackets.onPacket(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        autoEat.onConsume(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player p)) return;
        invalidPackets.onPacket(p);
        autoArmor.onInventoryClick(p, event);
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPickup(org.bukkit.event.player.PlayerPickupItemEvent event) {
        autoArmor.onPickup(event.getPlayer(), event);
    }
}
