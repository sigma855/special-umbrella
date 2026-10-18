package com.donutmarket;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RankListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Ranks.I.apply(e.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Ranks.Rank r = Ranks.I.rank(e.getPlayer().getUniqueId());
        e.renderer(ChatRenderer.viewerUnaware((source, name, message) -> Component.text()
                .append(r.prefix())
                .append(Component.text(source.getName(), r.color))
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(message.color(NamedTextColor.WHITE))
                .build()));
    }
}
