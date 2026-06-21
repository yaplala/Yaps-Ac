package dk.zai.anticheat.checks.movement;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Step detection.
 * Flags if a player ascends more than max-step-height in a single tick
 * without jumping (no slime, no step enchantment). Vanilla jump is 0.25
 * initial velocity, max ~0.42 single-tick Y delta is possible from a
 * jump. We use a conservative 0.6 default.
 */
public class StepCheck extends Check {

    private final double maxHeight;

    public StepCheck(AntiCheatPlugin plugin) {
        super(plugin, "Step", "movement.step");
        this.maxHeight = cfg().getDouble("max-step-height", 0.6);
    }

    public void onMove(Player player, PlayerMoveEvent event) {
        if (!enabled || bypass(player)) return;
        if (player.isFlying() || player.isGliding()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        double dy = to.getY() - from.getY();
        if (dy <= maxHeight) return;
        if (isOnGround(from)) return; // Jump from ground is allowed

        flag(player, "step dy=" + String.format("%.3f", dy));
        if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
    }

    private boolean isOnGround(Location loc) {
        return loc.clone().subtract(0, 0.1, 0).getBlock().getType().isSolid();
    }
}
