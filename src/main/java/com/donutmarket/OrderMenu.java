package com.donutmarket;

import com.donutmarket.Market.Order;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OrderMenu {
    private OrderMenu() {}

    private static final int PER_PAGE = 45;

    public static void open(Player player, int page, boolean mine) {
        Market mk = Market.I;
        List<Order> list = new ArrayList<>(mk.orders);
        if (mine) list.removeIf(o -> !o.buyer.equals(player.getUniqueId()));
        list.sort(Comparator.comparingLong((Order o) -> o.priceEach).reversed());
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        int pg = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu(mine ? "Your Orders" : "Orders");

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = pg * PER_PAGE + i;
            if (idx >= list.size()) break;
            Order o = list.get(idx);
            int remaining = o.wanted - o.filled;
            ItemStack icon = new ItemStack(o.item, Math.max(1, Math.min(remaining, o.item.getMaxStackSize())));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Ui.kv("Wants: ", remaining + " more (" + o.filled + "/" + o.wanted + ")", NamedTextColor.WHITE));
            lore.add(Ui.kv("Pays: ", Money.fmt(o.priceEach) + " each", NamedTextColor.GREEN));
            lore.add(Ui.kv("Payout for all: ", Money.fmt(remaining * o.priceEach), NamedTextColor.DARK_GREEN));
            lore.add(Ui.kv("Buyer: ", o.buyerName, NamedTextColor.YELLOW));
            lore.add(Component.empty());
            lore.add(Ui.t(mine ? "Click to cancel & get a refund" : "Click to deliver from your inventory", NamedTextColor.GOLD));
            if (!mine) lore.add(Ui.t("(plain items only - no enchants/names/damage)", NamedTextColor.DARK_GRAY));

            menu.set(i, Ui.withLore(icon, lore), c -> {
                if (mine) mk.cancelOrder(player, o.id);
                else mk.fillOrder(player, o.id);
                open(player, pg, mine);
            });
        }

        menu.fillRow(5);
        if (pg > 0) {
            menu.set(45, Ui.named(Material.ARROW, "Previous Page", NamedTextColor.YELLOW,
                    Ui.t("Page " + (pg + 1) + "/" + pages, NamedTextColor.GRAY)), c -> open(player, pg - 1, mine));
        }
        menu.set(47, Ui.named(Material.WRITABLE_BOOK, "Create Order", NamedTextColor.AQUA,
                Ui.t("/order create <item> <amount> <price each>", NamedTextColor.GRAY)), c -> {
            player.closeInventory();
            player.sendMessage(Component.text("Usage: /order create <item> <amount> <price each>  (e.g. /order create diamond 64 1.5k)",
                    NamedTextColor.YELLOW));
        });
        menu.set(49, mine
                        ? Ui.named(Material.ENDER_CHEST, "All Orders", NamedTextColor.LIGHT_PURPLE)
                        : Ui.named(Material.CHEST, "Your Orders", NamedTextColor.GOLD,
                                Ui.t("View or cancel your orders", NamedTextColor.GRAY)),
                c -> open(player, 0, !mine));
        menu.set(51, Ui.named(Material.EMERALD, "Balance", NamedTextColor.GREEN,
                Ui.t(Money.fmt(mk.balance(player.getUniqueId())), NamedTextColor.YELLOW)), null);
        if (pg < pages - 1) {
            menu.set(53, Ui.named(Material.ARROW, "Next Page", NamedTextColor.YELLOW,
                    Ui.t("Page " + (pg + 1) + "/" + pages, NamedTextColor.GRAY)), c -> open(player, pg + 1, mine));
        }
        menu.open(player);
    }
}
