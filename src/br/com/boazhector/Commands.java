package br.com.boazhector;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

//§
public class Commands implements CommandExecutor {

    private final Main plugin;

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
            if (events.hasPermissionFly(player, loc)) {
                if (!(player.isFlying())) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    player.sendMessage("§aFly ativado!");
                } else {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.sendMessage("§cFly desativado!");
                }
            }
        }

        if (cmd.getName().equalsIgnoreCase("comprarplot")) {
            // Verificar se já tem um plot
            if (plugin.getPlotManager().hasPlot(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Você já possui um terreno!");
                return true;
            }

            // Abrir GUI de seleção de tamanho
            GuiManager.openPlotSizeSelectionGui(player);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("meuplot")) {
            // Verificar se tem um plot
            if (!plugin.getPlotManager().hasPlot(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Você ainda não possui um terreno!");
                return true;
            }

            Plot plot = plugin.getPlotManager().getPlot(player.getUniqueId());
            player.teleport(plot.getCenter());
            player.sendMessage(ChatColor.GREEN + "Teleportado para seu terreno!");
            return true;
        } else if (cmd.getName().equalsIgnoreCase("venderplot")) {
            // Verificar se tem um plot
            if (!plugin.getPlotManager().hasPlot(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Você não possui um terreno!");
                return true;
            }

            // Abrir GUI de confirmação
            GuiManager.openConfirmSellPlotGui(player);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("plotinfo")) {
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
        } else if (cmd.getName().equalsIgnoreCase("adicionarinquilino")) {
            // Verificar se o comando tem argumentos
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Uso: /adicionarinquilino <jogador>");
                return true;
            }

            // Verificar se tem um plot
            if (!plugin.getPlotManager().hasPlot(player.getUniqueId())) {
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

            Plot plot = plugin.getPlotManager().getPlot(player.getUniqueId());

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
            GuiManager.openAddTenantGui(player, target);

            return true;
        } else if (cmd.getName().equalsIgnoreCase("removerinquilino")) {
            // Verificar se o comando tem argumentos
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Uso: /removerinquilino <jogador>");
                return true;
            }

            // Verificar se tem um plot
            if (!plugin.getPlotManager().hasPlot(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Você não possui um terreno!");
                return true;
            }

            // Obter jogador alvo
            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                return true;
            }

            Plot plot = plugin.getPlotManager().getPlot(player.getUniqueId());

            // Verificar se é inquilino
            if (!plot.isMember(target.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Esse jogador não é inquilino do seu terreno.");
                return true;
            }

            // Salvar o jogador alvo e abrir GUI de confirmação
            plugin.setTargetPlayer(player, target);
            GuiManager.openRemoveTenantGui(player, target);

            return true;
        } else if (cmd.getName().equalsIgnoreCase("listarinquilinos")) {
            // Verificar se tem um plot
            if (!plugin.getPlotManager().hasPlot(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "Você não possui um terreno!");
                return true;
            }

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