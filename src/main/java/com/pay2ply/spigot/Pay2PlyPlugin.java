package com.pay2ply.spigot;

import com.pay2ply.sdk.SDK;
import com.pay2ply.sdk.dispense.Dispense;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public class Pay2PlyPlugin extends JavaPlugin {

    public static final SDK SDK = new SDK();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        SDK.setToken(getConfig().getString("settings.token"));

        registerCommand();

        startCheckTask();
    }

    private void startCheckTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                this,
                () -> CompletableFuture.supplyAsync(() -> {
                            try {
                                return SDK.getDispenses().join();
                            } catch (Exception exception) {
                                warning(getDescription().getName(), exception.getMessage());
                                return new Dispense[0];
                            }
                        })
                        .thenAccept(dispensesArray -> runSync(() -> processDispenses(dispensesArray)))
                        .exceptionally(exception -> {
                            warning(getDescription().getName(), exception.getMessage());
                            return null;
                        }),
                0L,
                1_200L
        );
    }

    private void registerCommand() {
        getCommand("pay2ply").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("pay2ply.command")) {
                sender.sendMessage(ChatColor.RED + "Você não possui permissão para executar este comando.");
                return false;
            }

            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Uso incorreto do comando. Utilize: /pay2ply <token do servidor>.");
                return false;
            }

            final String token = args[0].trim().toLowerCase();

            SDK.setToken(token);

            getConfig().set("settings.token", token);
            saveConfig();

            sender.sendMessage(ChatColor.GREEN + "Se o token do servidor estiver correto, a loja será vinculada em alguns instantes.");
            return true;
        });
    }

    private void processDispenses(Dispense[] dispenses) {
        for (Dispense dispense : dispenses) {
            final Player player = Bukkit.getServer().getPlayerExact(dispense.getUsername());
            if (player == null) return;

            try {
                SDK.update(dispense.getUsername(), dispense.getId());

                executeCommand(dispense.getCommand());

                if (getConfig().getBoolean("settings.debug")) {
                    info(
                            "[%s] O produto de %s foi ativado.",
                            getDescription().getName(),
                            dispense.getUsername()
                    );
                }
            } catch (Exception exception) {
                warning(getDescription().getName(), exception.getMessage());
            }
        }
    }

    private void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(this, runnable);
    }

    private void executeCommand(String command) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private void info(String message, Object... objects) {
        getLogger().info(String.format(message, objects));
    }

    private void warning(Object... objects) {
        getLogger().warning(String.format("[%s] %s", objects));
    }

    public static Pay2PlyPlugin getPlugin() {
        return getPlugin(Pay2PlyPlugin.class);
    }

}