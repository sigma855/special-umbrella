package com.donutmarket;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public class MenuListener implements Listener {
    private final Plugin plugin;

    public MenuListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        e.setCancelled(true); // nobody can take, put, shift-click or swap items
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        Consumer<ClickType> action = menu.action(e.getSlot());
        if (action != null) {
            ClickType type = e.getClick();
            // run next tick so opening another menu from inside the click event is safe
            Bukkit.getScheduler().runTask(plugin, () -> action.accept(type));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Market.I.deliverPending(e.getPlayer());
    }
}
