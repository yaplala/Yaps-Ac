package dk.zai.anticheat.checks.combat;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;
import java.util.UUID;

/**
 * KillAura / MultiAura detection.
 * Flags if:
 *   - Player hits a target whose bounding box is more than max-angle degrees
 *     away from the player's facing direction (impossible angle).
 *   - Player hits more than multi-target-max different targets within
 *     multi-target-window-ticks.
 */
public class KillAuraCheck extends Check {

    private final double maxAngle;
    private final long multiTargetWindowTicks;
    private final int multiTargetMax;

    public KillAuraCheck(AntiCheatPlugin plugin) {
        super(plugin, "KillAura", "combat.killaura");
        this.maxAngle = cfg().getDouble("max-angle", 75.0);
        this.multiTargetWindowTicks = cfg().getLong("multi-target-window-ticks", 10);
        this.multiTargetMax = cfg().getInt("multi-target-max", 2);
    }

    public void onAttack(Player attacker, Entity target, EntityDamageByEntityEvent event) {
        if (!enabled || bypass(attacker)) return;
        var pd = plugin.getDataManager().get(attacker.getUniqueId());

        // 1) Angle check
        double angle = angleToEntity(attacker, target);
        if (angle > maxAngle) {
            flag(attacker, "angle=" + String.format("%.1f", angle) + "deg");
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
            return;
        }

        // 2) Multi-target check
        long now = System.currentTimeMillis();
        long windowMs = multiTargetWindowTicks * 50L;
        if (now - pd.getRecentTargetWindow() > windowMs) {
            pd.getRecentTargets().clear();
            pd.setRecentTargetWindow(now);
        }
        UUID tid = target.getUniqueId();
        if (!pd.getRecentTargets().contains(tid)) {
            pd.getRecentTargets().add(tid);
            if (pd.getRecentTargets().size() > multiTargetMax) {
                flag(attacker, "multi=" + pd.getRecentTargets().size() + " targets");
                if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
            }
        }
    }

    /** Angle in degrees between attacker's facing direction and the vector to target. */
    private double angleToEntity(Player attacker, Entity target) {
        var loc = attacker.getEyeLocation();
        var to = target.getLocation().add(0, target.getHeight() / 2, 0).subtract(loc).toVector();
        var dir = loc.getDirection();
        double dot = to.normalize().dot(dir);
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
    }
}
