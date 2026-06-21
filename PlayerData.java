package dk.zai.anticheat.checks;

import dk.zai.anticheat.AntiCheatPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Abstract base for all anti-cheat checks. Subclasses read their own
 * sub-section from config.yml and call {@link #flag(Player, String)} when
 * a violation is detected. The base class handles bypass, ping filter
 * and alert dispatch.
 */
public abstract class Check {

    protected final AntiCheatPlugin plugin;
    protected final String name;
    protected final String configPath;
    protected boolean enabled = true;

    protected Check(AntiCheatPlugin plugin, String name, String configPath) {
        this.plugin = plugin;
        this.name = name;
        this.configPath = configPath;
        reload();
    }

    public String getName() { return name; }
    public String getConfigPath() { return configPath; }
    public boolean isEnabled() { return enabled; }

    /** Reload this check's settings from the current in-memory config. */
    public void reload() {
        enabled = plugin.getConfig().getBoolean(configPath + ".enabled", true);
    }

    /** Toggle this check's enabled state both in-memory and in config.yml on disk. */
    public void toggle() {
        enabled = !enabled;
        plugin.getConfig().set(configPath + ".enabled", enabled);
        plugin.saveConfig();
    }

    protected ConfigurationSection cfg() {
        return plugin.getConfig().getConfigurationSection(configPath);
    }

    protected boolean bypass(Player p) {
        String perm = plugin.getConfig().getString("settings.bypass-permission", "yaps.bypass");
        if (p.hasPermission(perm)) return true;
        int ping = p.getPing();
        int maxPing = plugin.getConfig().getInt("settings.max-ping", 250);
        return ping > maxPing;
    }

    protected void flag(Player p, String detail) {
        plugin.getAlertManager().flag(p, name, detail);
    }
}
