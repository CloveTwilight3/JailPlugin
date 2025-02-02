/*
 * Copyright (c) 2025 Mazey-Jessica Emily Twilight
 * Copyright (c) 2025 UnifiedGaming Systems Ltd (Company Number: 16108983)
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.example.jailplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.ChatColor;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import com.org.clovelib.CloveLib;

public class JailPlugin extends JavaPlugin implements Listener {
    private Location jailLocation;
    private Location unjailLocation;
    private final Map<UUID, Long> jailedPlayers = new HashMap<>();

    @Override
    public void onEnable() {
        new BukkitRunnable() {
            @Override
            public void run() {
                CloveLib cloveLib = CloveLib.getInstance();
                if (cloveLib == null) {
                    getLogger().severe("CloveLib is still not initialized! Disabling JailPlugin.");
                    getServer().getPluginManager().disablePlugin(JailPlugin.this);
                    return;
                }

                getLogger().info("CloveLib successfully accessed by JailPlugin.");
                getLogger().info("CloveLib instance hash: " + System.identityHashCode(cloveLib));

                // Proceed with plugin initialization
                saveDefaultConfig();
                loadLocations();
                startUnjailTask();
                getServer().getPluginManager().registerEvents(JailPlugin.this, JailPlugin.this);

                getLogger().info("JailPlugin has been enabled successfully!");
            }
        }.runTaskLater(this, 1L);
    }

    @Override
    public void onDisable() {
        saveLocations();
        getLogger().info("JailPlugin has been disabled!");
    }

    @Override
    public void onLoad() {
        getLogger().info("JailPlugin is loading...");
        CloveLib cloveLib = CloveLib.getInstance();
        if (cloveLib == null) {
            getLogger().warning("CloveLib instance is null during JailPlugin onLoad. This may resolve during onEnable.");
        } else {
            getLogger().info("CloveLib instance is accessible during JailPlugin onLoad.");
        }
    }


    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            if (CloveLib.getInstance().isPlayerJailed(attacker.getUniqueId())) {
                attacker.sendMessage(ChatColor.RED + "You cannot attack other players while jailed!");
                event.setCancelled(true);
            }
        }
    }

    private void startUnjailTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                jailedPlayers.entrySet().removeIf(entry -> {
                    UUID playerId = entry.getKey();
                    long unjailTime = entry.getValue();
                    if (currentTime >= unjailTime) {
                        Player player = getServer().getPlayer(playerId);
                        if (player != null && unjailLocation != null) {
                            player.teleport(unjailLocation);
                            player.sendMessage(ChatColor.GREEN + "You have been released from jail!");
                        }
                        CloveLib cloveLib = CloveLib.getInstance();
                        if (cloveLib != null) {
                            cloveLib.clearPlayerJailData(playerId);
                        } else {
                            getLogger().severe("CloveLib instance is null during unjail task!");
                        }
                        return true;
                    }
                    return false;
                });
            }
        }.runTaskTimer(this, 20, 20);
    }

    private void loadLocations() {
        if (getConfig().contains("jail")) {
            World world = getServer().getWorld(getConfig().getString("jail.world"));
            double x = getConfig().getDouble("jail.x");
            double y = getConfig().getDouble("jail.y");
            double z = getConfig().getDouble("jail.z");
            float yaw = (float) getConfig().getDouble("jail.yaw");
            float pitch = (float) getConfig().getDouble("jail.pitch");
            jailLocation = new Location(world, x, y, z, yaw, pitch);
        }

        if (getConfig().contains("unjail")) {
            World world = getServer().getWorld(getConfig().getString("unjail.world"));
            double x = getConfig().getDouble("unjail.x");
            double y = getConfig().getDouble("unjail.y");
            double z = getConfig().getDouble("unjail.z");
            float yaw = (float) getConfig().getDouble("unjail.yaw");
            float pitch = (float) getConfig().getDouble("unjail.pitch");
            unjailLocation = new Location(world, x, y, z, yaw, pitch);
        }
    }

    private void saveLocations() {
        if (jailLocation != null) {
            getConfig().set("jail.world", jailLocation.getWorld().getName());
            getConfig().set("jail.x", jailLocation.getX());
            getConfig().set("jail.y", jailLocation.getY());
            getConfig().set("jail.z", jailLocation.getZ());
            getConfig().set("jail.yaw", jailLocation.getYaw());
            getConfig().set("jail.pitch", jailLocation.getPitch());
        }

        if (unjailLocation != null) {
            getConfig().set("unjail.world", unjailLocation.getWorld().getName());
            getConfig().set("unjail.x", unjailLocation.getX());
            getConfig().set("unjail.y", unjailLocation.getY());
            getConfig().set("unjail.z", unjailLocation.getZ());
            getConfig().set("unjail.yaw", unjailLocation.getYaw());
            getConfig().set("unjail.pitch", unjailLocation.getPitch());
        }

        saveConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "setjail":
                return handleSetJail(sender, args);
            case "setunjail":
                return handleSetUnjail(sender, args);
            case "jail":
                return handleJail(sender, args);
            case "unjail":
                return handleUnjail(sender, args);
            case "jailreload":
                return handleReload(sender);
            case "jailhelp":
                return handleHelp(sender);
            default:
                return false;
        }
    }

    private boolean handleSetJail(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                jailLocation = new Location(player.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid coordinates! Use: /setjail <x> <y> <z>");
                return true;
            }
        } else if (args.length == 0) {
            jailLocation = player.getLocation();
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /setjail or /setjail <x> <y> <z>");
            return true;
        }

        saveLocations();
        player.sendMessage(ChatColor.GREEN + "Jail location has been set!");
        return true;
    }

    private boolean handleSetUnjail(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                unjailLocation = new Location(player.getWorld(), x, y, z);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid coordinates! Use: /setunjail <x> <y> <z>");
                return true;
            }
        } else if (args.length == 0) {
            unjailLocation = player.getLocation();
        } else {
            player.sendMessage(ChatColor.RED + "Usage: /setunjail or /setunjail <x> <y> <z>");
            return true;
        }

        saveLocations();
        player.sendMessage(ChatColor.GREEN + "Unjail location has been set!");
        return true;
    }

    private boolean handleJail(CommandSender sender, String[] args) {
        if (jailLocation == null) {
            sender.sendMessage(ChatColor.RED + "Jail location has not been set! Use /setjail first.");
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /jail <player> [time]");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        long jailTime = 0;
        if (args.length == 2) {
            try {
                jailTime = Long.parseLong(args[1]) * 1000L;
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid time format! Use an integer for seconds.");
                return true;
            }
        }

        target.teleport(jailLocation);
        target.sendMessage(ChatColor.RED + "You have been jailed!");
        sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " has been jailed!");

        CloveLib cloveLib = CloveLib.getInstance();
        if (cloveLib != null) {
            cloveLib.setPlayerJailData(target.getUniqueId(), new CloveLib.JailData(true, System.currentTimeMillis() + jailTime, jailLocation, unjailLocation));
        } else {
            getLogger().severe("CloveLib instance is null while jailing a player!");
        }

        return true;
    }

    private boolean handleUnjail(CommandSender sender, String[] args) {
        if (unjailLocation == null) {
            sender.sendMessage(ChatColor.RED + "Unjail location has not been set! Use /setunjail first.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /unjail <player>");
            return true;
        }

        Player target = getServer().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found!");
            return true;
        }

        target.teleport(unjailLocation);
        target.sendMessage(ChatColor.GREEN + "You have been released from jail!");
        sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " has been released from jail!");

        CloveLib.getInstance().clearPlayerJailData(target.getUniqueId());

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        reloadConfig();
        loadLocations();
        sender.sendMessage(ChatColor.GREEN + "JailPlugin configuration reloaded!");
        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "JailPlugin Commands:");
        sender.sendMessage(ChatColor.GREEN + "/setjail [x y z]" + ChatColor.WHITE + " - Set the jail location.");
        sender.sendMessage(ChatColor.GREEN + "/setunjail [x y z]" + ChatColor.WHITE + " - Set the unjail location.");
        sender.sendMessage(ChatColor.GREEN + "/jail <player> [time]" + ChatColor.WHITE + " - Jail a player. Optional time in seconds.");
        sender.sendMessage(ChatColor.GREEN + "/unjail <player>" + ChatColor.WHITE + " - Unjail a player.");
        sender.sendMessage(ChatColor.GREEN + "/jailreload" + ChatColor.WHITE + " - Reload the plugin configuration.");
        sender.sendMessage(ChatColor.GREEN + "/jailhelp" + ChatColor.WHITE + " - Show this help message.");
        return true;
    }
}
