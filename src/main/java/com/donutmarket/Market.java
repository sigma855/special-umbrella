package com.donutmarket;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** All economy / auction house / order logic + saving. */
public final class Market {
    public static Market I;

    // ---- settings you can edit ----
    public static final long START_BALANCE = 10_000;
    public static final long LISTING_DURATION_MS = 48L * 3600 * 1000;
    public static final int MAX_LISTINGS = 10;
    public static final int MAX_ORDERS = 5;
    // --------------------------------

    public static class Listing {
        public UUID id = UUID.randomUUID();
        public UUID seller;
        public String sellerName;
        public ItemStack item;
        public long price;       // total price for the whole stack
        public long expiresAt;   // epoch millis
    }

    public static class Order {
        public UUID id = UUID.randomUUID();
        public UUID buyer;
        public String buyerName;
        public Material item;
        public int wanted;
        public int filled;
        public long priceEach;
    }

    private final JavaPlugin plugin;
    private final File file;
    private boolean dirty;

    public final Map<UUID, Long> balances = new HashMap<>();
    public final List<Listing> listings = new ArrayList<>();
    public final List<Order> orders = new ArrayList<>();
    /** Items waiting for offline players. */
    public final Map<UUID, List<ItemStack>> pending = new HashMap<>();

    public Market(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ================= persistence =================
    private static String enc(ItemStack s) {
        return Base64.getEncoder().encodeToString(s.serializeAsBytes());
    }

    private static ItemStack dec(String b64) {
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(b64));
        } catch (Exception e) {
            return null;
        }
    }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection b = y.getConfigurationSection("balances");
        if (b != null) for (String k : b.getKeys(false)) balances.put(UUID.fromString(k), b.getLong(k));

        ConfigurationSection ls = y.getConfigurationSection("listings");
        if (ls != null) for (String k : ls.getKeys(false)) {
            ConfigurationSection s = ls.getConfigurationSection(k);
            if (s == null) continue;
            ItemStack item = dec(s.getString("item", ""));
            if (item == null) { plugin.getLogger().warning("Skipping unreadable listing " + k); continue; }
            Listing l = new Listing();
            l.id = UUID.fromString(k);
            l.seller = UUID.fromString(s.getString("seller"));
            l.sellerName = s.getString("sellerName", "?");
            l.item = item;
            l.price = s.getLong("price");
            l.expiresAt = s.getLong("expiresAt");
            listings.add(l);
        }

        ConfigurationSection os = y.getConfigurationSection("orders");
        if (os != null) for (String k : os.getKeys(false)) {
            ConfigurationSection s = os.getConfigurationSection(k);
            if (s == null) continue;
            Material m = Material.matchMaterial(s.getString("item", ""));
            if (m == null) continue;
            Order o = new Order();
            o.id = UUID.fromString(k);
            o.buyer = UUID.fromString(s.getString("buyer"));
            o.buyerName = s.getString("buyerName", "?");
            o.item = m;
            o.wanted = s.getInt("wanted");
            o.filled = s.getInt("filled");
            o.priceEach = s.getLong("priceEach");
            orders.add(o);
        }

        ConfigurationSection ps = y.getConfigurationSection("pending");
        if (ps != null) for (String k : ps.getKeys(false)) {
            List<ItemStack> items = new ArrayList<>();
            for (String str : ps.getStringList(k)) {
                ItemStack it = dec(str);
                if (it != null) items.add(it);
            }
            if (!items.isEmpty()) pending.put(UUID.fromString(k), items);
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        balances.forEach((id, v) -> y.set("balances." + id, v));
        for (Listing l : listings) {
            String p = "listings." + l.id + ".";
            y.set(p + "seller", l.seller.toString());
            y.set(p + "sellerName", l.sellerName);
            y.set(p + "item", enc(l.item));
            y.set(p + "price", l.price);
            y.set(p + "expiresAt", l.expiresAt);
        }
        for (Order o : orders) {
            String p = "orders." + o.id + ".";
            y.set(p + "buyer", o.buyer.toString());
            y.set(p + "buyerName", o.buyerName);
            y.set(p + "item", o.item.name());
            y.set(p + "wanted", o.wanted);
            y.set(p + "filled", o.filled);
            y.set(p + "priceEach", o.priceEach);
        }
        pending.forEach((id, items) -> y.set("pending." + id, items.stream().map(Market::enc).toList()));
        try {
            y.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    public void saveIfDirty() {
        if (dirty) save();
    }

    // ================= money =================
    public long balance(UUID id) {
        return balances.computeIfAbsent(id, k -> START_BALANCE);
    }

    public void add(UUID id, long amt) {
        balances.put(id, balance(id) + amt);
        dirty = true;
    }

    public boolean take(UUID id, long amt) {
        long b = balance(id);
        if (b < amt) return false;
        balances.put(id, b - amt);
        dirty = true;
        return true;
    }

    public void setBalance(UUID id, long amt) {
        balances.put(id, Math.max(0, amt));
        dirty = true;
    }

    // ================= helpers =================
    public static Component ok(String s) { return Component.text(s, NamedTextColor.GREEN); }
    public static Component err(String s) { return Component.text(s, NamedTextColor.RED); }

    public static String pretty(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String part : m.name().toLowerCase(Locale.ROOT).split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    public void tell(UUID id, Component msg) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) p.sendMessage(msg);
    }

    private static void giveNow(Player p, ItemStack stack) {
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(stack);
        for (ItemStack rest : leftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), rest);
    }

    public void giveStack(UUID id, ItemStack stack) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) giveNow(p, stack);
        else pending.computeIfAbsent(id, k -> new ArrayList<>()).add(stack);
        dirty = true;
    }

    public void giveItems(UUID id, Material m, int amount) {
        while (amount > 0) {
            int n = Math.min(amount, m.getMaxStackSize());
            giveStack(id, new ItemStack(m, n));
            amount -= n;
        }
    }

    public void deliverPending(Player p) {
        List<ItemStack> list = pending.remove(p.getUniqueId());
        if (list == null || list.isEmpty()) return;
        for (ItemStack s : list) giveNow(p, s);
        dirty = true;
        p.sendMessage(ok("You received " + list.size() + " item stack(s) that were waiting for you."));
    }

    // ================= auction house =================
    public void sellHeld(Player p, long price) {
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held.getType().isAir()) { p.sendMessage(err("Hold the item you want to sell.")); return; }
        long mine = listings.stream().filter(l -> l.seller.equals(p.getUniqueId())).count();
        if (mine >= MAX_LISTINGS) { p.sendMessage(err("You can only have " + MAX_LISTINGS + " listings at once.")); return; }

        ItemStack copy = held.clone();
        p.getInventory().setItemInMainHand(null);

        Listing l = new Listing();
        l.seller = p.getUniqueId();
        l.sellerName = p.getName();
        l.item = copy;
        l.price = price;
        l.expiresAt = System.currentTimeMillis() + LISTING_DURATION_MS;
        listings.add(l);
        dirty = true;
        p.sendMessage(ok("Listed " + copy.getAmount() + "x " + pretty(copy.getType()) + " for " + Money.fmt(price)
                + " on the Auction House."));
    }

    private Listing findListing(UUID id) {
        return listings.stream().filter(l -> l.id.equals(id)).findFirst().orElse(null);
    }

    public void buyListing(Player p, UUID id) {
        Listing l = findListing(id);
        if (l == null) { p.sendMessage(err("That listing is no longer available.")); return; }
        if (l.seller.equals(p.getUniqueId())) { p.sendMessage(err("You can't buy your own listing.")); return; }
        if (!take(p.getUniqueId(), l.price)) {
            p.sendMessage(err("You need " + Money.fmt(l.price - balance(p.getUniqueId())) + " more."));
            return;
        }
        listings.remove(l);
        add(l.seller, l.price);
        giveStack(p.getUniqueId(), l.item);
        String name = l.item.getAmount() + "x " + pretty(l.item.getType());
        p.sendMessage(ok("Bought " + name + " for " + Money.fmt(l.price) + "."));
        tell(l.seller, ok(p.getName() + " bought your " + name + " for " + Money.fmt(l.price) + "!"));
    }

    public void cancelListing(Player p, UUID id) {
        Listing l = findListing(id);
        if (l == null) return;
        if (!l.seller.equals(p.getUniqueId())) { p.sendMessage(err("That's not your listing.")); return; }
        listings.remove(l);
        giveStack(p.getUniqueId(), l.item);
        p.sendMessage(ok("Listing cancelled. Item returned."));
    }

    public void expireListings() {
        long now = System.currentTimeMillis();
        Iterator<Listing> it = listings.iterator();
        while (it.hasNext()) {
            Listing l = it.next();
            if (l.expiresAt <= now) {
                it.remove();
                giveStack(l.seller, l.item);
                dirty = true;
                tell(l.seller, Component.text("Your auction listing expired and the item was returned.", NamedTextColor.YELLOW));
            }
        }
    }

    // ================= orders =================
    private Order findOrder(UUID id) {
        return orders.stream().filter(o -> o.id.equals(id)).findFirst().orElse(null);
    }

    public void createOrder(Player p, Material item, int amount, long priceEach) {
        long mine = orders.stream().filter(o -> o.buyer.equals(p.getUniqueId())).count();
        if (mine >= MAX_ORDERS) { p.sendMessage(err("You can only have " + MAX_ORDERS + " active orders.")); return; }
        long total;
        try { total = Math.multiplyExact(priceEach, (long) amount); }
        catch (ArithmeticException e) { p.sendMessage(err("That total is too large.")); return; }
        if (!take(p.getUniqueId(), total)) {
            p.sendMessage(err("You need " + Money.fmt(total) + " to place this order (money is held until filled or cancelled)."));
            return;
        }
        Order o = new Order();
        o.buyer = p.getUniqueId();
        o.buyerName = p.getName();
        o.item = item;
        o.wanted = amount;
        o.priceEach = priceEach;
        orders.add(o);
        dirty = true;
        p.sendMessage(ok("Order placed: " + amount + "x " + pretty(item) + " at " + Money.fmt(priceEach)
                + " each. " + Money.fmt(total) + " held in escrow."));
    }

    public void fillOrder(Player p, UUID id) {
        Order o = findOrder(id);
        if (o == null) { p.sendMessage(err("That order is no longer available.")); return; }
        if (o.buyer.equals(p.getUniqueId())) { p.sendMessage(err("You can't fill your own order.")); return; }

        int need = o.wanted - o.filled;
        int taken = 0;
        ItemStack plain = new ItemStack(o.item);
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < 36 && taken < need; i++) {
            ItemStack s = inv.getItem(i);
            // only plain items count (isSimilar rejects enchanted / renamed / damaged / etc.)
            if (s != null && s.isSimilar(plain)) {
                int n = Math.min(s.getAmount(), need - taken);
                if (n >= s.getAmount()) inv.setItem(i, null);
                else { s.setAmount(s.getAmount() - n); inv.setItem(i, s); }
                taken += n;
            }
        }
        if (taken == 0) {
            p.sendMessage(err("You don't have any plain " + pretty(o.item) + " to deliver."));
            return;
        }
        long pay = taken * o.priceEach;
        add(p.getUniqueId(), pay);
        o.filled += taken;
        giveItems(o.buyer, o.item, taken);
        String name = taken + "x " + pretty(o.item);
        p.sendMessage(ok("Delivered " + name + " and earned " + Money.fmt(pay) + "."));
        tell(o.buyer, ok(p.getName() + " delivered " + name + " to your order!"));
        if (o.filled >= o.wanted) {
            orders.remove(o);
            tell(o.buyer, ok("Your order for " + pretty(o.item) + " is complete."));
        }
        dirty = true;
    }

    public void cancelOrder(Player p, UUID id) {
        Order o = findOrder(id);
        if (o == null) return;
        if (!o.buyer.equals(p.getUniqueId())) { p.sendMessage(err("That's not your order.")); return; }
        long refund = (long) (o.wanted - o.filled) * o.priceEach;
        orders.remove(o);
        add(p.getUniqueId(), refund);
        p.sendMessage(ok("Order cancelled. Refunded " + Money.fmt(refund) + "."));
    }
}
