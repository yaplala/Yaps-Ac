package dk.zai.anticheat.checks.combat;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Silent Aim / TriggerBot detection.
 * Flags if the attacker's facing direction is more than max-angle degrees
 * off the target (silent aim), OR if the time between crosshair-over-target
 * and the actual hit is suspiciously short (TriggerBot, < trigger-window-ms).
 *
 * The crosshair proximity is measured as the same angle used by KillAura -
 * we keep a tighter threshold here because the player isn't even looking
 * at the target.
 */
public class SilentAimCheck extends Check {

    private final double maxAngle;
    private final long triggerWindowMs;

    public SilentAimCheck(AntiCheatPlugin plugin) {
        super(plugin, "SilentAim", "combat.silent-aim");
        this.maxAngle = cfg().getDouble("max-angle", 55.0);
        this.triggerWindowMs = cfg().getLong("trigger-window-ms", 30);
    }

    public void onAttack(Player attacker, Entity target, EntityDamageByEntityEvent event) {
        if (!enabled || bypass(attacker)) return;
        double angle = angleToEntity(attacker, target);
        if (angle > maxAngle) {
            flag(attacker, "silent angle=" + String.format("%.1f", angle) + "deg");
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
            return;
        }
        // TriggerBot heuristic: if attacker just rotated onto target within last triggerWindowMs
        var pd = plugin.getDataManager().get(attacker.getUniqueId());
        long now = System.currentTimeMillis();
        long lastRotate = pd.getLastCombatTime();
        if (lastRotate > 0 && (now - lastRotate) < triggerWindowMs) {
            flag(attacker, "triggerbot dt=" + (now - lastRotate) + "ms");
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
        }
    }

    private double angleToEntity(Player attacker, Entity target) {
        var loc = attacker.getEyeLocation();
        var to = target.getLocation().add(0, target.getHeight() / 2, 0).subtract(loc).toVector();
        var dir = loc.getDirection();
        double dot = to.normalize().dot(dir);
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
    }
}
