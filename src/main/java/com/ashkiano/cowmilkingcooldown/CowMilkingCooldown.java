package com.ashkiano.cowmilkingcooldown;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

public class CowMilkingCooldown extends JavaPlugin implements Listener {

    private final HashMap<UUID, HashMap<UUID, Long>> cooldowns = new HashMap<>();
    private long cooldown = 300; // 5 minutes in seconds
    private boolean applyToMooshrooms = true;
    private boolean applyToCows = true;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        Metrics metrics = new Metrics(this, 19845);

        loadConfigValues();

        this.getLogger().info("Thank you for using the CowMilkingCooldown plugin! If you enjoy using this plugin, please consider making a donation to support the development. You can donate at: https://donate.ashkiano.com");

        checkForUpdates();
    }

    private void loadConfigValues() {
        this.saveDefaultConfig();

        applyDefaultValueIfNotFound("cooldown", 300);
        applyDefaultValueIfNotFound("mushroom-stew-from-mooshroom", false);
        applyDefaultValueIfNotFound("apply-to-cows", true);

        cooldown = this.getConfig().getLong("cooldown");
        applyToMooshrooms = this.getConfig().getBoolean("mushroom-stew-from-mooshroom");
        applyToCows = this.getConfig().getBoolean("apply-to-cows");
    }

    public void applyDefaultValueIfNotFound(String path, Object value) {
        if (!this.getConfig().contains(path)) {
            this.getConfig().set(path, value);
            this.saveConfig();
        }
    }

    public void reload() {
        super.reloadConfig();
        loadConfigValues();
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Cow)) {
            return;
        }

        Cow cow = (Cow) event.getRightClicked();
        Player player = event.getPlayer();

        ItemStack stack = event.getPlayer().getInventory().getItem(event.getHand());

        if (applyToCows)
            if (checkCow(cow, player, stack)) event.setCancelled(true);
        if (applyToMooshrooms)
            if (checkMooshroom(cow, player, stack)) event.setCancelled(true);
    }

    public boolean checkCow(Cow cow, Player player, ItemStack stack) {
        if (stack != null && stack.getType() != Material.BUCKET) {
            return false;
        }
        return (updateMilkingCooldown(player, cow, ChatColor.RED+"You must wait %s seconds before milking this cow again!"));
    }

    public boolean checkMooshroom(Cow cow, Player player, ItemStack stack) {
        if (stack != null && stack.getType() != Material.BOWL) {
            return false;
        }
        if (!(cow instanceof MushroomCow)) return false;
        return (updateMilkingCooldown(player, cow, ChatColor.RED+"You must wait %s seconds before milking this mooshroom again!"));
    }

    public boolean updateMilkingCooldown(Player player, Cow cow, String message) {
        UUID cowUUID = cow.getUniqueId();
        UUID playerUUID = player.getUniqueId();

        cooldowns.computeIfAbsent(cowUUID, k -> new HashMap<>());

        if (cooldowns.get(cowUUID).containsKey(playerUUID)) {
            long cooldownTimeLeft = (cooldowns.get(cowUUID).get(playerUUID) + cooldown*1000L) - System.currentTimeMillis();

            if (cooldownTimeLeft > 0) {
                player.sendMessage(String.format(message, (cooldownTimeLeft/1000L)));
                return true;
            }
        }
        cooldowns.get(cowUUID).put(playerUUID, System.currentTimeMillis());
        return false;
    }

    private void checkForUpdates() {
        try {
            String pluginName = this.getDescription().getName();
            URL url = new URL("https://www.ashkiano.com/version_check.php?plugin=" + pluginName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String jsonResponse = response.toString();
                JSONObject jsonObject = new JSONObject(jsonResponse);
                if (jsonObject.has("error")) {
                    this.getLogger().warning("Error when checking for updates: " + jsonObject.getString("error"));
                } else {
                    String latestVersion = jsonObject.getString("latest_version");

                    String currentVersion = this.getDescription().getVersion();
                    if (currentVersion.equals(latestVersion)) {
                        this.getLogger().info("This plugin is up to date!");
                    } else {
                        this.getLogger().warning("There is a newer version (" + latestVersion + ") available! Please update!");
                    }
                }
            } else {
                this.getLogger().warning("Failed to check for updates. Response code: " + responseCode);
            }
        } catch (Exception e) {
            this.getLogger().warning("Failed to check for updates. Error: " + e.getMessage());
        }
    }
}