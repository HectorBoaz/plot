package br.com.boazhector;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CommandsAdmin implements CommandExecutor {

    private final Main plugin;

    public CommandsAdmin(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("plotsystem.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("delplot")) {
            // Verificar se o comando tem argumentos
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Uso: /delplot <jogador>");
                return true;
            }

            // Obter jogador alvo
            String targetName = args[0];
            Player targetPlayer = plugin.getServer().getPlayer(targetName);
            UUID targetID = null;

            if (targetPlayer != null) {
                targetID = targetPlayer.getUniqueId();
            } else {
                // Tentar encontrar pelo nome no cache
                for (String uuidStr : plugin.getConfig().getConfigurationSection("player-names").getKeys(false)) {
                    if (plugin.getConfig().getString("player-names." + uuidStr).equalsIgnoreCase(targetName)) {
                        try {
                            targetID = UUID.fromString(uuidStr);
                            break;
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("UUID inválido no cache: " + uuidStr);
                        }
                    }
                }

                if (targetID == null) {
                    sender.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                    return true;
                }
            }

            // Verificar se o jogador tem um plot
            if (!plugin.getPlotManager().hasPlot(targetID)) {
                sender.sendMessage(ChatColor.RED + "Este jogador não possui um terreno!");
                return true;
            }

            // Remover o plot
            plugin.getPlotManager().removePlot(targetID);

            sender.sendMessage(ChatColor.GREEN + "Terreno de " + targetName + " removido com sucesso!");

            // Notificar o jogador se estiver online
            if (targetPlayer != null) {
                targetPlayer.sendMessage(ChatColor.RED + "Seu terreno foi removido por um administrador.");
            }

            return true;
        } else if (cmd.getName().equalsIgnoreCase("plotadmin")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores!");
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 1) {
                sendAdminHelp(player);
                return true;
            }

            String subCommand = args[0].toLowerCase();

            if (subCommand.equals("reload")) {
                // Recarregar configuração
                plugin.reloadConfig();

                // Recarregar plots
                plugin.getPlotManager().savePlots();
                plugin.getPlotManager().loadPlots();

                player.sendMessage(ChatColor.GREEN + "Configuração e plots recarregados com sucesso!");
                return true;
            } else if (subCommand.equals("teleport") || subCommand.equals("tp")) {
                // Verificar argumentos
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /plotadmin teleport <jogador>");
                    return true;
                }

                // Obter jogador alvo
                String targetName = args[1];
                Player targetPlayer = plugin.getServer().getPlayer(targetName);
                UUID targetID = null;

                if (targetPlayer != null) {
                    targetID = targetPlayer.getUniqueId();
                } else {
                    // Tentar encontrar pelo nome no cache
                    for (String uuidStr : plugin.getConfig().getConfigurationSection("player-names").getKeys(false)) {
                        if (plugin.getConfig().getString("player-names." + uuidStr).equalsIgnoreCase(targetName)) {
                            try {
                                targetID = UUID.fromString(uuidStr);
                                break;
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("UUID inválido no cache: " + uuidStr);
                            }
                        }
                    }

                    if (targetID == null) {
                        player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                        return true;
                    }
                }

                // Verificar se o jogador tem um plot
                if (!plugin.getPlotManager().hasPlot(targetID)) {
                    player.sendMessage(ChatColor.RED + "Este jogador não possui um terreno!");
                    return true;
                }

                // Teleportar para o plot
                Plot plot = plugin.getPlotManager().getPlot(targetID);
                player.teleport(plot.getCenter());

                player.sendMessage(ChatColor.GREEN + "Teleportado para o terreno de " + targetName + "!");
                return true;
            } else if (subCommand.equals("borders") || subCommand.equals("fence")) {
                // Verificar argumentos
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /plotadmin borders <jogador>");
                    return true;
                }

                // Obter jogador alvo
                String targetName = args[1];
                Player targetPlayer = plugin.getServer().getPlayer(targetName);
                UUID targetID = null;

                if (targetPlayer != null) {
                    targetID = targetPlayer.getUniqueId();
                } else {
                    // Tentar encontrar pelo nome no cache
                    for (String uuidStr : plugin.getConfig().getConfigurationSection("player-names").getKeys(false)) {
                        if (plugin.getConfig().getString("player-names." + uuidStr).equalsIgnoreCase(targetName)) {
                            try {
                                targetID = UUID.fromString(uuidStr);
                                break;
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("UUID inválido no cache: " + uuidStr);
                            }
                        }
                    }

                    if (targetID == null) {
                        player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                        return true;
                    }
                }

                // Verificar se o jogador tem um plot
                if (!plugin.getPlotManager().hasPlot(targetID)) {
                    player.sendMessage(ChatColor.RED + "Este jogador não possui um terreno!");
                    return true;
                }

                // Recriar as bordas do plot
                Plot plot = plugin.getPlotManager().getPlot(targetID);
                plot.createBorder();

                player.sendMessage(ChatColor.GREEN + "Bordas do terreno de " + targetName + " recriadas!");
                return true;
            } else if (subCommand.equals("setsize")) {
                // Verificar argumentos
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Uso: /plotadmin setsize <jogador> <tamanho>");
                    return true;
                }

                // Obter jogador alvo
                String targetName = args[1];
                Player targetPlayer = plugin.getServer().getPlayer(targetName);
                UUID targetID = null;

                if (targetPlayer != null) {
                    targetID = targetPlayer.getUniqueId();
                } else {
                    // Tentar encontrar pelo nome no cache
                    for (String uuidStr : plugin.getConfig().getConfigurationSection("player-names").getKeys(false)) {
                        if (plugin.getConfig().getString("player-names." + uuidStr).equalsIgnoreCase(targetName)) {
                            try {
                                targetID = UUID.fromString(uuidStr);
                                break;
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("UUID inválido no cache: " + uuidStr);
                            }
                        }
                    }

                    if (targetID == null) {
                        player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                        return true;
                    }
                }

                // Verificar se o jogador tem um plot
                if (!plugin.getPlotManager().hasPlot(targetID)) {
                    player.sendMessage(ChatColor.RED + "Este jogador não possui um terreno!");
                    return true;
                }

                // Obter novo tamanho
                int newSize;
                try {
                    newSize = Integer.parseInt(args[2]);
                    if (newSize <= 0) {
                        player.sendMessage(ChatColor.RED + "O tamanho deve ser maior que zero!");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Tamanho inválido!");
                    return true;
                }

                // Atualizar tamanho do plot
                Plot plot = plugin.getPlotManager().getPlot(targetID);

                // Verificar se o novo tamanho não sobrepõe outros plots
                for (UUID uuid : plugin.getPlotManager().getAllPlotOwners()) {
                    if (uuid.equals(targetID)) continue;

                    Plot otherPlot = plugin.getPlotManager().getPlot(uuid);
                    if (plot.overlapsWithNewSize(otherPlot, newSize)) {
                        player.sendMessage(ChatColor.RED + "O novo tamanho sobrepõe outros terrenos!");
                        return true;
                    }
                }

                // Atualizar tamanho
                plot.setSize(newSize);
                plot.createBorder();
                plot.save();

                player.sendMessage(ChatColor.GREEN + "Tamanho do terreno de " + targetName + " alterado para " + newSize + "x" + newSize + "!");

                // Notificar o jogador se estiver online
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.GREEN + "O tamanho do seu terreno foi alterado para " + newSize + "x" + newSize + " por um administrador.");
                }

                return true;
            } else {
                sendAdminHelp(player);
                return true;
            }

        }

        return false;
    }

    private void sendAdminHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "==== PlotSystem Admin ====");
        player.sendMessage(ChatColor.GOLD + "/plotadmin reload" + ChatColor.WHITE + " - Recarregar configuração e plots");
        player.sendMessage(ChatColor.GOLD + "/plotadmin teleport <jogador>" + ChatColor.WHITE + " - Teleportar para o terreno de um jogador");
        player.sendMessage(ChatColor.GOLD + "/plotadmin borders <jogador>" + ChatColor.WHITE + " - Recriar as bordas do terreno de um jogador");
        player.sendMessage(ChatColor.GOLD + "/plotadmin setsize <jogador> <tamanho>" + ChatColor.WHITE + " - Alterar o tamanho do terreno de um jogador");
        player.sendMessage(ChatColor.GOLD + "/delplot <jogador>" + ChatColor.WHITE + " - Remover o terreno de um jogador");
    }
}