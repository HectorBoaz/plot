package br.com.boazhector;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin {

    private static Main instance;
    public static Main m;
    private PlotManager plotManager;
    private final HashMap<UUID, UUID> targetPlayerMap = new HashMap<>();
    private final HashMap<UUID, Integer> selectedSizeMap = new HashMap<>();
    private final HashMap<UUID, Integer> selectedPlotIndexMap = new HashMap<>(); // Índice do plot selecionado

    // Método para recarregar o plugin
    public void reload() {
        reloadConfig();
        plotManager.savePlots();
        plotManager.loadPlots();
    }

    @Override
    public void onEnable() {
        m = this;
        instance = this;

        // Salvar config padrão se não existir
        saveDefaultConfig();

        // Inicializar o gerenciador de plots
        plotManager = new PlotManager(this);

        // Registrar comandos regulares
        getCommand("comprarplot").setExecutor(new Commands(this));
        getCommand("meuplot").setExecutor(new Commands(this));
        getCommand("venderplot").setExecutor(new Commands(this));
        getCommand("plotinfo").setExecutor(new Commands(this));
        getCommand("adicionarinquilino").setExecutor(new Commands(this));
        getCommand("removerinquilino").setExecutor(new Commands(this));
        getCommand("listarinquilinos").setExecutor(new Commands(this));
        getCommand("meusplots").setExecutor(new Commands(this));
        getCommand("irplot").setExecutor(new Commands(this));

        // Registrar comando fly como um executor separado
        getCommand("fly").setExecutor(new FlyCommand(this));

        // Comandos de administrador
        getCommand("delplot").setExecutor(new CommandsAdmin(this));
        getCommand("plotadmin").setExecutor(new CommandsAdmin(this));

        // Registrar eventos
        getServer().getPluginManager().registerEvents(new Events(), this);

        // Carregar dados
        plotManager.loadPlots();

        getLogger().info("PlotSystem ativado com sucesso!");
    }

    @Override
    public void onDisable() {
        // Salvar dados
        if (plotManager != null) {
            plotManager.savePlots();
        }

        getLogger().info("PlotSystem desativado com sucesso!");
    }

    public static Main getInstance() {
        return instance;
    }

    public PlotManager getPlotManager() {
        return plotManager;
    }

    public void setTargetPlayer(Player player, Player target) {
        targetPlayerMap.put(player.getUniqueId(), target.getUniqueId());
    }

    public UUID getTargetPlayer(Player player) {
        return targetPlayerMap.get(player.getUniqueId());
    }

    public void removeTargetPlayer(Player player) {
        targetPlayerMap.remove(player.getUniqueId());
    }

    public void setSelectedSize(Player player, int size) {
        selectedSizeMap.put(player.getUniqueId(), size);
    }

    public int getSelectedSize(Player player) {
        return selectedSizeMap.getOrDefault(player.getUniqueId(), getConfig().getInt("plots.default-size", 30));
    }

    public void removeSelectedSize(Player player) {
        selectedSizeMap.remove(player.getUniqueId());
    }

    public void setSelectedPlotIndex(Player player, int index) {
        selectedPlotIndexMap.put(player.getUniqueId(), index);
    }

    public int getSelectedPlotIndex(Player player) {
        return selectedPlotIndexMap.getOrDefault(player.getUniqueId(), 0);
    }

    public void removeSelectedPlotIndex(Player player) {
        selectedPlotIndexMap.remove(player.getUniqueId());
    }

    // Obtém o número máximo de plots permitidos para um jogador
    public int getMaxPlots(Player player) {
        if (player.hasPermission("plotsystem.admin")) {
            return Integer.MAX_VALUE; // ilimitado para OPs
        } else if (player.hasPermission("vipastral.perm")) {
            return 5; // Astral VIP - 5 plots
        } else if (player.hasPermission("viplegacy.perm")) {
            return 3; // Legacy VIP - 3 plots
        } else if (player.hasPermission("vipcosmo.perm")) {
            return 2; // Cosmo VIP - 2 plots
        } else {
            return 1; // Jogador normal - 1 plot
        }
    }

    // Classe para gerenciar plots
    public class PlotManager {
        private final Main plugin;
        private final HashMap<UUID, List<Plot>> playerPlots = new HashMap<>();

        public PlotManager(Main plugin) {
            this.plugin = plugin;
        }

        public Set<UUID> getAllPlotOwners() {
            return playerPlots.keySet();
        }

        public void savePlots() {
            // Limpar dados antigos
            plugin.getConfig().set("plots.data", null);

            // Implementação para salvar plots no config
            for (UUID playerId : playerPlots.keySet()) {
                List<Plot> plots = playerPlots.get(playerId);
                for (int i = 0; i < plots.size(); i++) {
                    plots.get(i).save(i);
                }
            }
            plugin.saveConfig();
        }

        public void loadPlots() {
            playerPlots.clear();

            // Implementação para carregar plots do config
            FileConfiguration config = plugin.getConfig();
            if (config.contains("plots.data")) {
                for (String key : config.getConfigurationSection("plots.data").getKeys(false)) {
                    try {
                        UUID ownerID = UUID.fromString(key);
                        List<Plot> plots = new ArrayList<>();

                        // Verificar se há plots múltiplos para este jogador
                        if (config.contains("plots.data." + key + ".0")) {
                            // Formato novo - múltiplos plots
                            for (int i = 0; config.contains("plots.data." + key + "." + i); i++) {
                                Plot plot = new Plot(plugin, ownerID);
                                plot.load(i);
                                plots.add(plot);
                            }
                        } else {
                            // Formato antigo - único plot
                            Plot plot = new Plot(plugin, ownerID);
                            plot.load(-1);
                            plots.add(plot);
                        }

                        playerPlots.put(ownerID, plots);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("ID de plot inválido: " + key);
                    }
                }
            }
        }

        public boolean hasPlot(UUID playerID) {
            return playerPlots.containsKey(playerID) && !playerPlots.get(playerID).isEmpty();
        }

        public int getPlotCount(UUID playerID) {
            if (!playerPlots.containsKey(playerID)) {
                return 0;
            }
            return playerPlots.get(playerID).size();
        }

        public Plot getPlot(UUID playerID) {
            // Retorna o primeiro plot (compatibilidade)
            if (!hasPlot(playerID)) return null;
            return playerPlots.get(playerID).get(0);
        }

        public Plot getPlot(UUID playerID, int index) {
            if (!hasPlot(playerID)) return null;
            List<Plot> plots = playerPlots.get(playerID);
            if (index < 0 || index >= plots.size()) return null;
            return plots.get(index);
        }

        public List<Plot> getAllPlots(UUID playerID) {
            if (!playerPlots.containsKey(playerID)) {
                return new ArrayList<>();
            }
            return playerPlots.get(playerID);
        }

        public void addPlot(UUID playerID, Plot plot) {
            List<Plot> plots = playerPlots.getOrDefault(playerID, new ArrayList<>());
            plots.add(plot);
            playerPlots.put(playerID, plots);
        }

        public void removePlot(UUID playerID, int index) {
            if (!hasPlot(playerID)) return;

            List<Plot> plots = playerPlots.get(playerID);
            if (index < 0 || index >= plots.size()) return;

            Plot plot = plots.get(index);
            plot.removeBorder();

            plots.remove(index);

            // Se ficar sem plots, remover do mapa
            if (plots.isEmpty()) {
                playerPlots.remove(playerID);
                plugin.getConfig().set("plots.data." + playerID.toString(), null);
            } else {
                // Reordenar os plots no config
                plugin.getConfig().set("plots.data." + playerID.toString(), null);
                for (int i = 0; i < plots.size(); i++) {
                    plots.get(i).save(i);
                }
            }
            plugin.saveConfig();
        }

        public void removePlot(UUID playerID) {
            // Remove o primeiro plot (compatibilidade)
            removePlot(playerID, 0);
        }

        public Plot getPlotAt(org.bukkit.Location location) {
            for (List<Plot> plotList : playerPlots.values()) {
                for (Plot plot : plotList) {
                    if (plot.isInside(location)) {
                        return plot;
                    }
                }
            }
            return null;
        }

        public boolean canCreatePlot(org.bukkit.Location center, int size) {
            for (List<Plot> plotList : playerPlots.values()) {
                for (Plot plot : plotList) {
                    if (plot.overlaps(center, size)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}