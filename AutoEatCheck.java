package dk.zai.anticheat.checks.movement;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Speed detection.
 * Flags if horizontal or vertical movement per tick exceeds the
 * configured limits. Wind Charge immunity and elytra/flight are skipped.
 */
public class SpeedCheck extends Check {

    private final double maxHSpeed;
    private final double maxVSpeed;

    public SpeedCheck(AntiCheatPlugin plugin) {
        super(plugin, "Speed", "movement.speed");
        this.maxHSpeed = cfg().getDouble("max-horizontal-speed", 0.65);
        this.maxVSpeed = cfg().getDouble("max-vertical-speed", 0.6);
    }

    public void onMove(Player player, PlayerMoveEvent event) {
        if (!enabled || bypass(player)) return;
        if (player.isFlying() || player.isGliding() || player.getAllowFlight()) return;
        if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED)) return;
        if (plugin.getDataManager().get(player.getUniqueId()).isWindChargeImmune()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double dy = to.getY() - from.getY();
        double hSpeed = Math.sqrt(dx * dx + dz * dz);

        if (hSpeed > maxHSpeed) {
            flag(player, "hSpeed=" + String.format("%.3f", hSpeed));
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
            return;
        }
        // Vertical speed only checked when ascending (jumping up)
        if (dy > maxVSpeed && !isOnGround(to)) {
            flag(player, "vSpeed=" + String.format("%.3f", dy));
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
        }
    }

    private boolean isOnGround(Location loc) {
        return loc.clone().subtract(0, 0.1, 0).getBlock().getType().isSolid();
    }
}
