package br.com.boazhector;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class Main extends JavaPlugin {

    private static Main instance;
    public static Main m;
    private PlotManager plotManager;
    private final HashMap<UUID, UUID> targetPlayerMap = new HashMap<>();
    private final HashMap<UUID, Integer> selectedSizeMap = new HashMap<>();

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

        // Registrar comandos
        getCommand("comprarplot").setExecutor(new Commands(this));
        getCommand("meuplot").setExecutor(new Commands(this));
        getCommand("venderplot").setExecutor(new Commands(this));
        getCommand("plotinfo").setExecutor(new Commands(this));
        getCommand("adicionarinquilino").setExecutor(new Commands(this));
        getCommand("removerinquilino").setExecutor(new Commands(this));
        getCommand("listarinquilinos").setExecutor(new Commands(this));

        getCommand("fly").setExecutor(new Commands(this));

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

    // Classe para gerenciar plots
    public class PlotManager {
        private final Main plugin;
        private final HashMap<UUID, Plot> plots = new HashMap<>();

        public PlotManager(Main plugin) {
            this.plugin = plugin;
        }

        public Set<UUID> getAllPlotOwners() {
            return plots.keySet();
        }

        public void savePlots() {
            // Implementação para salvar plots no config
            for (Plot plot : plots.values()) {
                plot.save();
            }
        }

        public void loadPlots() {
            // Implementação para carregar plots do config
            FileConfiguration config = plugin.getConfig();
            if (config.contains("plots.data")) {
                for (String key : config.getConfigurationSection("plots.data").getKeys(false)) {
                    try {
                        UUID ownerID = UUID.fromString(key);
                        Plot plot = new Plot(plugin, ownerID);
                        plot.load();
                        plots.put(ownerID, plot);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("ID de plot inválido: " + key);
                    }
                }
            }
        }

        public boolean hasPlot(UUID playerID) {
            return plots.containsKey(playerID);
        }

        public Plot getPlot(UUID playerID) {
            return plots.get(playerID);
        }

        public void addPlot(UUID playerID, Plot plot) {
            plots.put(playerID, plot);
        }

        public void removePlot(UUID playerID) {
            Plot plot = plots.remove(playerID);
            if (plot != null) {
                plot.removeBorder();
                // Remover do config
                plugin.getConfig().set("plots.data." + playerID.toString(), null);
                plugin.saveConfig();
            }
        }

        public Plot getPlotAt(org.bukkit.Location location) {
            for (Plot plot : plots.values()) {
                if (plot.isInside(location)) {
                    return plot;
                }
            }
            return null;
        }

        public boolean canCreatePlot(org.bukkit.Location center, int size) {
            for (Plot plot : plots.values()) {
                if (plot.overlaps(center, size)) {
                    return false;
                }
            }
            return true;
        }
    }
}