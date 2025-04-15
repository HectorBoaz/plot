package br.com.boazhector;

import net.byebye.balance.BalanceAPI;
import net.byebye.balance.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Events implements Listener {

    Economy eco = BalanceAPI.getEconomy();
    private static final Map<UUID, BukkitTask> teleportePendente = new HashMap<>();

    private final HashMap<UUID, Long> lastNotificationMap = new HashMap<>();
    private final HashMap<UUID, Long> flyDisabledMap = new HashMap<>();
    private final long NOTIFICATION_COOLDOWN = 3000000;
    private final long FLY_PROTECTION_DURATION = 10000;

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

        // Se o bloco quebrado for uma cerca, verifique se é parte de uma borda de plot
        if (block.getType().name().contains("FENCE")) {
            // Verificar se está em algum plot
            Plot plot = Main.m.getPlotManager().getPlotAt(block.getLocation());
            if (plot != null) {
                // Se a cerca quebrada faz parte da borda, não precisamos fazer nada especial
                // pois já salvamos todas as localizações de bordas e podemos recriá-las quando necessário

                // Verificar se o jogador tem permissão para quebrar
                if (!hasPermissionInPlot(player, block.getLocation())) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Você não tem permissão para quebrar blocos aqui!");
                }
            }
        } else {
            // Para blocos que não são cercas, verificar normalmente
            if (!hasPermissionInPlot(player, block.getLocation())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Você não tem permissão para quebrar blocos aqui!");
            }
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

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!hasPermissionInPlot(player, block.getLocation())) {
                event.setCancelled(true);
                return;
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

        if (teleportePendente.containsKey(player.getUniqueId())) {
            if (event.getFrom().distanceSquared(event.getTo()) > 0.1) {
                teleportePendente.get(player.getUniqueId()).cancel();
                teleportePendente.remove(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "Você se mexeu! Teleporte cancelado.");
            }
        }

        if (player.hasPermission("plotsystem.admin") || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return; // Não restringir voo para admins ou jogadores em modos especiais
        }

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
            if (player.getAllowFlight() && hasVipFlyPermission(player) && !player.hasPermission("plotsystem.admin")) {
                // Desativar voo
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage("§cVocê saiu da sua plot, o Fly Foi desativado automaticamente!");

                // Marcar o momento em que o fly foi desativado para proteção contra queda
                flyDisabledMap.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    // Verificar se o jogador tem permissão de VIP para voar
    private boolean hasVipFlyPermission(Player player) {
        return player.hasPermission("vip.voar") ||
                player.hasPermission("vipcosmo.perm") ||
                player.hasPermission("viplegacy.perm") ||
                player.hasPermission("vipastral.perm");
    }


    // Prevenir dano de queda após o voo ser desativado
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        Player player = (Player) event.getEntity();
        Location location = (Location) player.getLocation();
        Plot plot = Main.m.getPlotManager().getPlotAt(location);

        if(plot != null){
            if (hasPermissionInPlot(player, location)) {
                event.setCancelled(true);
            }
        }

        if (teleportePendente.containsKey(player.getUniqueId())) {
            teleportePendente.get(player.getUniqueId()).cancel();
            teleportePendente.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "Você foi atingido! Teleporte cancelado.");
        }


        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        Long flyDisabledTime = flyDisabledMap.get(player.getUniqueId());

        if (flyDisabledTime != null) {
            long now = System.currentTimeMillis();
            if (now - flyDisabledTime <= FLY_PROTECTION_DURATION) {
                event.setCancelled(true);
                // Se desejar, remova o jogador da lista depois que a proteção for usada
                if (now - flyDisabledTime > 5000) { // Remover após 5 segundos
                    flyDisabledMap.remove(player.getUniqueId());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (item == null || item.getType() == Material.AIR) return;

        String title = event.getView().getTitle();

        if (title.contains("Plot")) {
            event.setCancelled(true);

            // GUI de seleção de tamanho
            if (title.equals(ChatColor.DARK_GRAY + "Escolha o Tamanho do Plot")) {
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

            // GUI de confirmação de compra
            else if (title.equals(ChatColor.DARK_GRAY + "Confirmar Compra de Plot")) {
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

            // GUI de seleção de plot (para teleporte ou operações)
            else if (title.equals(ChatColor.DARK_GRAY + "Seus Plots") ||
                    title.equals(ChatColor.DARK_GRAY + "Selecione um Plot para Vender") ||
                    title.equals(ChatColor.DARK_GRAY + "Selecione um Plot para Adicionar Inquilino") ||
                    title.equals(ChatColor.DARK_GRAY + "Selecione um Plot para Remover Inquilino")) {

                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                    if (displayName.startsWith("Plot #")) {
                        try {
                            int plotIndex = Integer.parseInt(displayName.substring(6).trim()) - 1;

                            if (title.equals(ChatColor.DARK_GRAY + "Seus Plots")) {
                                // Teleportar para o plot selecionado
                                Plot plot = Main.m.getPlotManager().getPlot(player.getUniqueId(), plotIndex);
                                if (plot != null) {
                                    player.closeInventory();
                                    player.sendMessage(ChatColor.YELLOW + "Fique parado por 5 segundos para ser teleportado...");

                                    Location localAtual = player.getLocation(); // para checar se ele se move

                                    BukkitTask task = Bukkit.getScheduler().runTaskLater(Main.m, () -> {
                                        Location agora = player.getLocation();
                                        if (agora.distanceSquared(localAtual) <= 0.1) {
                                            player.teleport(plot.getCenter());
                                            player.sendMessage(ChatColor.GREEN + "Teleportado para seu plot #" + (plotIndex + 1) + "!");
                                        } else {
                                            player.sendMessage(ChatColor.RED + "Você se mexeu! Teleporte cancelado.");
                                        }
                                        teleportePendente.remove(player.getUniqueId());
                                    }, 20L * 5); // 5 segundos

                                    teleportePendente.put(player.getUniqueId(), task);
                                }
                            } else if (title.equals(ChatColor.DARK_GRAY + "Selecione um Plot para Vender")) {
                                // Confirmar venda do plot selecionado
                                player.closeInventory();
                                Main.m.setSelectedPlotIndex(player, plotIndex);
                                GuiManager.openConfirmSellPlotGui(player, plotIndex);
                            } else if (title.equals(ChatColor.DARK_GRAY + "Selecione um Plot para Adicionar Inquilino")) {
                                // Adicionar inquilino ao plot selecionado
                                player.closeInventory();
                                Main.m.setSelectedPlotIndex(player, plotIndex);
                                UUID targetId = Main.m.getTargetPlayer(player);
                                if (targetId != null) {
                                    Player target = Main.m.getServer().getPlayer(targetId);
                                    if (target != null) {
                                        GuiManager.openAddTenantGui(player, target);
                                    }
                                }
                            } else if (title.equals(ChatColor.DARK_GRAY + "Selecione um Plot para Remover Inquilino")) {
                                // Remover inquilino do plot selecionado
                                player.closeInventory();
                                Main.m.setSelectedPlotIndex(player, plotIndex);
                                UUID targetId = Main.m.getTargetPlayer(player);
                                if (targetId != null) {
                                    Player target = Main.m.getServer().getPlayer(targetId);
                                    if (target != null) {
                                        GuiManager.openRemoveTenantGui(player, target);
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "Erro ao processar seleção de plot!");
                        }
                    }
                }
            }


            // GUI de confirmação de venda
            else if (title.equals(ChatColor.DARK_GRAY + "Confirmar Venda de Plot")) {
                if (item.getType() == Material.GREEN_WOOL) {
                    player.closeInventory();
                    int plotIndex = Main.m.getSelectedPlotIndex(player);
                    sellPlot(player, plotIndex);
                    Main.m.removeSelectedPlotIndex(player);
                } else if (item.getType() == Material.RED_WOOL) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.RED + "Venda de terreno cancelada.");
                    Main.m.removeSelectedPlotIndex(player);
                }
            }

            // GUI de adicionar inquilino
            else if (title.equals(ChatColor.DARK_GRAY + "Adicionar Inquilino")) {
                if (item.getType() == Material.GREEN_WOOL) {
                    player.closeInventory();
                    UUID targetId = Main.m.getTargetPlayer(player);
                    int plotIndex = Main.m.getSelectedPlotIndex(player);

                    if (targetId != null) {
                        Player target = Main.m.getServer().getPlayer(targetId);
                        if (target != null) {
                            addTenant(player, target, plotIndex);
                        } else {
                            player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                        }
                    }
                    Main.m.removeTargetPlayer(player);
                    Main.m.removeSelectedPlotIndex(player);
                } else if (item.getType() == Material.RED_WOOL) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.RED + "Adição de inquilino cancelada.");
                    Main.m.removeTargetPlayer(player);
                    Main.m.removeSelectedPlotIndex(player);
                }
            }

            // GUI de remover inquilino
            else if (title.equals(ChatColor.DARK_GRAY + "Remover Inquilino")) {
                if (item.getType() == Material.GREEN_WOOL) {
                    player.closeInventory();
                    UUID targetId = Main.m.getTargetPlayer(player);
                    int plotIndex = Main.m.getSelectedPlotIndex(player);

                    if (targetId != null) {
                        Player target = Main.m.getServer().getPlayer(targetId);
                        if (target != null) {
                            removeTenant(player, target, plotIndex);
                        } else {
                            player.sendMessage(ChatColor.RED + "Jogador não encontrado!");
                        }
                    }
                    Main.m.removeTargetPlayer(player);
                    Main.m.removeSelectedPlotIndex(player);
                } else if (item.getType() == Material.RED_WOOL) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.RED + "Remoção de inquilino cancelada.");
                    Main.m.removeTargetPlayer(player);
                    Main.m.removeSelectedPlotIndex(player);
                }
            }
        }
    }


    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Location loc = event.getLocation();
        Plot plot = Main.m.getPlotManager().getPlotAt(loc);

        if (plot == null) {
            event.setCancelled(false); // Permitir explosões fora de plots
        } else {
            event.setCancelled(true); // Bloquear explosões dentro dos plots
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Location loc = event.getBlock().getLocation();
        Plot plot = Main.m.getPlotManager().getPlotAt(loc);

        if (plot == null) {
            event.setCancelled(false); // Permitir explosões fora de plots
        } else {
            event.setCancelled(true); // Bloquear explosões dentro dos plots
        }
    }


    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Location loc = event.getBlock().getLocation();
        Plot plot = Main.m.getPlotManager().getPlotAt(loc);

        if (plot == null) {
            event.setCancelled(false); // Permitir queima fora dos plots
        } else {
            event.setCancelled(true); // Impedir queima dentro dos plots
        }
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
        // Verificar se o jogador tem permissão de admin ou está em modo criativo/espectador
        if (player.hasPermission("plotsystem.admin") || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }

        Plot plot = Main.m.getPlotManager().getPlotAt(location);
        if (plot == null) {
            // Se não estiver em nenhum plot, não tem permissão de voo
            return false;
        }

        // Verificar se é o dono ou inquilino e se tem permissão vip.voar
        return plot.hasAccess(player.getUniqueId()) && hasVipFlyPermission(player);
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
        // Verificar limite de plots
        int maxPlots = Main.m.getMaxPlots(player);
        int currentPlots = Main.m.getPlotManager().getPlotCount(player.getUniqueId());

        if (currentPlots >= maxPlots) {
            player.sendMessage(ChatColor.RED + "Você atingiu seu limite de " + maxPlots + " plots!");
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
        plot.save(currentPlots); // Salvar com o índice correto
        plot.createBorder();

        player.sendMessage(ChatColor.GREEN + "Terreno comprado com sucesso! Preço: " + ChatColor.YELLOW + "R$" + String.format("%.2f", price));
        player.sendMessage(ChatColor.GRAY + "Use /meuplot para teleportar até ele.");

        // Informar sobre limites se o jogador está se aproximando do limite
        if (currentPlots + 1 == maxPlots && !player.hasPermission("plotsystem.admin")) {
            player.sendMessage(ChatColor.GOLD + "Você atingiu seu limite máximo de plots! (" + maxPlots + "/" + maxPlots + ")");
        } else if (currentPlots + 1 < maxPlots && !player.hasPermission("plotsystem.admin")) {
            player.sendMessage(ChatColor.GOLD + "Você tem agora " + (currentPlots + 1) + " plots de " + maxPlots + " permitidos.");
        }
    }

    private void sellPlot(Player player, int plotIndex) {
        // Verificar se tem um plot no índice especificado
        if (Main.m.getPlotManager().getPlotCount(player.getUniqueId()) <= plotIndex) {
            player.sendMessage(ChatColor.RED + "Você não possui um plot com esse número!");
            return;
        }

        Plot plot = Main.m.getPlotManager().getPlot(player.getUniqueId(), plotIndex);
        double refund = getPlotRefund(plot.getSize());

        // Remover o plot
        Main.m.getPlotManager().removePlot(player.getUniqueId(), plotIndex);

        // Reembolsar o jogador
        eco.addSaldo(player, refund);

        player.sendMessage(ChatColor.GREEN + "Você vendeu seu plot #" + (plotIndex + 1) + " por " +
                ChatColor.YELLOW + "R$" + String.format("%.2f", refund) + ".");
    }

    private void addTenant(Player owner, Player tenant, int plotIndex) {
        // Verificar se o dono tem um plot no índice especificado
        if (Main.m.getPlotManager().getPlotCount(owner.getUniqueId()) <= plotIndex) {
            owner.sendMessage(ChatColor.RED + "Você não possui um plot com esse número!");
            return;
        }

        // Verificar se o inquilino já tem um plot
        if (Main.m.getPlotManager().hasPlot(tenant.getUniqueId())) {
            owner.sendMessage(ChatColor.RED + "Esse jogador já possui um terreno!");
            return;
        }

        Plot plot = Main.m.getPlotManager().getPlot(owner.getUniqueId(), plotIndex);

        // Verificar limite de inquilinos
        int maxTenants = Main.m.getConfig().getInt("plots.max-tenants", 3);
        if (plot.getMembers().size() >= maxTenants) {
            owner.sendMessage(ChatColor.RED + "Você atingiu o limite máximo de inquilinos (" + maxTenants + ").");
            return;
        }

        // Adicionar inquilino
        plot.addMember(tenant.getUniqueId());

        owner.sendMessage(ChatColor.GREEN + "Você adicionou " + tenant.getName() + " como inquilino do seu plot #" + (plotIndex + 1) + ".");
        owner.sendMessage(ChatColor.YELLOW + "Agora ele pode construir, quebrar blocos e abrir baús.");
        tenant.sendMessage(ChatColor.GREEN + "Você foi adicionado como inquilino no plot #" + (plotIndex + 1) + " de " + owner.getName() + ".");
    }

    private void removeTenant(Player owner, Player tenant, int plotIndex) {
        // Verificar se o dono tem um plot no índice especificado
        if (Main.m.getPlotManager().getPlotCount(owner.getUniqueId()) <= plotIndex) {
            owner.sendMessage(ChatColor.RED + "Você não possui um plot com esse número!");
            return;
        }

        Plot plot = Main.m.getPlotManager().getPlot(owner.getUniqueId(), plotIndex);

        // Verificar se o jogador é inquilino
        if (!plot.isMember(tenant.getUniqueId())) {
            owner.sendMessage(ChatColor.RED + "Esse jogador não é inquilino do seu plot #" + (plotIndex + 1) + ".");
            return;
        }

        // Remover inquilino
        plot.removeMember(tenant.getUniqueId());

        owner.sendMessage(ChatColor.GREEN + "Você removeu " + tenant.getName() + " do seu plot #" + (plotIndex + 1) + ".");
        tenant.sendMessage(ChatColor.RED + "Você perdeu acesso ao plot #" + (plotIndex + 1) + " de " + owner.getName() + ".");
    }
}