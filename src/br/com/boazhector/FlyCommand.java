package br.com.boazhector;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {

    private final Main plugin;

    public FlyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores!");
            return true;
        }

        Player player = (Player) sender;

        // Verificar permissões
        if (!player.hasPermission("vip.voar") &&
                !player.hasPermission("vipcosmo.perm") &&
                !player.hasPermission("viplegacy.perm") &&
                !player.hasPermission("vipastral.perm") &&
                !player.hasPermission("plotsystem.admin") &&
                player.getGameMode() != GameMode.CREATIVE &&
                player.getGameMode() != GameMode.SPECTATOR) {

            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        // Verificar se está em uma plot ou tem permissão especial
        boolean inPlot = false;
        Plot plot = plugin.getPlotManager().getPlotAt(player.getLocation());

        // Verificar se o jogador está em uma plot onde tem acesso ou tem permissão especial
        if (plot != null && plot.hasAccess(player.getUniqueId())) {
            inPlot = true;
        }

        boolean isAdmin = player.hasPermission("plotsystem.admin");
        boolean isCreative = player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;

        if (inPlot || isAdmin || isCreative) {
            // Alternar modo de voo
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage(ChatColor.RED + "Modo de voo desativado!");
            } else {
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage(ChatColor.GREEN + "Modo de voo ativado!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Você só pode usar o fly dentro da sua plot!");
        }

        return true;
    }
}