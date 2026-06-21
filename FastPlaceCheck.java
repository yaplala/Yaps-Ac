package dk.zai.anticheat.checks.movement;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * NoWeb detection.
 * Flags if a player moves faster than min-web-speed while standing
 * inside cobweb. Vanilla reduces movement in cobweb to a near-crawl.
 */
public class NoWebCheck extends Check {

    private final double minWebSpeed;

    public NoWebCheck(AntiCheatPlugin plugin) {
        super(plugin, "NoWeb", "movement.noweb");
        this.minWebSpeed = cfg().getDouble("min-web-speed", 0.05);
    }

    public void onMove(Player player, PlayerMoveEvent event) {
        if (!enabled || bypass(player)) return;
        if (player.isFlying() || player.isGliding()) return;

        Location to = event.getTo();
        if (to == null) return;
        Material feet = to.getBlock().getType();
        if (feet != Material.COBWEB) return;

        Location from = event.getFrom();
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        if (h > minWebSpeed) {
            flag(player, "hSpeed=" + String.format("%.3f", h) + " in cobweb");
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
        }
    }
}
