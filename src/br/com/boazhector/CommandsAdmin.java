package br.com.boazhector;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
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
            // Verificar se o comando tem argumentos suficientes
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Uso: /delplot <jogador> [numero_do_plot]");
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

            // Verificar se o jogador tem plots
            int plotCount = plugin.getPlotManager().getPlotCount(targetID);
            if (plotCount == 0) {
                sender.sendMessage(ChatColor.RED + "Este jogador não possui terrenos!");
                return true;
            }

            // Verificar se foi especificado um plot específico
            int plotIndex = 0;
            if (args.length > 1) {
                try {
                    plotIndex = Integer.parseInt(args[1]) - 1; // Converter para índice baseado em 0
                    if (plotIndex < 0 || plotIndex >= plotCount) {
                        sender.sendMessage(ChatColor.RED + "Número de plot inválido! O jogador tem " + plotCount + " plots.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Por favor, digite um número válido para o plot!");
                    return true;
                }

                // Remover o plot específico
                plugin.getPlotManager().removePlot(targetID, plotIndex);

                sender.sendMessage(ChatColor.GREEN + "Plot #" + (plotIndex+1) + " de " + targetName + " removido com sucesso!");

                // Notificar o jogador se estiver online
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.RED + "Seu plot #" + (plotIndex+1) + " foi removido por um administrador.");
                }
            } else {
                // Se não foi especificado um plot, remover todos os plots
                List<Plot> plots = plugin.getPlotManager().getAllPlots(targetID);
                int totalRemoved = plots.size();

                for (int i = totalRemoved - 1; i >= 0; i--) {
                    plugin.getPlotManager().removePlot(targetID, i);
                }

                sender.sendMessage(ChatColor.GREEN + "Todos os " + totalRemoved + " plots de " + targetName + " foram removidos com sucesso!");

                // Notificar o jogador se estiver online
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.RED + "Todos os seus plots foram removidos por um administrador.");
                }
            }

            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("plotadmin")) {
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
                plugin.reload();

                player.sendMessage(ChatColor.GREEN + "Configuração e plots recarregados com sucesso!");
                return true;
            }

            else if (subCommand.equals("teleport") || subCommand.equals("tp")) {
                // Verificar argumentos
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /plotadmin teleport <jogador> [numero_do_plot]");
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

                // Verificar se o jogador tem plots
                int plotCount = plugin.getPlotManager().getPlotCount(targetID);
                if (plotCount == 0) {
                    player.sendMessage(ChatColor.RED + "Este jogador não possui terrenos!");
                    return true;
                }

                // Verificar se foi especificado um plot específico
                int plotIndex = 0;
                if (args.length > 2) {
                    try {
                        plotIndex = Integer.parseInt(args[2]) - 1; // Converter para índice baseado em 0
                        if (plotIndex < 0 || plotIndex >= plotCount) {
                            player.sendMessage(ChatColor.RED + "Número de plot inválido! O jogador tem " + plotCount + " plots.");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Por favor, digite um número válido para o plot!");
                        return true;
                    }
                }

                // Teleportar para o plot
                Plot plot = plugin.getPlotManager().getPlot(targetID, plotIndex);
                player.teleport(plot.getCenter());

                player.sendMessage(ChatColor.GREEN + "Teleportado para o plot #" + (plotIndex+1) + " de " + targetName + "!");
                return true;
            }

            else if (subCommand.equals("borders") || subCommand.equals("fence")) {
                // Verificar argumentos
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /plotadmin borders <jogador> [numero_do_plot]");
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

                // Verificar se o jogador tem plots
                int plotCount = plugin.getPlotManager().getPlotCount(targetID);
                if (plotCount == 0) {
                    player.sendMessage(ChatColor.RED + "Este jogador não possui terrenos!");
                    return true;
                }

                // Verificar se foi especificado um plot específico
                int plotIndex = 0;
                if (args.length > 2) {
                    try {
                        plotIndex = Integer.parseInt(args[2]) - 1; // Converter para índice baseado em 0
                        if (plotIndex < 0 || plotIndex >= plotCount) {
                            player.sendMessage(ChatColor.RED + "Número de plot inválido! O jogador tem " + plotCount + " plots.");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Por favor, digite um número válido para o plot!");
                        return true;
                    }
                }

                // Recriar as bordas do plot
                Plot plot = plugin.getPlotManager().getPlot(targetID, plotIndex);
                plot.createBorder();

                player.sendMessage(ChatColor.GREEN + "Bordas do plot #" + (plotIndex+1) + " de " + targetName + " recriadas!");
                return true;
            }

            else if (subCommand.equals("setsize")) {
                // Verificar argumentos
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Uso: /plotadmin setsize <jogador> <tamanho> [numero_do_plot]");
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

                // Verificar se o jogador tem plots
                int plotCount = plugin.getPlotManager().getPlotCount(targetID);
                if (plotCount == 0) {
                    player.sendMessage(ChatColor.RED + "Este jogador não possui terrenos!");
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

                // Verificar se foi especificado um plot específico
                int plotIndex = 0;
                if (args.length > 3) {
                    try {
                        plotIndex = Integer.parseInt(args[3]) - 1; // Converter para índice baseado em 0
                        if (plotIndex < 0 || plotIndex >= plotCount) {
                            player.sendMessage(ChatColor.RED + "Número de plot inválido! O jogador tem " + plotCount + " plots.");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Por favor, digite um número válido para o plot!");
                        return true;
                    }
                }

                // Atualizar tamanho do plot
                Plot plot = plugin.getPlotManager().getPlot(targetID, plotIndex);

                // Verificar sobreposição com outros plots
                List<Plot> allPlots = plugin.getPlotManager().getAllPlots(targetID);
                for (int i = 0; i < allPlots.size(); i++) {
                    if (i == plotIndex) continue; // Ignorar o próprio plot

                    if (plot.overlapsWithNewSize(allPlots.get(i), newSize)) {
                        player.sendMessage(ChatColor.RED + "O novo tamanho sobrepõe outros plots do jogador!");
                        return true;
                    }
                }

                // Atualizar tamanho
                plot.setSize(newSize);
                plot.createBorder();
                plot.save(plotIndex);

                player.sendMessage(ChatColor.GREEN + "Tamanho do plot #" + (plotIndex+1) + " de " + targetName +
                        " alterado para " + newSize + "x" + newSize + "!");

                // Notificar o jogador se estiver online
                if (targetPlayer != null) {
                    targetPlayer.sendMessage(ChatColor.GREEN + "O tamanho do seu plot #" + (plotIndex+1) +
                            " foi alterado para " + newSize + "x" + newSize + " por um administrador.");
                }

                return true;
            }

            else if (subCommand.equals("list")) {
                // Listar todos os plots de um jogador
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Uso: /plotadmin list <jogador>");
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

                // Verificar se o jogador tem plots
                List<Plot> plots = plugin.getPlotManager().getAllPlots(targetID);
                if (plots.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Este jogador não possui terrenos!");
                    return true;
                }

                // Listar todos os plots
                player.sendMessage(ChatColor.YELLOW + "=== Plots de " + targetName + " (" + plots.size() + ") ===");

                for (int i = 0; i < plots.size(); i++) {
                    Plot plot = plots.get(i);
                    player.sendMessage(ChatColor.GOLD + "Plot #" + (i+1) + ": " +
                            ChatColor.GRAY + "Tamanho: " + plot.getSize() + "x" + plot.getSize() + ", " +
                            "Posição: " + plot.getCenter().getBlockX() + ", " + plot.getCenter().getBlockZ());
                }

                return true;
            }

            else if (subCommand.equals("setlimit")) {
                // Comando apenas para informação, já que o limite é baseado em permissões
                player.sendMessage(ChatColor.YELLOW + "Os limites de plots são baseados em permissões:");
                player.sendMessage(ChatColor.GOLD + "Jogadores normais: " + ChatColor.WHITE + "1 plot");
                player.sendMessage(ChatColor.GOLD + "VIP Cosmo (vipcosmo.perm): " + ChatColor.WHITE + "2 plots");
                player.sendMessage(ChatColor.GOLD + "VIP Legacy (viplegacy.perm): " + ChatColor.WHITE + "3 plots");
                player.sendMessage(ChatColor.GOLD + "VIP Astral (vipastral.perm): " + ChatColor.WHITE + "5 plots");
                player.sendMessage(ChatColor.GOLD + "Admin (plotsystem.admin): " + ChatColor.WHITE + "Ilimitado");

                return true;
            }

            else {
                sendAdminHelp(player);
                return true;
            }

        }

        return false;
    }

    private void sendAdminHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "==== PlotSystem Admin ====");
        player.sendMessage(ChatColor.GOLD + "/plotadmin reload" + ChatColor.WHITE + " - Recarregar configuração e plots");
        player.sendMessage(ChatColor.GOLD + "/plotadmin teleport <jogador> [#plot]" + ChatColor.WHITE + " - Teleportar para o plot de um jogador");
        player.sendMessage(ChatColor.GOLD + "/plotadmin borders <jogador> [#plot]" + ChatColor.WHITE + " - Recriar as bordas do plot de um jogador");
        player.sendMessage(ChatColor.GOLD + "/plotadmin setsize <jogador> <tamanho> [#plot]" + ChatColor.WHITE + " - Alterar o tamanho do plot");
        player.sendMessage(ChatColor.GOLD + "/plotadmin list <jogador>" + ChatColor.WHITE + " - Listar todos os plots de um jogador");
        player.sendMessage(ChatColor.GOLD + "/plotadmin setlimit" + ChatColor.WHITE + " - Ver informações sobre limites de plots");
        player.sendMessage(ChatColor.GOLD + "/delplot <jogador> [#plot]" + ChatColor.WHITE + " - Remover plot(s) de um jogador");
    }
}