package dk.zai.anticheat.checks.movement;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Spider detection.
 *
 * Vanilla jump mechanics: when you jump, you get ~4 ticks of upward
 * movement (dy ~0.42, 0.36, 0.30, 0.24, 0.18) before gravity pulls
 * you back down. A spider hack maintains consistent upward movement
 * against a wall for many consecutive ticks.
 *
 * This check tracks consecutive "ascending against wall" ticks via
 * player metadata, and only flags when the count exceeds
 * min-climb-ticks (default 8 - well beyond what a vanilla jump
 * produces). It also resets the counter on every ground contact,
 * so jumping up a staircase next to a wall never false-flags.
 */
public class SpiderCheck extends Check {

    private final double minClimbSpeed;
    private final int minClimbTicks;

    public SpiderCheck(AntiCheatPlugin plugin) {
        super(plugin, "Spider", "movement.spider");
        this.minClimbSpeed = cfg().getDouble("min-climb-speed", 0.18);
        this.minClimbTicks = cfg().getInt("min-climb-ticks", 8);
    }

    public void onMove(Player player, PlayerMoveEvent event) {
        if (!enabled || bypass(player)) return;
        if (player.isFlying() || player.isGliding()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        double dy = to.getY() - from.getY();

        // Reset counter when player is on ground (jump start, landing, walking)
        if (isOnGround(from) || isOnGround(to)) {
            if (player.hasMetadata("zac_spider")) {
                player.removeMetadata("zac_spider", plugin);
            }
            return;
        }

        // Not ascending? Reset and return.
        if (dy < minClimbSpeed) {
            if (player.hasMetadata("zac_spider")) {
                player.removeMetadata("zac_spider", plugin);
            }
            return;
        }

        // Skip climbable blocks - vanilla allows climbing these
        Material feet = to.getBlock().getType();
        if (feet == Material.LADDER || feet == Material.VINE
                || feet == Material.SCAFFOLDING || feet == Material.WATER
                || feet == Material.LAVA || feet == Material.POINTED_DRIPSTONE) {
            if (player.hasMetadata("zac_spider")) {
                player.removeMetadata("zac_spider", plugin);
            }
            return;
        }

        // Check for adjacent solid wall
        boolean wallAdjacent = false;
        for (int x = -1; x <= 1 && !wallAdjacent; x++) {
            for (int z = -1; z <= 1 && !wallAdjacent; z++) {
                if (x == 0 && z == 0) continue;
                Material m = to.clone().add(x, 0, z).getBlock().getType();
                if (m.isSolid() && m != Material.LADDER && m != Material.VINE
                        && m != Material.SCAFFOLDING) {
                    wallAdjacent = true;
                }
            }
        }

        if (wallAdjacent) {
            // Increment sustained-climb counter
            int count = player.getMetadata("zac_spider").stream()
                    .findFirst().map(v -> v.asInt()).orElse(0);
            count++;
            player.setMetadata("zac_spider", new FixedMetadataValue(plugin, count));

            if (count >= minClimbTicks) {
                flag(player, "climbing wall " + count + " ticks dy=" + String.format("%.3f", dy));
                if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) {
                    event.setCancelled(true);
                }
                // Reset so we don't spam flags every tick
                player.removeMetadata("zac_spider", plugin);
            }
        } else {
            // Ascending but no wall adjacent = normal jump
            if (player.hasMetadata("zac_spider")) {
                player.removeMetadata("zac_spider", plugin);
            }
        }
    }

    private boolean isOnGround(Location loc) {
        return loc.clone().subtract(0, 0.1, 0).getBlock().getType().isSolid();
    }
}
