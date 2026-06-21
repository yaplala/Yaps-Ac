package dk.zai.anticheat.checks.player;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * FastPlace + Scaffolding detection.
 * Flags if:
 *   - Block placements happen less than min-interval-ms apart (FastPlace).
 *   - More than scaffold-max blocks are placed within scaffold-window-ms
 *     while looking downward and moving forward (auto-bridge / scaffolding).
 */
public class FastPlaceCheck extends Check {

    private final long minIntervalMs;
    private final long scaffoldWindowMs;
    private final int scaffoldMax;

    public FastPlaceCheck(AntiCheatPlugin plugin) {
        super(plugin, "FastPlace", "player.fastplace");
        this.minIntervalMs = cfg().getLong("min-interval-ms", 80);
        this.scaffoldWindowMs = cfg().getLong("scaffold-window-ms", 500);
        this.scaffoldMax = cfg().getInt("scaffold-max", 3);
    }

    public void onPlace(Player player, BlockPlaceEvent event) {
        if (!enabled || bypass(player)) return;
        var pd = plugin.getDataManager().get(player.getUniqueId());
        long now = System.currentTimeMillis();

        // 1) FastPlace: too-rapid placements
        long last = pd.getLastPlaceTime();
        if (last > 0 && (now - last) < minIntervalMs) {
            flag(player, "place dt=" + (now - last) + "ms");
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
            pd.setLastPlaceTime(now);
            return;
        }
        pd.setLastPlaceTime(now);

        // 2) Scaffolding: many blocks placed below the player's feet while moving
        Location placed = event.getBlock().getLocation();
        Location feet = player.getLocation();
        boolean belowFeet = placed.getY() < feet.getY() - 0.5;
        boolean moving = player.getVelocity().lengthSquared() > 0.01;
        boolean lookingDown = player.getLocation().getDirection().getY() < -0.4;

        if (belowFeet && moving && lookingDown) {
            if (now - pd.getScaffoldWindowStart() > scaffoldWindowMs) {
                pd.setScaffoldWindowStart(now);
                pd.setScaffoldCount(0);
            }
            pd.incrementScaffold();
            if (pd.getScaffoldCount() > scaffoldMax) {
                flag(player, "scaffold=" + pd.getScaffoldCount() + " in " + scaffoldWindowMs + "ms");
                if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
                pd.setScaffoldCount(0);
            }
        }
    }
}
