package com.donutmarket;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class DonutMarketPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        Market.I = new Market(this);
        Market.I.load();
        Ranks.I = new Ranks(this);

        Commands cmds = new Commands();
        for (String name : new String[]{"ah", "order", "bal", "pay", "eco"}) {
            PluginCommand pc = getCommand(name);
            if (pc != null) {
                pc.setExecutor(cmds);
                pc.setTabCompleter(cmds);
            }
        }
        PluginCommand rank = getCommand("rank");
        if (rank != null) {
            RankCommand rc = new RankCommand();
            rank.setExecutor(rc);
            rank.setTabCompleter(rc);
        }

        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new RankListener(), this);
        Bukkit.getOnlinePlayers().forEach(Ranks.I::apply);

        // expire old listings every 5s, autosave every 5min
        Bukkit.getScheduler().runTaskTimer(this, () -> Market.I.expireListings(), 100L, 100L);
        Bukkit.getScheduler().runTaskTimer(this, () -> Market.I.saveIfDirty(), 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (Market.I != null) Market.I.save();
    }
}
