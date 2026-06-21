package dk.zai.anticheat.checks.movement;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * NoSlowdown detection.
 * Flags if the player moves faster than min-speed-while-using while
 * eating, drawing a bow, or blocking with a shield. Vanilla heavily
 * reduces movement during these actions.
 */
public class NoSlowdownCheck extends Check {

    private final double minSpeedWhileUsing;

    public NoSlowdownCheck(AntiCheatPlugin plugin) {
        super(plugin, "NoSlowdown", "movement.noslowdown");
        this.minSpeedWhileUsing = cfg().getDouble("min-speed-while-using", 0.25);
    }

    public void onMove(Player player, PlayerMoveEvent event) {
        if (!enabled || bypass(player)) return;
        if (player.isFlying() || player.isGliding()) return;

        boolean using = player.isHandRaised()
                || player.isBlocking()
                || player.hasPotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
        if (!using) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        if (h > minSpeedWhileUsing) {
            flag(player, "hSpeed=" + String.format("%.3f", h) + " while using item");
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
        }
    }
}
