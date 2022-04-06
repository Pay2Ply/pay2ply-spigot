package com.pay2ply.spigot.command;

import com.pay2ply.spigot.Pay2Ply;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Pay2PlyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String label, String[] args) {
        if (!commandSender.hasPermission("pay2ply.command")) {
            commandSender.sendMessage(String.format("%sVocê não possui permissão para executar este comando.", ChatColor.RED));
            return true;
        }

        if (args.length > 0) {
            String token = args[0].trim().toLowerCase();

            Pay2Ply.getInstance().getSdk().setToken(token);
            Pay2Ply.getInstance().getConfig().set("settings.token", token);
            Pay2Ply.getInstance().saveConfig();

            commandSender.sendMessage(String.format("%sSe o token do servidor estiver correto, a loja será vinculada em alguns instantes.", ChatColor.GREEN));

            return true;
        } else {
            commandSender.sendMessage(String.format("%s/%s <token do servidor>.", ChatColor.RED, "pay2ply"));
        }

        return false;
    }
}
