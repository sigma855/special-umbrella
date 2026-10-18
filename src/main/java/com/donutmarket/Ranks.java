package com.donutmarket;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Rank storage + applying colors to chat, tab list and nametags. */
public final class Ranks {
    public static Ranks I;

    public enum Rank {
        // order = sort order in the tab list (top to bottom)
        OWNER("Owner", NamedTextColor.AQUA),
        ADMIN("Admin", NamedTextColor.RED),
        MEDIA("Media", NamedTextColor.LIGHT_PURPLE), // Minecraft's "pink"
        MEMBER("Member", NamedTextColor.GRAY);

        public final String display;
        public final NamedTextColor color;

        Rank(String display, NamedTextColor color) {
            this.display = display;
            this.color = color;
        }

        public Component prefix() {
            return Component.text("[" + display + "] ", color).decoration(TextDecoration.BOLD, true);
        }

        public static Rank parse(String s) {
            for (Rank r : values()) if (r.name().equalsIgnoreCase(s)) return r;
            return null;
        }
    }

    private final File file;
    private final Map<UUID, Rank> ranks = new ConcurrentHashMap<>();

    public Ranks(JavaPlugin plugin) {
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "ranks.yml");
        if (file.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
            for (String k : y.getKeys(false)) {
                Rank r = Rank.parse(y.getString(k, ""));
                if (r != null) ranks.put(UUID.fromString(k), r);
            }
        }
    }

    public Rank rank(UUID id) {
        return ranks.getOrDefault(id, Rank.MEMBER);
    }

    public void set(UUID id, Rank r) {
        if (r == Rank.MEMBER) ranks.remove(id); else ranks.put(id, r);
        save();
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        ranks.forEach((id, r) -> y.set(id.toString(), r.name()));
        try { y.save(file); } catch (IOException ignored) { }
    }

    /** Colors the player's tab-list entry and the nametag above their head. */
    public void apply(Player p) {
        Rank r = rank(p.getUniqueId());
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Rank each : Rank.values()) {
            String teamName = "dm_" + each.ordinal() + "_" + each.name().toLowerCase(Locale.ROOT);
            Team t = sb.getTeam(teamName);
            if (t == null) t = sb.registerNewTeam(teamName);
            t.color(each.color);
            t.prefix(each.prefix());
            if (each == r) t.addEntry(p.getName());
            else t.removeEntry(p.getName());
        }

        p.playerListName(Component.text()
                .append(r.prefix())
                .append(Component.text(p.getName(), r.color))
                .build());
    }
}
