package br.com.boazhector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class Plot {
    private final Main plugin;
    private final UUID ownerID;
    private Location center;
    private int size;
    private int plotIndex = 0; // Índice do plot (para jogadores com múltiplos plots)
    private final HashSet<UUID> members = new HashSet<>();
    private List<Location> borderLocations = new ArrayList<>(); // Armazenar localizações em vez de blocos

    public Plot(Main plugin, UUID ownerID, Location center, int size) {
        this.plugin = plugin;
        this.ownerID = ownerID;
        this.center = center;
        this.size = size;
        createBorder();
    }

    public Plot(Main plugin, UUID ownerID) {
        this.plugin = plugin;
        this.ownerID = ownerID;
    }

    public UUID getOwnerID() {
        return ownerID;
    }

    public String getOwnerName() {
        Player player = Bukkit.getPlayer(ownerID);
        if (player != null) {
            return player.getName();
        }

        // Tentar buscar do cache
        String cachedName = plugin.getConfig().getString("player-names." + ownerID.toString());
        return cachedName != null ? cachedName : "Desconhecido";
    }

    public Location getCenter() {
        return center;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int newSize) {
        this.size = newSize;
    }

    public int getPlotIndex() {
        return plotIndex;
    }

    public void setPlotIndex(int index) {
        this.plotIndex = index;
    }

    public HashSet<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID playerID) {
        members.add(playerID);
        save(plotIndex);
    }

    public void removeMember(UUID playerID) {
        members.remove(playerID);
        save(plotIndex);
    }

    public boolean isMember(UUID playerID) {
        return members.contains(playerID);
    }

    public boolean hasAccess(UUID playerID) {
        return ownerID.equals(playerID) || members.contains(playerID);
    }

    public boolean isInside(Location location) {
        if (!location.getWorld().equals(center.getWorld())) {
            return false;
        }

        int halfSize = size / 2;
        double x = location.getX();
        double z = location.getZ();

        return x >= center.getX() - halfSize &&
                x <= center.getX() + halfSize &&
                z >= center.getZ() - halfSize &&
                z <= center.getZ() + halfSize;
    }

    public boolean overlaps(Location otherCenter, int otherSize) {
        if (!otherCenter.getWorld().equals(center.getWorld())) {
            return false;
        }

        int halfSize = size / 2;
        int otherHalfSize = otherSize / 2;

        double x1 = center.getX() - halfSize;
        double x2 = center.getX() + halfSize;
        double z1 = center.getZ() - halfSize;
        double z2 = center.getZ() + halfSize;

        double ox1 = otherCenter.getX() - otherHalfSize;
        double ox2 = otherCenter.getX() + otherHalfSize;
        double oz1 = otherCenter.getZ() - otherHalfSize;
        double oz2 = otherCenter.getZ() + otherHalfSize;

        return x1 <= ox2 && x2 >= ox1 && z1 <= oz2 && z2 >= oz1;
    }

    public boolean overlapsWithNewSize(Plot otherPlot, int newSize) {
        if (!otherPlot.getCenter().getWorld().equals(center.getWorld())) {
            return false;
        }

        int halfSize = newSize / 2;
        int otherHalfSize = otherPlot.getSize() / 2;

        double x1 = center.getX() - halfSize;
        double x2 = center.getX() + halfSize;
        double z1 = center.getZ() - halfSize;
        double z2 = center.getZ() + halfSize;

        double ox1 = otherPlot.getCenter().getX() - otherHalfSize;
        double ox2 = otherPlot.getCenter().getX() + otherHalfSize;
        double oz1 = otherPlot.getCenter().getZ() - otherHalfSize;
        double oz2 = otherPlot.getCenter().getZ() + otherHalfSize;

        return x1 <= ox2 && x2 >= ox1 && z1 <= oz2 && z2 >= oz1;
    }

    public void createBorder() {
        removeBorder(); // Limpa qualquer borda existente primeiro

        World world = center.getWorld();
        int halfSize = size / 2;
        int x1 = center.getBlockX() - halfSize;
        int x2 = center.getBlockX() + halfSize;
        int z1 = center.getBlockZ() - halfSize;
        int z2 = center.getBlockZ() + halfSize;

        Material fenceMaterial = Material.OAK_FENCE;

        // Material das cercas
        String fenceType = plugin.getConfig().getString("plots.border-material", "OAK_FENCE");
        try {
            fenceMaterial = Material.valueOf(fenceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Material de cerca inválido: " + fenceType + ", usando padrão OAK_FENCE");
        }

        // Criar cercas ao redor do perímetro
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                // Colocar cercas apenas no perímetro
                if (x == x1 || x == x2 || z == z1 || z == z2) {
                    // Encontrar o bloco mais alto no local
                    int highestY = findSuitableY(world, x, z);

                    Location borderLoc = new Location(world, x, highestY, z);
                    borderLocations.add(borderLoc); // Armazenar localização

                    Block block = world.getBlockAt(borderLoc);
                    if (block.getType() == Material.AIR) {
                        block.setType(fenceMaterial);
                    }
                }
            }
        }

        // Salvar as localizações das bordas no config
        saveBorderLocations();
    }

    // Salvar as localizações das bordas no config para uso posterior
    private void saveBorderLocations() {
        List<String> locations = new ArrayList<>();

        for (Location loc : borderLocations) {
            String locString = loc.getWorld().getName() + ";" +
                    loc.getBlockX() + ";" +
                    loc.getBlockY() + ";" +
                    loc.getBlockZ();
            locations.add(locString);
        }

        String path;
        if (plotIndex >= 0) {
            path = "plots.data." + ownerID.toString() + "." + plotIndex + ".borders";
        } else {
            path = "plots.data." + ownerID.toString() + ".borders";
        }

        plugin.getConfig().set(path, locations);
        plugin.saveConfig();
    }

    // Carregar as localizações das bordas do config
    private void loadBorderLocations() {
        String path;
        if (plotIndex >= 0) {
            path = "plots.data." + ownerID.toString() + "." + plotIndex + ".borders";
        } else {
            path = "plots.data." + ownerID.toString() + ".borders";
        }

        List<String> locations = plugin.getConfig().getStringList(path);
        borderLocations.clear();

        if (locations != null && !locations.isEmpty()) {
            for (String locString : locations) {
                String[] parts = locString.split(";");
                if (parts.length == 4) {
                    World world = Bukkit.getWorld(parts[0]);
                    if (world != null) {
                        try {
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            int z = Integer.parseInt(parts[3]);
                            borderLocations.add(new Location(world, x, y, z));
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("Formato inválido para localização de borda: " + locString);
                        }
                    }
                }
            }
        }
    }

    private int findSuitableY(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);

        // Ajustar se for água ou outro bloco transparente
        Block highestBlock = world.getBlockAt(x, y, z);
        if (highestBlock.getType() == Material.WATER || highestBlock.getType() == Material.LAVA) {
            return y;
        }

        return y + 1; // Colocar a cerca um bloco acima
    }

    public void removeBorder() {
        // Primeiro, tentar carregar as localizações salvas se ainda não carregamos
        if (borderLocations.isEmpty()) {
            loadBorderLocations();
        }

        // Se ainda estiver vazio, não há nada para remover
        if (borderLocations.isEmpty()) {
            return;
        }

        // Remover todas as cercas nas localizações salvas
        for (Location loc : borderLocations) {
            Block block = loc.getBlock();
            if (block.getType().name().contains("FENCE")) {
                block.setType(Material.AIR);
            }
        }

        // Limpar a lista
        borderLocations.clear();

        // Também remover do config
        String path;
        if (plotIndex >= 0) {
            path = "plots.data." + ownerID.toString() + "." + plotIndex + ".borders";
        } else {
            path = "plots.data." + ownerID.toString() + ".borders";
        }

        plugin.getConfig().set(path, null);
        plugin.saveConfig();
    }

    // Verificar se as cercas da borda existem e recriá-las somente se estiverem faltando
    public void checkAndUpdateBorder() {
        if (borderLocations.isEmpty()) {
            loadBorderLocations();
        }

        // Se não houver localizações salvas, criamos as cercas
        if (borderLocations.isEmpty()) {
            createBorder();
            return;
        }

        // Não fazer nada - manter as cercas como estão
        // Se uma cerca foi quebrada ou substituída por outro bloco, não tentamos recriá-la
        // Se um jogador colocou um bloco sobre ou no lugar de uma cerca, respeitamos isso
    }

    public void save() {
        save(0); // Compatibilidade com formato antigo
    }

    public void save(int index) {
        String path;
        if (index >= 0) {
            // Formato novo com índice para múltiplos plots
            path = "plots.data." + ownerID.toString() + "." + index;
        } else {
            // Formato antigo (único plot)
            path = "plots.data." + ownerID.toString();
        }

        ConfigurationSection plotSection = plugin.getConfig().createSection(path);
        plotSection.set("world", center.getWorld().getName());
        plotSection.set("x", center.getX());
        plotSection.set("y", center.getY());
        plotSection.set("z", center.getZ());
        plotSection.set("size", size);

        List<String> membersList = new ArrayList<>();
        for (UUID member : members) {
            membersList.add(member.toString());
        }
        plotSection.set("members", membersList);

        // Salvar as localizações das bordas
        saveBorderLocations();

        plugin.saveConfig();
    }

    public void load() {
        load(-1); // Carrega no formato antigo
    }

    public void load(int index) {
        ConfigurationSection plotSection;

        if (index >= 0) {
            // Formato novo (múltiplos plots)
            plotSection = plugin.getConfig().getConfigurationSection("plots.data." + ownerID.toString() + "." + index);
            this.plotIndex = index;
        } else {
            // Formato antigo (único plot)
            plotSection = plugin.getConfig().getConfigurationSection("plots.data." + ownerID.toString());
            this.plotIndex = 0;
        }

        if (plotSection == null) {
            return;
        }

        String worldName = plotSection.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Mundo não encontrado: " + worldName);
            return;
        }

        double x = plotSection.getDouble("x");
        double y = plotSection.getDouble("y");
        double z = plotSection.getDouble("z");

        this.center = new Location(world, x, y, z);
        this.size = plotSection.getInt("size");

        List<String> membersList = plotSection.getStringList("members");
        for (String memberStr : membersList) {
            try {
                UUID memberID = UUID.fromString(memberStr);
                members.add(memberID);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("ID de membro inválido: " + memberStr);
            }
        }

        // Carregar as localizações das bordas
        loadBorderLocations();

        // Verificar as cercas e atualizar apenas se necessário
        checkAndUpdateBorder();
    }
}