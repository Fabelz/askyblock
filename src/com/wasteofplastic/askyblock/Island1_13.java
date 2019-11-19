package com.wasteofplastic.askyblock;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.UUID;

public class Island1_13 extends Island {
    public Island1_13(ASkyBlock plugin, String serial, List<String> settingsKey) {
        super(plugin, serial, settingsKey);
    }

    public Island1_13(ASkyBlock plugin, int x, int z) {
        super(plugin, x, z);
    }

    public Island1_13(ASkyBlock plugin, int x, int z, UUID owner) {
        super(plugin, x, z, owner);
    }

    public Island1_13(Island island) {
        super(island);
    }

    /**
     * @param material Bukkit material to check
     * @param world - world to check
     * @return count of how many tile entities of type mat are on the island at last count. Counts are done when a player places
     * a tile entity.
     */
    @Override
    public int getTileEntityCount(Material material, World world) {
        int result = 0;
        for (int x = getMinProtectedX() /16; x <= (getMinProtectedX() + getProtectionSize() - 1)/16; x++) {
            for (int z = getMinProtectedZ() /16; z <= (getMinProtectedZ() + getProtectionSize() - 1)/16; z++) {
                for (BlockState holder : world.getChunkAt(x, z).getTileEntities()) {
                    //plugin.getLogger().info("DEBUG: tile entity: " + holder.getType());
                    if (onIsland(holder.getLocation())) {
                        if (holder.getType() == material) {
                            result++;
                        } else if (material.name().equals("REDSTONE_COMPARATOR_OFF") || material.name().equals("COMPARATOR")) {
                            if (holder.getType().name().equals("REDSTONE_COMPARATOR_ON") || (holder.getType().name().equals("COMPARATOR") && ((Powerable) holder.getBlockData()).isPowered())) {
                                result++;
                            }
                        } else if (material.name().equals("FURNACE")) {
                            if (holder.getType().name().equals("BURNING_FURNACE") || (holder.getType().name().equals("FURNACE") && ((Lightable) holder.getBlockData()).isLit())) {
                                result++;
                            }
                        } else if (material.toString().endsWith("BANNER")) {
                            if (holder.getType().toString().endsWith("BANNER")) {
                                result++;
                            }
                        } else if (material.equals(Material.WALL_SIGN) || material.equals(Material.SIGN)) {
                            if (holder.getType().equals(Material.WALL_SIGN) || holder.getType().equals(Material.SIGN)) {
                                result++;
                            }
                        }
                    }
                }
                for (Entity holder : world.getChunkAt(x, z).getEntities()) {
                    //plugin.getLogger().info("DEBUG: entity: " + holder.getType());
                    if (holder.getType().toString().equals(material.toString()) && onIsland(holder.getLocation())) {
                        result++;
                    }
                }
            }
        }
        // Version 1.7.x counts differently to 1.8 (ugh)
        // In 1.7, the entity is present before it is cancelled and so gets counted.
        // Remove 1 from count if it is 1.7.x
        if (!plugin.isOnePointEight()) {
            result--;
        }
        return result;
    }
}
