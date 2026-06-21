package dk.zai.anticheat.checks.exploits;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.entity.Player;

/**
 * Timer detection.
 * Flags if the player sends more PlayerMovePackets per second than the
 * server's tick rate allows (max-moves-per-second). A Timer hack sends
 * extra packets so the player moves faster than the server tick rate.
 */
public class TimerCheck extends Check {

    private final int maxMovesPerSecond;
    private final long sampleWindowMs;

    public TimerCheck(AntiCheatPlugin plugin) {
        super(plugin, "Timer", "exploits.timer");
        this.maxMovesPerSecond = cfg().getInt("max-moves-per-second", 22);
        this.sampleWindowMs = cfg().getLong("sample-window-ms", 1000);
    }

    public void onMovePacket(Player player) {
        if (!enabled || bypass(player)) return;
        var pd = plugin.getDataManager().get(player.getUniqueId());
        pd.recordMove();

        long[] samples = pd.getMoveSamples();
        if (samples.length < 5) return;
        long now = System.currentTimeMillis();
        int count = 0;
        for (long t : samples) if (now - t < sampleWindowMs) count++;
        if (count > maxMovesPerSecond) {
            flag(player, "moves=" + count + "/" + (sampleWindowMs / 1000) + "s");
        }
    }
}
