package dk.zai.anticheat.checks.player;

import dk.zai.anticheat.AntiCheatPlugin;
import dk.zai.anticheat.checks.Check;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * AutoEat detection.
 * Flags if the player completes eating in less than min-eat-ms (vanilla
 * is ~1600ms / 32 ticks). AutoEat modules complete consumption instantly.
 */
public class AutoEatCheck extends Check {

    private final long minEatMs;

    public AutoEatCheck(AntiCheatPlugin plugin) {
        super(plugin, "AutoEat", "player.autoeat");
        this.minEatMs = cfg().getLong("min-eat-ms", 1500);
    }

    public void onStartEating(Player player) {
        if (!enabled || bypass(player)) return;
        plugin.getDataManager().get(player.getUniqueId()).setEatStart(System.currentTimeMillis());
    }

    public void onConsume(Player player, PlayerItemConsumeEvent event) {
        if (!enabled || bypass(player)) return;
        var pd = plugin.getDataManager().get(player.getUniqueId());
        long start = pd.getEatStart();
        if (start <= 0) return;
        long duration = System.currentTimeMillis() - start;
        if (duration < minEatMs) {
            flag(player, "ate in " + duration + "ms (min " + minEatMs + "ms)");
            if (plugin.getConfig().getBoolean("settings.cancel-on-flag", true)) event.setCancelled(true);
        }
        pd.setEatStart(0L);
    }
}
