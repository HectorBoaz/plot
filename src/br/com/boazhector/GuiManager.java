package br.com.boazhector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiManager {

    private static Main plugin = Main.getInstance();

    public static void openPlotSizeSelectionGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Escolha o Tamanho do Plot");

        // Obter tamanhos e preços da configuração
        List<Integer> availableSizes = new ArrayList<>(Arrays.asList(10, 20, 30, 50, 75, 100));

        // Opcionalmente, carregar tamanhos da config
        if (plugin.getConfig().contains("plots.available-sizes")) {
            availableSizes = plugin.getConfig().getIntegerList("plots.available-sizes");
        }

        int slot = 10;
        for (int size : availableSizes) {
            double price = getPlotPrice(size);

            ItemStack item = new ItemStack(Material.GRASS_BLOCK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "" + size + "x" + size);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Tamanho: " + ChatColor.YELLOW + size + "x" + size + " blocos");
            lore.add(ChatColor.GRAY + "Preço: " + ChatColor.YELLOW + "R$" + String.format("%.2f",price));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Clique para selecionar");

            meta.setLore(lore);
            item.setItemMeta(meta);

            gui.setItem(slot, item);
            slot += 2;

            if (slot > 16) break; // Limitar a 4 opções na GUI
        }

        // Adicionar item para voltar/cancelar
        ItemStack cancelItem = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancelar");
        cancelItem.setItemMeta(cancelMeta);
        gui.setItem(22, cancelItem);

        player.openInventory(gui);
    }

    public static void openConfirmBuyPlotGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_GRAY + "Confirmar Compra de Plot");

        int size = plugin.getSelectedSize(player);
        double price = getPlotPrice(size);

        // Item de confirmação
        ItemStack confirmItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirmar");

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Tamanho: " + ChatColor.YELLOW + size + "x" + size + " blocos");
        confirmLore.add(ChatColor.GRAY + "Preço: " + ChatColor.YELLOW + "R$" + String.format("%.2f", price));
        confirmLore.add("");
        confirmLore.add(ChatColor.YELLOW + "Clique para comprar o terreno");

        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        gui.setItem(2, confirmItem);

        // Item de cancelamento
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancelar");

        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ChatColor.GRAY + "Clique para cancelar a compra");

        cancelMeta.setLore(cancelLore);
        cancelItem.setItemMeta(cancelMeta);
        gui.setItem(6, cancelItem);

        player.openInventory(gui);
    }

    public static void openConfirmSellPlotGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_GRAY + "Confirmar Venda de Plot");

        Plot plot = plugin.getPlotManager().getPlot(player.getUniqueId());
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "Você não possui um terreno!");
            return;
        }

        int size = plot.getSize();
        double refund = getPlotRefund(size);

        // Item de confirmação
        ItemStack confirmItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirmar");

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Reembolso: " + ChatColor.YELLOW + "R$" + refund);
        confirmLore.add("");
        confirmLore.add(ChatColor.YELLOW + "Clique para vender o terreno");

        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        gui.setItem(2, confirmItem);

        // Item de cancelamento
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancelar");

        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ChatColor.GRAY + "Clique para cancelar a venda");

        cancelMeta.setLore(cancelLore);
        cancelItem.setItemMeta(cancelMeta);
        gui.setItem(6, cancelItem);

        player.openInventory(gui);
    }

    public static void openAddTenantGui(Player player, Player target) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_GRAY + "Adicionar Inquilino");

        // Item de confirmação
        ItemStack confirmItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirmar");

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Jogador: " + ChatColor.YELLOW + target.getName());
        confirmLore.add("");
        confirmLore.add(ChatColor.YELLOW + "Clique para adicionar como inquilino");

        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        gui.setItem(2, confirmItem);

        // Item de cancelamento
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancelar");

        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ChatColor.GRAY + "Clique para cancelar");

        cancelMeta.setLore(cancelLore);
        cancelItem.setItemMeta(cancelMeta);
        gui.setItem(6, cancelItem);

        player.openInventory(gui);
    }

    public static void openRemoveTenantGui(Player player, Player target) {
        Inventory gui = Bukkit.createInventory(null, 9, ChatColor.DARK_GRAY + "Remover Inquilino");

        // Item de confirmação
        ItemStack confirmItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Confirmar");

        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ChatColor.GRAY + "Jogador: " + ChatColor.YELLOW + target.getName());
        confirmLore.add("");
        confirmLore.add(ChatColor.YELLOW + "Clique para remover como inquilino");

        confirmMeta.setLore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        gui.setItem(2, confirmItem);

        // Item de cancelamento
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancelar");

        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ChatColor.GRAY + "Clique para cancelar");

        cancelMeta.setLore(cancelLore);
        cancelItem.setItemMeta(cancelMeta);
        gui.setItem(6, cancelItem);

        player.openInventory(gui);
    }

    private static double getPlotPrice(int size) {
        // Preço base para o tamanho padrão (configurável)
        double basePrice = plugin.getConfig().getDouble("plots.prices.base-price", 500.0);
        int baseSize = plugin.getConfig().getInt("plots.default-size", 30);

        // Fator de multiplicação para tamanhos maiores
        double priceFactor = plugin.getConfig().getDouble("plots.prices.size-multiplier", 0.5);

        // Calcular preço com base no tamanho
        return basePrice * (1 + priceFactor * ((double)size / baseSize - 1));
    }

    private static double getPlotRefund(int size) {
        // Percentual de reembolso (configurável)
        double refundPercent = plugin.getConfig().getDouble("plots.prices.refund-percent", 60.0);

        // Calcular reembolso com base no preço
        return getPlotPrice(size) * (refundPercent / 100.0);
    }
}