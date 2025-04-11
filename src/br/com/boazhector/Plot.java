package br.com.boazhector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.server.BroadcastMessageEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class Plot {
    private final Main plugin;
    private final UUID ownerID;
    private Location center;
    private int size;
    private final HashSet<UUID> members = new HashSet<>();
    private final List<Block> borderBlocks = new ArrayList<>();

    public Plot(Main plugin, UUID ownerID, Location center, int size) {
        this.plugin = plugin;
        this.ownerID = ownerID;
        this.center = center;
        this.size = size;
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

    public HashSet<UUID> getMembers() {
        return members;
    }

    public void addMember(UUID playerID) {
        members.add(playerID);
        save();
    }

    public void removeMember(UUID playerID) {
        members.remove(playerID);
        save();
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
        int y = center.getBlockY();

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

                    Block block = world.getBlockAt(x, highestY, z);
                    if (block.getType() == Material.AIR) {
                        block.setType(fenceMaterial);
                        borderBlocks.add(block);
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
        for (Block block : borderBlocks) {
            if (block.getType().equals(Material.OAK_FENCE)) {
                block.setType(Material.AIR);
            }
        }
        borderBlocks.clear();
    }

    public void save() {
        ConfigurationSection plotSection = plugin.getConfig().createSection("plots.data." + ownerID.toString());
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

        plugin.saveConfig();
    }

    public void load() {
        ConfigurationSection plotSection = plugin.getConfig().getConfigurationSection("plots.data." + ownerID.toString());
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
    }
}