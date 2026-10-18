package com.donutmarket;

import com.donutmarket.Market.Listing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AhMenu {
    private AhMenu() {}

    private static final int PER_PAGE = 45;

    public enum Sort {
        NEWEST("Newest", Comparator.comparingLong((Listing l) -> l.expiresAt).reversed()),
        LOWEST("Lowest price", Comparator.comparingLong((Listing l) -> l.price)),
        HIGHEST("Highest price", Comparator.comparingLong((Listing l) -> l.price).reversed());

        final String label;
        final Comparator<Listing> cmp;
        Sort(String label, Comparator<Listing> cmp) { this.label = label; this.cmp = cmp; }
        Sort next() { return values()[(ordinal() + 1) % values().length]; }
    }

    public static void open(Player player, int page, boolean mine, Sort sort) {
        Market mk = Market.I;
        List<Listing> list = new ArrayList<>(mk.listings);
        if (mine) list.removeIf(l -> !l.seller.equals(player.getUniqueId()));
        list.sort(sort.cmp);
        int pages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        int pg = Math.max(0, Math.min(page, pages - 1));
        long now = System.currentTimeMillis();

        Menu menu = new Menu(mine ? "Your Listings" : "Auction House");

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = pg * PER_PAGE + i;
            if (idx >= list.size()) break;
            Listing l = list.get(idx);

            List<Component> extra = new ArrayList<>();
            extra.add(Component.empty());
            extra.add(Ui.kv("Price: ", Money.fmt(l.price), NamedTextColor.GREEN));
            if (l.item.getAmount() > 1) {
                extra.add(Ui.kv("Each: ", Money.fmt(Math.max(1, l.price / l.item.getAmount())), NamedTextColor.DARK_GREEN));
            }
            extra.add(Ui.kv("Seller: ", l.sellerName, NamedTextColor.YELLOW));
            extra.add(Ui.kv("Expires in: ", Money.duration(l.expiresAt - now), NamedTextColor.AQUA));
            extra.add(Component.empty());
            extra.add(Ui.t(mine ? "Click to cancel & take back" : "Click to buy", NamedTextColor.GOLD));
            ItemStack shown = Ui.withLore(l.item, extra);

            menu.set(i, shown, c -> {
                if (mine) {
                    mk.cancelListing(player, l.id);
                    open(player, pg, true, sort);
                } else {
                    Menu.confirm(player, "Buy for " + Money.fmt(l.price) + "?", shown.clone(),
                            () -> { mk.buyListing(player, l.id); open(player, pg, false, sort); },
                            () -> open(player, pg, false, sort));
                }
            });
        }

        menu.fillRow(5);
        if (pg > 0) {
            menu.set(45, Ui.named(Material.ARROW, "Previous Page", NamedTextColor.YELLOW,
                    Ui.t("Page " + (pg + 1) + "/" + pages, NamedTextColor.GRAY)), c -> open(player, pg - 1, mine, sort));
        }
        menu.set(47, Ui.named(Material.HOPPER, "Sort: " + sort.label, NamedTextColor.AQUA,
                Ui.t("Click to change", NamedTextColor.GRAY)), c -> open(player, 0, mine, sort.next()));
        menu.set(49, mine
                        ? Ui.named(Material.ENDER_CHEST, "All Listings", NamedTextColor.LIGHT_PURPLE)
                        : Ui.named(Material.CHEST, "Your Listings", NamedTextColor.GOLD,
                                Ui.t("Cancel or view your items", NamedTextColor.GRAY)),
                c -> open(player, 0, !mine, sort));
        menu.set(51, Ui.named(Material.EMERALD, "Balance", NamedTextColor.GREEN,
                Ui.t(Money.fmt(mk.balance(player.getUniqueId())), NamedTextColor.YELLOW),
                Ui.t("Sell: hold item, /ah sell <price>", NamedTextColor.GRAY)), null);
        if (pg < pages - 1) {
            menu.set(53, Ui.named(Material.ARROW, "Next Page", NamedTextColor.YELLOW,
                    Ui.t("Page " + (pg + 1) + "/" + pages, NamedTextColor.GRAY)), c -> open(player, pg + 1, mine, sort));
        }
        menu.open(player);
    }
}
