package dk.zai.anticheat.checks.player;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * NoFall detection.
 * Flags if the client reports onGround=true while the player is still
 * falling faster than min-fall-speed-for-flag, which is the classic
 * NoFall packet spoof to reset fall distance and avoid fall damage.
 */
public class NoFallCheck extends Check {

    private final double minFallSpeedForFlag;

    public NoFallCheck(AntiCheatPlugin plugin) {
        super(plugin, "NoFall", "player.nofall");
        this.minFallSpeedForFlag = cfg().getDouble("min-fall-speed-for-flag", -0.6);
    }

    public void onMove(Player player, PlayerMoveEvent event) {
        if (!enabled || bypass(player)) return;
        if (player.isFlying() || player.isGliding()) return;
        if (plugin.getDataManager().get(player.getUniqueId()).isWindChargeImmune()) return;

        var pd = plugin.getDataManager().get(player.getUniqueId());
        boolean reportedGround = player.isOnGround();
        double fallSpeed = player.getVelocity().getY();
        double fallDist = player.getFallDistance();

        // Track last state to detect impossible transitions
        if (reportedGround && fallSpeed < minFallSpeedForFlag && fallDist > 2.0) {
            // Client claims on ground while falling fast - NoFall spoof
            flag(player, "onGround=true y=" + String.format("%.2f", fallSpeed)
                    + " fallDist=" + String.format("%.2f", fallDist));
            // Force server-side damage to undo NoFall benefit
            double dmg = (int) (fallDist - 3.0);
            if (dmg > 0) {
                final double finalDmg = dmg;
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                        player.damage(finalDmg));
            }
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
        }
        pd.setLastFallDistance(fallDist);
        pd.setLastReportedGround(reportedGround);
    }
}
