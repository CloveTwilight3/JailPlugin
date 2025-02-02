/*
 * Copyright (c) 2025 Mazey-Jessica Emily Twilight
 * Copyright (c) 2025 UnifiedGaming Systems Ltd (Company Number: 16108983)
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.example.jailplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.example.jailplugin.JailPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class UpdateChecker implements Listener {
    private final Plugin plugin;
    private final String githubApiUrl;

    public UpdateChecker(Plugin plugin, String githubApiUrl) {
        this.plugin = plugin;
        this.githubApiUrl = githubApiUrl;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(githubApiUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
                String latestVersion = (String) jsonResponse.get("tag_name");

                String currentVersion = plugin.getDescription().getVersion();

                // Adjust comparison to handle format "v1.0" directly
                if (!currentVersion.trim().equalsIgnoreCase(latestVersion.trim())) {
                    Bukkit.getLogger().warning("A new version of the plugin is available! Latest: " + latestVersion + ", Current: " + currentVersion);
                } else {
                    Bukkit.getLogger().info("The plugin is up-to-date.");
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(githubApiUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONParser parser = new JSONParser();
                    JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
                    String latestVersion = (String) jsonResponse.get("tag_name");

                    String currentVersion = plugin.getDescription().getVersion();

                    if (!currentVersion.trim().equalsIgnoreCase(latestVersion.trim())) {
                        player.sendMessage("§cA new version of the plugin is available! Latest: " + latestVersion + ", Current: " + currentVersion);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Failed to check for updates: " + e.getMessage());
                }
            });
        }
    }
}