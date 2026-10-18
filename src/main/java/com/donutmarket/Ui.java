package com.donutmarket;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class Ui {
    private Ui() {}

    public static Component t(String s, NamedTextColor c) {
        return Component.text(s, c).decoration(TextDecoration.ITALIC, false);
    }

    public static Component kv(String key, String val, NamedTextColor valColor) {
        return Component.text()
                .append(Component.text(key, NamedTextColor.GRAY))
                .append(Component.text(val, valColor))
                .build()
                .decoration(TextDecoration.ITALIC, false);
    }

    public static ItemStack named(Material m, String name, NamedTextColor color, Component... lore) {
        ItemStack s = new ItemStack(m);
        s.editMeta(meta -> {
            meta.displayName(t(name, color));
            if (lore.length > 0) meta.lore(List.of(lore));
        });
        return s;
    }

    public static ItemStack pane() {
        return named(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.WHITE);
    }

    /** Copy of base with extra lines appended to its existing lore. */
    public static ItemStack withLore(ItemStack base, List<Component> extra) {
        ItemStack s = base.clone();
        s.editMeta(meta -> {
            List<Component> lines = new ArrayList<>();
            List<Component> existing = meta.lore();
            if (existing != null) lines.addAll(existing);
            lines.addAll(extra);
            meta.lore(lines);
        });
        return s;
    }
}
