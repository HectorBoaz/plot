package br.com.boazhector;

import net.byebye.balance.BalanceAPI;
import net.byebye.balance.Economy;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;

//§
public class Commands implements CommandExecutor {

    private final Main plugin;
    private final Economy eco = BalanceAPI.getEconomy();
    private final Map<UUID, Long> cooldowns = new HashMap<>();


    public Commands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores!");
            return true;
        }

        Player player = (Player) sender;
        Location loc = (Location) player.getLocation();

        if (cmd.getName().equalsIgnoreCase("fly")) {
            Events events = new Events();

            // Verificar se o jogador é admin ou está em modo criativo/espectador
            boolean isAdmin = player.hasPermission("plotsystem.admin");
            boolean isCreativeOrSpectator = player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;

            // Verificar se está em um plot onde tem permissão ou se é admin/creative
            if (events.hasPermissionFly(player, loc) || isAdmin || isCreativeOrSpectator) {
                if (!(player.isFlying())) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    player.sendMessage("§aFly ativado!");
                } else {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage("§cFly desativado!");
                }
            } else {
                player.sendMessage("§cVocê só pode usar o fly dentro do seu plot!");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("comprarplot")) {
            // Verificar se já atingiu o limite de plots
            int maxPlots = plugin.getMaxPlots(player);
            int currentPlots = plugin.getPlotManager().getPlotCount(player.getUniqueId());

            if (currentPlots >= maxPlots) {
                player.sendMessage(ChatColor.RED + "Você atingiu seu limite de " + maxPlots + " plots!");

                // Exibir informações VIP se não for OP
                if (!player.hasPermission("plotsystem.admin")) {
                    player.sendMessage(ChatColor.GOLD + "Adquira VIP para ter mais plots:");
                    player.sendMessage(ChatColor.YELLOW + "VIP Cosmo: " + ChatColor.WHITE + "2 plots");
                    player.sendMessage(ChatColor.YELLOW + "VIP Legacy: " + ChatColor.WHITE + "3 plots");
                    player.sendMessage(ChatColor.YELLOW + "VIP Astral: " + ChatColor.WHITE + "5 plots");
                }

                return true;
            }

            // Abrir GUI de seleção de tamanho
            GuiManager.openPlotSizeSelectionGui(player);
            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("meuplot")) {
            long tempoAgora = System.currentTimeMillis();
            UUID uuid = player.getUniqueId();

            if (cooldowns.containsKey(uuid)) {
                long ultimoUso = cooldowns.get(uuid);
                long tempoRestante = (ultimoUso + 30000) - tempoAgora;

                if (tempoRestante > 0) {
                    double segundos = tempoRestante / 1000.0;
                    player.sendMessage(ChatColor.RED + "Aguarde " + String.format("%.1f", segundos) + " segundos para usar este comando novamente.");
                    return true;
                }
            }

            cooldowns.put(uuid, tempoAgora); // registra uso

            int plotCount = plugin.getPlotManager().getPlotCount(uuid);

            if (plotCount == 0) {
                player.sendMessage(ChatColor.RED + "Você ainda não possui um terreno!");
            } else if (plotCount == 1) {
                Plot plot = plugin.getPlotManager().getPlot(uuid);
                player.teleport(plot.getCenter());
                player.sendMessage(ChatColor.GREEN + "Teleportado para seu terreno!");
            } else {
                GuiManager.openPlotSelectionGui(player);
            }
            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("irplot")) {
            // Comando para teleportar para um plot específico pelo índice
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Uso: /irplot <número>");
                return true;
            }

            try {
                int index = Integer.parseInt(args[0]) - 1; // Usuário vê plot 1, 2, 3, mas internamente é 0, 1, 2

                if (index < 0) {
                    player.sendMessage(ChatColor.RED + "O número do plot deve ser positivo!");
                    return true;
                }

                Plot plot = plugin.getPlotManager().getPlot(player.getUniqueId(), index);

                if (plot == null) {
                    player.sendMessage(ChatColor.RED + "Você não possui um plot com o número " + (index + 1) + "!");
                    return true;
                }

                player.teleport(plot.getCenter());
                player.sendMessage(ChatColor.GREEN + "Teleportado para seu plot #" + (index + 1) + "!");
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Por favor, digite um número válido!");
            }

            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("meusplots")) {
            // Listar todos os plots do jogador
            List<Plot> plots = plugin.getPlotManager().getAllPlots(player.getUniqueId());

            if (plots.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Você ainda não possui nenhum terreno!");
                return true;
            }

            player.sendMessage(ChatColor.YELLOW + "=== Seus Plots (" + plots.size() + "/" + plugin.getMaxPlots(player) + ") ===");

            for (int i = 0; i < plots.size(); i++) {
                Plot plot = plots.get(i);
                player.sendMessage(ChatColor.GOLD + "Plot #" + (i + 1) + ": " +
                        ChatColor.GRAY + "Tamanho: " + plot.getSize() + "x" + plot.getSize() + ", " +
                        "Posição: " + plot.getCenter().getBlockX() + ", " + plot.getCenter().getBlockZ());
            }

            player.sendMessage(ChatColor.YELLOW + "Use /irplot <número> para teleportar para um plot específico");

            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("venderplot")) {
            // Verificar se tem plots
            int plotCount = plugin.getPlotManager().getPlotCount(player.getUniqueId());

            if (plotCount == 0) {
                player.sendMessage(ChatColor.RED + "Você não possui um terreno!");
                return true;
            }

            // Se especificou um número de plot nos argumentos
            if (args.length > 0) {
                try {
                    int plotIndex = Integer.parseInt(args[0]) - 1;
                    if (plotIndex < 0 || plotIndex >= plotCount) {
                        player.sendMessage(ChatColor.RED + "Número de plot inválido! Você tem " + plotCount + " plots.");
                        return true;
                    }

                    plugin.setSelectedPlotIndex(player, plotIndex);
                    GuiManager.openConfirmSellPlotGui(player);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Por favor, digite um número válido!");
                }
                return true;
            }

            if (plotCount == 1) {
                // Só tem um plot, abre a GUI de confirmação direta
                GuiManager.openConfirmSellPlotGui(player);
            } else {
                // Tem vários plots, abre a GUI para selecionar qual vender
                GuiManager.openPlotSelectionForSellGui(player);
            }

            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("plotinfo")) {
            // Obter plot na localização atual
            Plot plot = plugin.getPlotManager().getPlotAt(player.getLocation());

            if (plot == null) {
                player.sendMessage(ChatColor.RED + "Você não está dentro de um terreno protegido.");
                return true;
            }

            // Obter nome do proprietário
            String ownerName = plot.getOwnerName();

            player.sendMessage(ChatColor.YELLOW + "Terreno de: " + ChatColor.GREEN + ownerName);
            player.sendMessage(ChatColor.YELLOW + "Centro: " + ChatColor.GRAY +
                    plot.getCenter().getBlockX() + ", " +
                    plot.getCenter().getBlockZ());
            player.sendMessage(ChatColor.YELLOW + "Tamanho: " + ChatColor.GRAY +
                    plot.getSize() + "x" + plot.getSize());

            // Se o jogador é o dono, mostra o número do plot
            if (plot.getOwnerID().equals(player.getUniqueId())) {
                List<Plot> plots = plugin.getPlotManager().getAllPlots(player.getUniqueId());
                for (int i = 0; i < plots.size(); i++) {
                    if (plots.get(i).getCenter().equals(plot.getCenter())) {
                        player.sendMessage(ChatColor.YELLOW + "Este é seu plot #" + (i + 1));
                        break;
                    }
                }
            }

            Set<UUID> members = plot.getMembers();
            if (!members.isEmpty()) {
                player.sendMessage(ChatColor.YELLOW + "Inquilinos:");
                for (UUID memberID : members) {
                    String memberName = getMemberName(memberID);
                    player.sendMessage("- " + ChatColor.WHITE + memberName);
                }
            } else {
                player.sendMessage(ChatColor.YELLOW + "Este terreno não possui inquilinos.");
            }

            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("adicionarinquilino")) {
            // Verificar se o comando tem argumentos
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Uso: /adicionarinquilino <jogador>");
                return true;
            }

            // Verificar se tem plots
            int plotCount = plugin.getPlotManager().getPlotCount(player.getUniqueId());

            if (plotCount == 0) {
                player.sendMessage(ChatColor.RED + "Você não possui um terreno!");
                return true;
            }

            // Obter jogador alvo
            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                return true;
            }

            // Verificar se o jogador alvo já tem um plot
            if (plugin.getPlotManager().hasPlot(target.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Esse jogador já possui um terreno!");
                return true;
            }

            // Se o jogador tem múltiplos plots, pergunta em qual plot adicionar inquilino
            if (plotCount > 1 && args.length < 2) {
                plugin.setTargetPlayer(player, target);
                GuiManager.openPlotSelectionForAddTenantGui(player);
                return true;
            }

            // Se especificou o número do plot
            int plotIndex = 0;
            if (args.length > 1) {
                try {
                    plotIndex = Integer.parseInt(args[1]) - 1;
                    if (plotIndex < 0 || plotIndex >= plotCount) {
                        player.sendMessage(ChatColor.RED + "Número de plot inválido! Você tem " + plotCount + " plots.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "O número do plot deve ser um valor numérico!");
                    return true;
                }
            }

            Plot plot = plugin.getPlotManager().getPlot(player.getUniqueId(), plotIndex);

            // Verificar limite de inquilinos
            int maxTenants = plugin.getConfig().getInt("plots.max-tenants", 3);
            if (plot.getMembers().size() >= maxTenants) {
                player.sendMessage(ChatColor.RED + "Você atingiu o limite máximo de inquilinos (" + maxTenants + ").");
                return true;
            }

            // Verificar se já é inquilino
            if (plot.isMember(target.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Esse jogador já é inquilino do seu terreno!");
                return true;
            }

            // Salvar o jogador alvo e abrir GUI de confirmação
            plugin.setTargetPlayer(player, target);
            plugin.setSelectedPlotIndex(player, plotIndex);
            GuiManager.openAddTenantGui(player, target);

            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("removerinquilino")) {
            // Verificar se o comando tem argumentos
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Uso: /removerinquilino <jogador>");
                return true;
            }

            // Verificar se tem plots
            int plotCount = plugin.getPlotManager().getPlotCount(player.getUniqueId());

            if (plotCount == 0) {
                player.sendMessage(ChatColor.RED + "Você não possui um terreno!");
                return true;
            }

            // Obter jogador alvo
            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                return true;
            }

            // Encontrar em qual plot o jogador é inquilino
            List<Plot> plots = plugin.getPlotManager().getAllPlots(player.getUniqueId());
            int plotIndex = -1;

            // Se é especificado o número do plot
            if (args.length > 1) {
                try {
                    plotIndex = Integer.parseInt(args[1]) - 1;
                    if (plotIndex < 0 || plotIndex >= plotCount) {
                        player.sendMessage(ChatColor.RED + "Número de plot inválido! Você tem " + plotCount + " plots.");
                        return true;
                    }

                    Plot plot = plots.get(plotIndex);
                    if (!plot.isMember(target.getUniqueId())) {
                        player.sendMessage(ChatColor.RED + "Esse jogador não é inquilino do seu plot #" + (plotIndex+1) + ".");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "O número do plot deve ser um valor numérico!");
                    return true;
                }
            } else {
                // Procurar o jogador em todos os plots
                for (int i = 0; i < plots.size(); i++) {
                    if (plots.get(i).isMember(target.getUniqueId())) {
                        if (plotIndex == -1) {
                            plotIndex = i;
                        } else {
                            // O jogador é inquilino em múltiplos plots, perguntar qual remover
                            plugin.setTargetPlayer(player, target);
                            plugin.setTargetPlayer(player, target); // Salva o jogador alvo
                            GuiManager.openPlotSelectionForRemoveTenantGui(player); // Chama a GUI com apenas o player
                            return true;
                        }
                    }
                }

                if (plotIndex == -1) {
                    player.sendMessage(ChatColor.RED + "Esse jogador não é inquilino de nenhum dos seus terrenos.");
                    return true;
                }
            }

            // Salvar o jogador alvo e abrir GUI de confirmação
            plugin.setTargetPlayer(player, target);
            plugin.setSelectedPlotIndex(player, plotIndex);
            GuiManager.openRemoveTenantGui(player, target);

            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("listarinquilinos")) {
            // Verificar se tem plots
            int plotCount = plugin.getPlotManager().getPlotCount(player.getUniqueId());

            if (plotCount == 0) {
                player.sendMessage(ChatColor.RED + "Você não possui um terreno!");
                return true;
            }

            // Se tiver apenas um plot, mostra os inquilinos direto
            if (plotCount == 1) {
                Plot plot = plugin.getPlotManager().getPlot(player.getUniqueId());
                Set<UUID> members = plot.getMembers();

                if (members.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "Você não possui inquilinos.");
                    return true;
                }

                player.sendMessage(ChatColor.YELLOW + "Inquilinos:");
                for (UUID memberID : members) {
                    String memberName = getMemberName(memberID);
                    player.sendMessage("- " + ChatColor.WHITE + memberName);
                }
            } else {
                // Se tem múltiplos plots
                int plotIndex = 0;

                // Verificar se especificou um plot
                if (args.length > 0) {
                    try {
                        plotIndex = Integer.parseInt(args[0]) - 1;
                        if (plotIndex < 0 || plotIndex >= plotCount) {
                            player.sendMessage(ChatColor.RED + "Número de plot inválido! Você tem " + plotCount + " plots.");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "O número do plot deve ser um valor numérico!");
                        return true;
                    }

                    // Mostrar inquilinos do plot específico
                    Plot plot = plugin.getPlotManager().getPlot(player.getUniqueId(), plotIndex);
                    Set<UUID> members = plot.getMembers();

                    if (members.isEmpty()) {
                        player.sendMessage(ChatColor.YELLOW + "Seu plot #" + (plotIndex+1) + " não possui inquilinos.");
                        return true;
                    }

                    player.sendMessage(ChatColor.YELLOW + "Inquilinos do plot #" + (plotIndex+1) + ":");
                    for (UUID memberID : members) {
                        String memberName = getMemberName(memberID);
                        player.sendMessage("- " + ChatColor.WHITE + memberName);
                    }
                } else {
                    // Listar inquilinos de todos os plots
                    player.sendMessage(ChatColor.YELLOW + "=== Inquilinos dos seus plots ===");
                    boolean hasAnyTenant = false;

                    List<Plot> plots = plugin.getPlotManager().getAllPlots(player.getUniqueId());
                    for (int i = 0; i < plots.size(); i++) {
                        Plot plot = plots.get(i);
                        Set<UUID> members = plot.getMembers();

                        if (!members.isEmpty()) {
                            hasAnyTenant = true;
                            player.sendMessage(ChatColor.GOLD + "Plot #" + (i+1) + ":");

                            for (UUID memberID : members) {
                                String memberName = getMemberName(memberID);
                                player.sendMessage("  - " + ChatColor.WHITE + memberName);
                            }
                        }
                    }

                    if (!hasAnyTenant) {
                        player.sendMessage(ChatColor.YELLOW + "Nenhum dos seus plots possui inquilinos.");
                    }
                }
            }

            return true;
        }

        return false;
    }

    private String getMemberName(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }

        // Tentar buscar do cache
        String cachedName = plugin.getConfig().getString("player-names." + uuid.toString());
        return cachedName != null ? cachedName : "Desconhecido";
    }
}