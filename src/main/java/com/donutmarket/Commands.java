package com.donutmarket;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class Commands implements CommandExecutor, TabCompleter {

    private static final List<String> ITEM_NAMES = new ArrayList<>();
    static {
        for (Material m : Material.values()) {
            if (m.isItem() && !m.isAir() && !m.name().startsWith("LEGACY_")) ITEM_NAMES.add(m.getKey().getKey());
        }
        Collections.sort(ITEM_NAMES);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Market mk = Market.I;
        switch (cmd.getName().toLowerCase(Locale.ROOT)) {
            case "ah" -> {
                if (!(sender instanceof Player p)) return playersOnly(sender);
                if (args.length == 0) {
                    AhMenu.open(p, 0, false, AhMenu.Sort.NEWEST);
                } else if (args[0].equalsIgnoreCase("sell") && args.length == 2) {
                    long price = money(sender, args[1]);
                    if (price < 0) return true;
                    mk.sellHeld(p, price);
                } else {
                    sender.sendMessage(Market.err("Usage: /ah  or  /ah sell <price>"));
                }
            }
            case "order" -> {
                if (!(sender instanceof Player p)) return playersOnly(sender);
                if (args.length == 0) {
                    OrderMenu.open(p, 0, false);
                } else if (args[0].equalsIgnoreCase("create") && args.length == 4) {
                    Material m = Material.matchMaterial(args[1]);
                    if (m == null || !m.isItem() || m.isAir() || m.name().startsWith("LEGACY_")) {
                        sender.sendMessage(Market.err("Unknown item: " + args[1]));
                        return true;
                    }
                    int amount;
                    try { amount = Integer.parseInt(args[2]); }
                    catch (NumberFormatException e) { amount = -1; }
                    if (amount < 1 || amount > 100000) {
                        sender.sendMessage(Market.err("Amount must be between 1 and 100000."));
                        return true;
                    }
                    long price = money(sender, args[3]);
                    if (price < 0) return true;
                    mk.createOrder(p, m, amount, price);
                } else {
                    sender.sendMessage(Market.err("Usage: /order  or  /order create <item> <amount> <price each>"));
                }
            }
            case "bal" -> {
                if (!(sender instanceof Player p)) return playersOnly(sender);
                p.sendMessage(Market.ok("Balance: " + Money.fmt(mk.balance(p.getUniqueId()))));
            }
            case "pay" -> {
                if (!(sender instanceof Player p)) return playersOnly(sender);
                if (args.length != 2) { sender.sendMessage(Market.err("Usage: /pay <player> <amount>")); return true; }
                Player to = Bukkit.getPlayerExact(args[0]);
                if (to == null) { sender.sendMessage(Market.err("That player isn't online.")); return true; }
                if (to.getUniqueId().equals(p.getUniqueId())) { sender.sendMessage(Market.err("You can't pay yourself.")); return true; }
                long amt = money(sender, args[1]);
                if (amt < 0) return true;
                if (!mk.take(p.getUniqueId(), amt)) {
                    sender.sendMessage(Market.err("You don't have " + Money.fmt(amt) + "."));
                    return true;
                }
                mk.add(to.getUniqueId(), amt);
                p.sendMessage(Market.ok("Paid " + to.getName() + " " + Money.fmt(amt) + "."));
                to.sendMessage(Market.ok(p.getName() + " paid you " + Money.fmt(amt) + "."));
            }
            case "eco" -> {
                if (!sender.hasPermission("donutmarket.admin")) { sender.sendMessage(Market.err("No permission.")); return true; }
                if (args.length != 3) { sender.sendMessage(Market.err("Usage: /eco <give|take|set> <player> <amount>")); return true; }
                Player t = Bukkit.getPlayerExact(args[1]);
                if (t == null) { sender.sendMessage(Market.err("That player isn't online.")); return true; }
                long amt = money(sender, args[2]);
                if (amt < 0) return true;
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "give" -> mk.add(t.getUniqueId(), amt);
                    case "take" -> mk.setBalance(t.getUniqueId(), mk.balance(t.getUniqueId()) - amt);
                    case "set" -> mk.setBalance(t.getUniqueId(), amt);
                    default -> { sender.sendMessage(Market.err("Use give, take or set.")); return true; }
                }
                sender.sendMessage(Market.ok(t.getName() + " now has " + Money.fmt(mk.balance(t.getUniqueId())) + "."));
            }
            default -> { return false; }
        }
        return true;
    }

    private static boolean playersOnly(CommandSender s) {
        s.sendMessage(Market.err("Only players can use this command."));
        return true;
    }

    /** Returns -1 (after telling the sender) if invalid. */
    private static long money(CommandSender s, String raw) {
        try {
            return Money.parse(raw);
        } catch (Exception e) {
            s.sendMessage(Market.err("Invalid amount. Use numbers like 500, 1.5k, 2m, 3b."));
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        switch (name) {
            case "ah" -> { if (args.length == 1) out.add("sell"); }
            case "order" -> {
                if (args.length == 1) out.add("create");
                else if (args.length == 2 && args[0].equalsIgnoreCase("create")) out.addAll(ITEM_NAMES);
            }
            case "pay" -> { if (args.length == 1) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName())); }
            case "eco" -> {
                if (args.length == 1) out.addAll(List.of("give", "take", "set"));
                else if (args.length == 2) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            }
            default -> { }
        }
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(last));
        return out.size() > 50 ? out.subList(0, 50) : out;
    }
}
