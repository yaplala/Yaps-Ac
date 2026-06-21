package dk.zai.anticheat.checks.movement;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Fly detection.
 * Flags if a player stays airborne for more than max-air-ticks without
 * jumping, levitating, having an elytra, etc. - and their vertical
 * velocity exceeds vertical-tolerance.
 */
public class FlyCheck extends Check {

    private final int maxAirTicks;
    private final double verticalTolerance;

    public FlyCheck(AntiCheatPlugin plugin) {
        super(plugin, "Fly", "movement.fly");
        this.maxAirTicks = cfg().getInt("max-air-ticks", 30);
        this.verticalTolerance = cfg().getDouble("vertical-tolerance", 0.05);
    }

    public void onMove(Player player, PlayerMoveEvent event) {
        if (!enabled || bypass(player)) return;
        if (player.isFlying() || player.isGliding() || player.getAllowFlight()) return;
        if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION)) return;
        if (plugin.getDataManager().get(player.getUniqueId()).isWindChargeImmune()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        boolean fromOnGround = isOnGround(from);
        boolean toOnGround = isOnGround(to);
        if (fromOnGround && toOnGround) return;

        // Count air ticks via velocity Y > 0 sustained
        double dy = to.getY() - from.getY();
        if (player.getVelocity().getY() > verticalTolerance && !toOnGround && !player.getLocation().getBlock().isLiquid()) {
            int airTicks = player.getFallDistance() > 0 ? 0 : estimateAirTicks(player);
            if (airTicks > maxAirTicks) {
                flag(player, "airTicks=" + airTicks + " dy=" + String.format("%.3f", dy));
                if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private int estimateAirTicks(Player p) {
        // Approximation: if velocity Y is small positive while airborne, count
        // ticks since last ground contact using fallDistance as inverse proxy.
        return Math.max(0, (int) (p.getVelocity().getY() * 20) + 20);
    }

    private boolean isOnGround(Location loc) {
        return loc.clone().subtract(0, 0.1, 0).getBlock().getType().isSolid()
                || loc.clone().subtract(0, 0.1, 0).getBlock().isLiquid();
    }
}
