package com.pay2ply.spigot;

import com.pay2ply.sdk.SDK;
import com.pay2ply.sdk.dispense.Dispense;
import com.pay2ply.spigot.command.Pay2PlyCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.CompletableFuture;

public class Pay2Ply extends JavaPlugin {
    private static Pay2Ply instance;
    private final SDK sdk = new SDK();

    public Pay2Ply() {
        Pay2Ply.instance = this;
    }

    public static Pay2Ply getInstance() {
        return Pay2Ply.instance;
    }

    public SDK getSdk() {
        return sdk;
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        this.sdk.setToken(this.getConfig().getString("settings.token"));

        getCommand("pay2ply").setExecutor(new Pay2PlyCommand());

        new BukkitRunnable() {
            @Override
            public void run() {
                // Use supplyAsync to handle the future
                CompletableFuture.supplyAsync(() -> {
                    try {
                        // Recupera os dispensers de forma assíncrona
                        return sdk.getDispenses().join(); // Usa join() para esperar o resultado
                    } catch (Exception e) {
                        Bukkit.getLogger().warning(String.format("[%s] %s", getDescription().getName(), e.getMessage()));
                        return new Dispense[0]; // Retorna um array vazio em caso de erro
                    }
                }).thenAccept(dispensesArray -> {
                    if (dispensesArray != null && dispensesArray.length > 0) { // Verifica se o array não é vazio
                        for (Dispense dispense : dispensesArray) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    Player player = getServer().getPlayerExact(dispense.getUsername());

                                    if (player != null) {
                                        try {
                                            sdk.update(dispense.getUsername(), dispense.getId());
                                            getServer().dispatchCommand(getServer().getConsoleSender(), dispense.getCommand());

                                            if (getConfig().getBoolean("settings.messages")) {
                                                Bukkit.getLogger().info(String.format("[%s] O produto de %s foi ativo.", getDescription().getName(), dispense.getUsername()));
                                            }
                                        } catch (Exception exception) {
                                            Bukkit.getLogger().warning(String.format("[%s] %s", getDescription().getName(), exception.getMessage()));
                                        }
                                    }
                                }
                            }.runTask(getInstance());
                        }
                    }
                }).exceptionally(ex -> {
                    Bukkit.getLogger().warning(String.format("[%s] %s", getDescription().getName(), ex.getMessage()));
                    return null;
                });
            }
        }.runTaskTimerAsynchronously(this, 60 * 20, 20 * 20);
    }
}
