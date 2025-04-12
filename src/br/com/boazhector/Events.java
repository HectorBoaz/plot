package br.com.boazhector;

import net.byebye.balance.BalanceAPI;
import net.byebye.balance.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class Events implements Listener {

    Economy eco = BalanceAPI.getEconomy();

    private final HashMap<UUID, Long> lastNotificationMap = new HashMap<>();
    private final long NOTIFICATION_COOLDOWN = 3000; // 3 segundos em milissegundos


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Salvar nome do jogador para referência futura
        Main.m.getConfig().set("player-names." + player.getUniqueId().toString(), player.getName());
        Main.m.saveConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Verificar se o jogador tem permissão para quebrar
        if (!hasPermissionInPlot(player, block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Você não tem permissão para quebrar blocos aqui!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Verificar se o jogador tem permissão para construir
        if (!hasPermissionInPlot(player, block.getLocation())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Você não tem permissão para colocar blocos aqui!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled() || event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        // Verificar se o bloco é um contêiner (baú, fornalha, etc.)
        if (block.getState() instanceof Container) {
            // Verificar se o jogador tem permissão para interagir
            if (!hasPermissionInPlot(player, block.getLocation())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você não tem permissão para acessar este contêiner!");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Verificar se o jogador realmente se moveu de um bloco para outro
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getTo();
        Block block = event.getTo().getBlock();

        // Verificar se o jogador entrou em um terreno
        Plot plot = Main.m.getPlotManager().getPlotAt(location);
        if (plot != null) {
            // Verificar se não é o proprietário do terreno
            if (!hasPermissionInPlot(player, block.getLocation())) {
                if (!plot.getOwnerID().equals(player.getUniqueId())) {
                    // Verificar cooldown para não spammar mensagens
                    long now = System.currentTimeMillis();
                    Long lastNotification = lastNotificationMap.get(player.getUniqueId());

                    if (lastNotification == null || now - lastNotification > NOTIFICATION_COOLDOWN) {
                        player.sendMessage(ChatColor.YELLOW + "Terreno de: " + ChatColor.GREEN + plot.getOwnerName());
                        lastNotificationMap.put(player.getUniqueId(), now);
                    }
                }
            }
        } else {
            if (player.getAllowFlight()){
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage("§cVocê saiu da sua plot, o Fly Foi desativado automaticamente!");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (event.getView().getTitle().contains("Plot")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }
            // Lidar com cliques na GUI de tamanhos de plots
            if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "Escolha o Tamanho do Plot")) {
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

                    if (displayName.contains("x")) {
                        String sizeStr = displayName.split("x")[0].trim();
                        try {
                            int size = Integer.parseInt(sizeStr);
                            Main.m.setSelectedSize(player, size);

                            // Abrir GUI de confirmação
                            player.closeInventory();
                            GuiManager.openConfirmBuyPlotGui(player);
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Tamanho inválido!");
                        }
                    }
                }
            }
            // Lidar com cliques na GUI de confirmação de compra
            else if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "Confirmar Compra de Plot")) {
                if (item.getType() == Material.GREEN_WOOL) {
                    player.closeInventory();
                    int size = Main.m.getSelectedSize(player);
                    double price = getPlotPrice(size);

                    if (eco.getSaldo(player) >= price) {
                        createPlotForPlayer(player, size, price);
                    } else {
                        player.sendMessage(ChatColor.RED + "Você não tem dinheiro suficiente! Preço: "
                                + ChatColor.YELLOW + "R$" + String.format("%.2f", price));
                    }

                    Main.m.removeSelectedSize(player);
                } else if (item.getType() == Material.RED_WOOL) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.RED + "Compra de terreno cancelada.");
                    Main.m.removeSelectedSize(player);
                }
            }
            // Lidar com cliques na GUI de venda de plot
            else if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "Confirmar Venda de Plot")) {
                if (item.getType() == Material.GREEN_WOOL) {
                    player.closeInventory();
                    sellPlot(player);
                } else if (item.getType() == Material.RED_WOOL) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.RED + "Venda de terreno cancelada.");
                }
            }
        }
        if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "Adicionar Inquilino")) {
            event.setCancelled(true);
            if (item.getType() == Material.GREEN_WOOL) {
                player.closeInventory();
                UUID targetId = Main.m.getTargetPlayer(player);
                if (targetId != null) {
                    Player target = Main.m.getServer().getPlayer(targetId);
                    if (target != null) {
                        addTenant(player, target);
                    } else {
                        player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                    }
                }
                Main.m.removeTargetPlayer(player);
            } else if (item.getType() == Material.RED_WOOL) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "Adição de inquilino cancelada.");
                Main.m.removeTargetPlayer(player);
            }
        } else if (event.getView().getTitle().equals(ChatColor.DARK_GRAY + "Remover Inquilino")) {
            event.setCancelled(true);
            if (item.getType() == Material.GREEN_WOOL) {
                player.closeInventory();
                UUID targetId = Main.m.getTargetPlayer(player);
                if (targetId != null) {
                    Player target = Main.m.getServer().getPlayer(targetId);
                    if (target != null) {
                        removeTenant(player, target);
                    } else {
                        player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                    }
                }
                Main.m.removeTargetPlayer(player);
            } else if (item.getType() == Material.RED_WOOL) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + "Remoção de inquilino cancelada.");
                Main.m.removeTargetPlayer(player);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        // Verificar se o pistão atravessa diferentes plots
        for (Block block : event.getBlocks()) {
            Plot plot1 = Main.m.getPlotManager().getPlotAt(block.getLocation());
            Plot plot2 = Main.m.getPlotManager().getPlotAt(block.getRelative(event.getDirection()).getLocation());

            // Se um dos blocos estiver em um plot e o outro em outro (ou fora), cancela
            if (plot1 != plot2) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;

        // Verificar se o pistão atravessa diferentes plots
        for (Block block : event.getBlocks()) {
            Plot plot1 = Main.m.getPlotManager().getPlotAt(block.getLocation());
            Plot plot2 = Main.m.getPlotManager().getPlotAt(block.getRelative(event.getDirection().getOppositeFace()).getLocation());

            // Se um dos blocos estiver em um plot e o outro em outro (ou fora), cancela
            if (plot1 != plot2) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public boolean hasPermissionInPlot(Player player, Location location) {
        // Verificar se o jogador tem permissão de admin
        if (player.hasPermission("plotsystem.admin")) {
            return true;
        }

        Plot plot = Main.m.getPlotManager().getPlotAt(location);
        if (plot == null) {
            // Se não estiver em nenhum plot, tem permissão
            return true;
        }

        // Verificar se é o dono ou inquilino
        return plot.hasAccess(player.getUniqueId());
    }

    public boolean hasPermissionFly(Player player, Location location) {
        // Verificar se o jogador tem permissão de admin
        if (player.hasPermission("plotsystem.admin")) {
            return true;
        }

        Plot plot = Main.m.getPlotManager().getPlotAt(location);
        if (plot == null) {
            // Se não estiver em nenhum plot, tem permissão
            return false;
        }

        // Verificar se é o dono ou inquilino
        return plot.hasAccess(player.getUniqueId());
    }

    private double getPlotPrice(int size) {
        // Preço base para o tamanho padrão (configurável)
        double basePrice = Main.m.getConfig().getDouble("plots.prices.base-price", 500.0);
        int baseSize = Main.m.getConfig().getInt("plots.default-size", 30);

        // Fator de multiplicação para tamanhos maiores
        double priceFactor = Main.m.getConfig().getDouble("plots.prices.size-multiplier", 0.5);

        // Calcular preço com base no tamanho
        return basePrice * (1 + priceFactor * ((double) size / baseSize - 1));
    }

    private double getPlotRefund(int size) {
        // Percentual de reembolso (configurável)
        double refundPercent = Main.m.getConfig().getDouble("plots.prices.refund-percent", 60.0);

        // Calcular reembolso com base no preço
        return getPlotPrice(size) * (refundPercent / 100.0);
    }

    private void createPlotForPlayer(Player player, int size, double price) {
        // Verificar se já tem um plot
        if (Main.m.getPlotManager().hasPlot(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Você já possui um terreno!");
            return;
        }

        // Verificar se a localização é válida (não sobrepõe outros plots)
        Location location = player.getLocation();
        if (!Main.m.getPlotManager().canCreatePlot(location, size)) {
            player.sendMessage(ChatColor.RED + "Este local está muito próximo ou sobrepõe outro terreno!");
            return;
        }

        // Cobrar o jogador
        eco.removeSaldo(player, price);

        // Criar o plot
        Plot plot = new Plot(Main.m, player.getUniqueId(), location, size);
        Main.m.getPlotManager().addPlot(player.getUniqueId(), plot);

        // Salvar o plot
        plot.save();
        plot.createBorder();
        player.sendMessage(ChatColor.GREEN + "Terreno comprado com sucesso! Preço: " + ChatColor.YELLOW + "R$" + String.format("%.2f", price));
        player.sendMessage(ChatColor.GRAY + "Use /meuplot para teleportar até ele.");
    }

    private void sellPlot(Player player) {
        // Verificar se tem um plot
        if (!Main.m.getPlotManager().hasPlot(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Você não possui um terreno!");
            return;
        }

        Plot plot = Main.m.getPlotManager().getPlot(player.getUniqueId());
        double refund = getPlotRefund(plot.getSize());

        // Remover o plot
        Main.m.getPlotManager().removePlot(player.getUniqueId());

        // Reembolsar o jogador
        eco.addSaldo(player, refund);

        player.sendMessage(ChatColor.GREEN + "Você vendeu seu terreno por " + ChatColor.YELLOW + "R$" + String.format("%.2f", refund) + ".");
    }

    private void addTenant(Player owner, Player tenant) {
        // Verificar se o dono tem um plot
        if (!Main.m.getPlotManager().hasPlot(owner.getUniqueId())) {
            owner.sendMessage(ChatColor.RED + "Você não possui um terreno!");
            return;
        }

        // Verificar se o inquilino já tem um plot
        if (Main.m.getPlotManager().hasPlot(tenant.getUniqueId())) {
            owner.sendMessage(ChatColor.RED + "Esse jogador já possui um terreno!");
            return;
        }

        Plot plot = Main.m.getPlotManager().getPlot(owner.getUniqueId());

        // Verificar limite de inquilinos
        int maxTenants = Main.m.getConfig().getInt("plots.max-tenants", 3);
        if (plot.getMembers().size() >= maxTenants) {
            owner.sendMessage(ChatColor.RED + "Você atingiu o limite máximo de inquilinos (" + maxTenants + ").");
            return;
        }

        // Adicionar inquilino
        plot.addMember(tenant.getUniqueId());

        owner.sendMessage(ChatColor.GREEN + "Você adicionou " + tenant.getName() + " como inquilino do seu terreno.");
        owner.sendMessage(ChatColor.YELLOW + "Agora ele pode construir, quebrar blocos e abrir baús.");
        tenant.sendMessage(ChatColor.GREEN + "Você foi adicionado como inquilino no terreno de " + owner.getName() + ".");
    }

    private void removeTenant(Player owner, Player tenant) {
        // Verificar se o dono tem um plot
        if (!Main.m.getPlotManager().hasPlot(owner.getUniqueId())) {
            owner.sendMessage(ChatColor.RED + "Você não possui um terreno!");
            return;
        }

        Plot plot = Main.m.getPlotManager().getPlot(owner.getUniqueId());

        // Verificar se o jogador é inquilino
        if (!plot.isMember(tenant.getUniqueId())) {
            owner.sendMessage(ChatColor.RED + "Esse jogador não é inquilino do seu terreno.");
            return;
        }

        // Remover inquilino
        plot.removeMember(tenant.getUniqueId());

        owner.sendMessage(ChatColor.GREEN + "Você removeu " + tenant.getName() + " do seu terreno.");
        tenant.sendMessage(ChatColor.RED + "Você perdeu acesso ao terreno de " + owner.getName() + ".");
    }
}