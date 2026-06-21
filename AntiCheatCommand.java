package dk.zai.anticheat.checks.combat;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * AutoClicker detection.
 * Records each left-click timestamp into a ring buffer, then:
 *   - Computes CPS over the last second.
 *   - Computes the standard deviation of click intervals (jitter).
 *   - Flags if CPS exceeds max-cps, or if jitter is below min-jitter-ms
 *     (perfect click rhythm is impossible for a human).
 */
public class AutoClickerCheck extends Check {

    private final int maxCps;
    private final long minJitterMs;
    private final int sampleSize;

    public AutoClickerCheck(AntiCheatPlugin plugin) {
        super(plugin, "AutoClicker", "combat.autoclicker");
        this.maxCps = cfg().getInt("max-cps", 14);
        this.minJitterMs = cfg().getLong("min-jitter-ms", 15);
        this.sampleSize = cfg().getInt("sample-size", 20);
    }

    public void onClick(Player player, PlayerInteractEvent event) {
        if (!enabled || bypass(player)) return;
        var pd = plugin.getDataManager().get(player.getUniqueId());
        pd.recordClick();

        long[] samples = pd.getClickSamples();
        if (samples.length < sampleSize) return;

        // CPS over last 1000ms
        long now = System.currentTimeMillis();
        int cps = 0;
        for (long t : samples) if (now - t < 1000L) cps++;
        if (cps > maxCps) {
            flag(player, "cps=" + cps + " > " + maxCps);
            return;
        }

        // Jitter: standard deviation of intervals
        double mean = 0;
        long[] intervals = new long[samples.length - 1];
        for (int i = 1; i < samples.length; i++) {
            intervals[i - 1] = samples[i] - samples[i - 1];
            mean += intervals[i - 1];
        }
        mean /= intervals.length;
        double variance = 0;
        for (long d : intervals) variance += (d - mean) * (d - mean);
        double std = Math.sqrt(variance / intervals.length);
        if (std < minJitterMs) {
            flag(player, "jitter=" + String.format("%.1f", std) + "ms < " + minJitterMs);
        }
    }
}
