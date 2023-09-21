package com.ashkiano.cowmilkingcooldown;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class CowMilkingCooldown extends JavaPlugin implements Listener {

    private final HashMap<UUID, HashMap<UUID, Long>> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 300000; // 5 minutes in milliseconds

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        Metrics metrics = new Metrics(this, 19845);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Cow)) {
            return;
        }

        Cow cow = (Cow) event.getRightClicked();
        Player player = event.getPlayer();

        if (player.getItemInHand().getType() != Material.BUCKET) {
            return;
        }

        UUID cowUUID = cow.getUniqueId();
        UUID playerUUID = player.getUniqueId();

        cooldowns.computeIfAbsent(cowUUID, k -> new HashMap<>());

        if (cooldowns.get(cowUUID).containsKey(playerUUID)) {
            long cooldownTimeLeft = (cooldowns.get(cowUUID).get(playerUUID) + COOLDOWN_TIME) - System.currentTimeMillis();
            if (cooldownTimeLeft > 0) {
                event.setCancelled(true);
                player.sendMessage("§cYou must wait " + (cooldownTimeLeft / 1000) + " seconds before milking this cow again!");
                return;
            }
        }

        cooldowns.get(cowUUID).put(playerUUID, System.currentTimeMillis());
    }
}