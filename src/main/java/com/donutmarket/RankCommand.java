package com.donutmarket;

import com.donutmarket.Ranks.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RankCommand implements CommandExecutor, TabCompleter {

    /** Console, ops (donutmarket.admin) and players with the Owner rank may change ranks. */
    private static boolean canManage(CommandSender s) {
        if (!(s instanceof Player p)) return true;
        return s.hasPermission("donutmarket.admin") || Ranks.I.rank(p.getUniqueId()) == Rank.OWNER;
    }

    private static UUID resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        OfflinePlayer off = Bukkit.getOfflinePlayerIfCached(name);
        return off == null ? null : off.getUniqueId();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Ranks ranks = Ranks.I;

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(Component.text("Ranks:", NamedTextColor.YELLOW));
            for (Rank r : Rank.values()) sender.sendMessage(r.prefix());
            if (sender instanceof Player p) {
                Rank mine = ranks.rank(p.getUniqueId());
                sender.sendMessage(Component.text("Your rank: ", NamedTextColor.GRAY)
                        .append(Component.text(mine.display, mine.color)));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("get") && args.length == 2) {
            UUID id = resolve(args[1]);
            if (id == null) { sender.sendMessage(Market.err("Player not found.")); return true; }
            Rank r = ranks.rank(id);
            sender.sendMessage(Component.text(args[1] + " is ", NamedTextColor.GRAY)
                    .append(Component.text(r.display, r.color)));
            return true;
        }

        if (args[0].equalsIgnoreCase("set") && args.length == 3) {
            if (!canManage(sender)) { sender.sendMessage(Market.err("Only the Owner can change ranks.")); return true; }
            UUID id = resolve(args[1]);
            if (id == null) { sender.sendMessage(Market.err("Player not found (they must have joined before).")); return true; }
            Rank r = Rank.parse(args[2]);
            if (r == null) { sender.sendMessage(Market.err("Ranks: owner, admin, media, member")); return true; }

            ranks.set(id, r);
            Player online = Bukkit.getPlayer(id);
            if (online != null) {
                ranks.apply(online);
                online.sendMessage(Component.text("Your rank is now ", NamedTextColor.GRAY)
                        .append(Component.text(r.display, r.color)));
            }
            sender.sendMessage(Component.text("Set " + args[1] + " to ", NamedTextColor.GREEN)
                    .append(Component.text(r.display, r.color)));
            return true;
        }

        sender.sendMessage(Market.err("Usage: /rank list | /rank get <player> | /rank set <player> <rank>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> out = new ArrayList<>();
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) out.addAll(List.of("list", "get", "set"));
        else if (args.length == 2) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        else if (args.length == 3 && args[0].equalsIgnoreCase("set"))
            for (Rank r : Rank.values()) out.add(r.name().toLowerCase(Locale.ROOT));
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
