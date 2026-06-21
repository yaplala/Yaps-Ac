package dk.zai.anticheat.checks.movement;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Jesus / WaterWalk detection.
 * Flags if the player stays on top of water (without ice/frost walker)
 * for more than min-ticks. Standing on water is impossible in vanilla
 * unless you're sneaking on top with frost walker.
 */
public class JesusCheck extends Check {

    private final int minTicks;

    public JesusCheck(AntiCheatPlugin plugin) {
        super(plugin, "Jesus", "movement.jesus");
        this.minTicks = cfg().getInt("min-ticks", 8);
    }

    public void onMove(Player player, PlayerMoveEvent event) {
        if (!enabled || bypass(player)) return;
        if (player.isFlying() || player.isGliding()) return;

        Location to = event.getTo();
        if (to == null) return;

        Location below = to.clone().subtract(0, 0.1, 0);
        Material belowType = below.getBlock().getType();
        Location feet = to.clone();
        Material feetType = feet.getBlock().getType();

        boolean waterBelow = belowType == Material.WATER;
        boolean feetAir = feetType.isAir();
        if (waterBelow && feetAir && player.getVelocity().getY() >= -0.05) {
            // Track a counter via player metadata
            int count = player.getMetadata("zac_jesus").stream()
                    .findFirst().map(v -> v.asInt()).orElse(0);
            count++;
            player.setMetadata("zac_jesus", new org.bukkit.metadata.FixedMetadataValue(plugin, count));
            if (count >= minTicks) {
                flag(player, "standing on water " + count + " ticks");
                if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
                player.removeMetadata("zac_jesus", plugin);
            }
        } else {
            if (player.hasMetadata("zac_jesus")) player.removeMetadata("zac_jesus", plugin);
        }
    }
}
