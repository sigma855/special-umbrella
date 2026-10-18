package com.donutmarket;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** A 6-row chest menu. All clicks are cancelled by MenuListener; only the registered buttons do anything. */
public class Menu implements InventoryHolder {
    private final Inventory inv;
    private final Map<Integer, Consumer<ClickType>> actions = new HashMap<>();

    public Menu(String title) {
        this.inv = Bukkit.createInventory(this, 54, Component.text(title));
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void set(int slot, ItemStack item, Consumer<ClickType> action) {
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action); else actions.remove(slot);
    }

    public void fillRow(int row) {
        for (int i = row * 9; i < row * 9 + 9; i++) set(i, Ui.pane(), null);
    }

    public void fillAll() {
        for (int i = 0; i < 54; i++) set(i, Ui.pane(), null);
    }

    public Consumer<ClickType> action(int slot) {
        return actions.get(slot);
    }

    public void open(Player p) {
        p.openInventory(inv);
    }

    public static void confirm(Player p, String title, ItemStack display, Runnable yes, Runnable no) {
        Menu m = new Menu(title);
        m.fillAll();
        m.set(13, display, null);
        m.set(29, Ui.named(Material.LIME_STAINED_GLASS_PANE, "CONFIRM", NamedTextColor.GREEN), c -> yes.run());
        m.set(33, Ui.named(Material.RED_STAINED_GLASS_PANE, "CANCEL", NamedTextColor.RED), c -> no.run());
        m.open(p);
    }
}
