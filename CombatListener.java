package dk.zai.anticheat.checks.player;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * FastBreak detection.
 * Flags if blocks are broken less than min-interval-ms apart, which is
 * faster than the survival game mechanic allows for instant-break tools.
 */
public class FastBreakCheck extends Check {

    private final long minIntervalMs;

    public FastBreakCheck(AntiCheatPlugin plugin) {
        super(plugin, "FastBreak", "player.fastbreak");
        this.minIntervalMs = cfg().getLong("min-interval-ms", 50);
    }

    public void onBreak(Player player, BlockBreakEvent event) {
        if (!enabled || bypass(player)) return;
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        var pd = plugin.getDataManager().get(player.getUniqueId());
        long now = System.currentTimeMillis();
        long last = pd.getLastBreakTime();
        if (last > 0 && (now - last) < minIntervalMs) {
            flag(player, "break dt=" + (now - last) + "ms");
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
        }
        pd.setLastBreakTime(now);
    }
}
