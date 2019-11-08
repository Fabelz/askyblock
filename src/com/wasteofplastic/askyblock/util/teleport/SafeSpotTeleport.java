package com.wasteofplastic.askyblock.util.teleport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.wasteofplastic.askyblock.XMaterial;
import org.bukkit.ChunkSnapshot;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Island;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.util.Pair;

/**
 * A class that calculates finds a safe spot asynchronously and then teleports the player there.
 * @author tastybento
 *
 */
public class SafeSpotTeleport {

    private static final int MAX_CHUNKS = 200;
    private static final long SPEED = 1;
    private static final int MAX_RADIUS = 200;
    private boolean checking = true;
    private BukkitTask task;

    // Parameters
    private final Entity entity;
    private final Location location;
    private boolean portal;
    private final int homeNumber;

    // Locations
    private Location bestSpot;


    private ASkyBlock plugin;
    private List<Pair<Integer, Integer>> chunksToScan;

    /**
     * Teleports and entity to a safe spot on island
     * @param plugin - ASkyBlock plugin object
     * @param entity
     * @param location
     * @param failureMessage - already translated failure message
     * @param portal
     * @param homeNumber
     */
    protected SafeSpotTeleport(ASkyBlock plugin, final Entity entity, final Location location, final String failureMessage, boolean portal,
            int homeNumber) {
        this.plugin = plugin;
        this.entity = entity;
        this.location = location;
        this.portal = portal;
        this.homeNumber = homeNumber;

        // Put player into spectator mode
        if (!plugin.getServer().getVersion().contains("1.7") && entity instanceof Player && ((Player)entity).getGameMode().equals(GameMode.SURVIVAL)) {
            ((Player)entity).setGameMode(GameMode.SPECTATOR);
        }

        // Get chunks to scan
        chunksToScan = getChunksToScan();

        // Start checking
        checking = true;

        // Start a recurring task until done or cancelled
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {

            @Override
            public void run() {
                List<ChunkSnapshot> chunkSnapshot = new ArrayList<>();
                if (checking) {
                    Iterator<Pair<Integer, Integer>> it = chunksToScan.iterator();
                    if (!it.hasNext()) {
                        // Nothing left
                        tidyUp(entity, failureMessage);
                        return;
                    }
                    // Add chunk snapshots to the list
                    while (it.hasNext() && chunkSnapshot.size() < MAX_CHUNKS) {
                        Pair<Integer, Integer> pair = it.next();
                        chunkSnapshot.add(location.getWorld().getChunkAt(pair.x, pair.z).getChunkSnapshot());
                        it.remove();
                    }
                    // Move to next step
                    checking = false;
                    checkChunks(chunkSnapshot);
                }

            }
        }, 0L, SPEED);
    }

    private void tidyUp(Entity entity, String failureMessage) {
        // Nothing left to check and still not canceled
        task.cancel();
        // Check portal
        if (portal && bestSpot != null) {
            // No portals found, teleport to the best spot we found
            teleportEntity(bestSpot);
        } else if (entity instanceof Player && !failureMessage.isEmpty()) {
            // Failed, no safe spot
            entity.sendMessage(failureMessage);
        }
        if (entity instanceof Player && (plugin.getServer().getVersion().contains("1.7") || ((Player)entity).getGameMode().equals(GameMode.SPECTATOR))) {
            ((Player)entity).setGameMode(GameMode.SURVIVAL);
        }
        if (entity instanceof Player) {
            plugin.getPlayers().setInTeleport(entity.getUniqueId(), false);
        }
    }

    /**
     * Gets a set of chunk coords that will be scanned.
     * @return
     */
    private List<Pair<Integer, Integer>> getChunksToScan() {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        // Get island if available
        Island island = plugin.getGrid().getIslandAt(location);
        int maxRadius = island == null ? Settings.islandProtectionRange/2 : island.getProtectionSize()/2;
        maxRadius = maxRadius > MAX_RADIUS ? MAX_RADIUS : maxRadius;

        int x = location.getBlockX();
        int z = location.getBlockZ();
        // Create ever increasing squares around the target location
        int radius = 0;
        do {
            for (int i = x - radius; i <= x + radius; i+=16) {
                for (int j = z - radius; j <= z + radius; j+=16) {
                    addChunk(result, island, new Pair<>(i,j), new Pair<>(i/16, j/16));
                }
            }
            radius++;
        } while (radius < maxRadius);
        return result;
    }

    private void addChunk(List<Pair<Integer, Integer>> result, Island island, Pair<Integer, Integer> blockCoord, Pair<Integer, Integer> chunkCoord) {
        if (!result.contains(chunkCoord)) {
            // Add the chunk coord
            if (island == null) {
                // If there is no island, just add it
                result.add(chunkCoord);
            } else {
                // If there is an island, only add it if the coord is in island space
                if (island.inIslandSpace(blockCoord.x, blockCoord.z)) {
                    result.add(chunkCoord);
                }
            }
        }
    }

    /**
     * Loops through the chunks and if a safe spot is found, fires off the teleportation
     * @param chunkSnapshot
     */
    private void checkChunks(final List<ChunkSnapshot> chunkSnapshot) {
        // Run async task to scan chunks
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                for (ChunkSnapshot chunk: chunkSnapshot) {
                    if (scanChunk(chunk)) {
                        task.cancel();
                        return;
                    }
                }
                // Nothing happened, change state
                checking = true;
            }

        });
    }


    /**
     * @param chunk
     * @return true if a safe spot was found
     */
    private boolean scanChunk(ChunkSnapshot chunk) {
        // Max height
        int maxHeight = location.getWorld().getMaxHeight() - 20;
        // Run through the chunk
        for (int x = 0; x< 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Work down from the entry point up
                for (int y = Math.min(chunk.getHighestBlockYAt(x, z), maxHeight); y >= 0; y--) {
                    if (checkBlock(chunk, x,y,z, maxHeight)) {
                        return true;
                    }
                } // end y
            } //end z
        } // end x
        return false;
    }

    /**
     * Teleports entity to the safe spot
     */
    private void teleportEntity(final Location loc) {
        task.cancel();
        // Return to main thread and teleport the player
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!portal && entity instanceof Player) {
                    // Set home
                    plugin.getPlayers().setHomeLocation(entity.getUniqueId(), loc, homeNumber);
                }
                Vector velocity = entity.getVelocity();
                entity.teleport(loc);
                // Exit spectator mode if in it
                if (entity instanceof Player) {
                    Player player = (Player)entity;
                    if (plugin.getServer().getVersion().contains("1.7")) {
                        player.setGameMode(GameMode.SURVIVAL);
                    } else if (player.getGameMode().equals(GameMode.SPECTATOR)) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                } else {
                    entity.setVelocity(velocity);
                }

            }
        });

    }

    /**
     * Returns true if the location is a safe one.
     * @param chunk
     * @param x
     * @param y
     * @param z
     * @param worldHeight
     * @return true if this is a safe spot, false if this is a portal scan
     */
    @SuppressWarnings("deprecation")
    private boolean checkBlock(ChunkSnapshot chunk, int x, int y, int z, int worldHeight) {
        World world = location.getWorld();
        Material type = chunk.getBlockType(x, y, z);
        if (!type.equals(Material.AIR)) { // AIR
            Material space1 = chunk.getBlockType(x, Math.min(y + 1, worldHeight), z);
            Material space2 = chunk.getBlockType(x, Math.min(y + 2, worldHeight), z);
            if ((space1.equals(Material.AIR) && space2.equals(Material.AIR)) || (space1.equals(XMaterial.NETHER_PORTAL.parseMaterial()) && space2.equals(XMaterial.NETHER_PORTAL.parseMaterial()))
                    && (!type.toString().contains("FENCE") && !type.toString().contains("DOOR") && !type.toString().contains("GATE") && !type.toString().contains("PLATE"))) {
                switch (type.name()) {
                // Unsafe
                case "ACACIA_BOAT":
                case "ACACIA_BUTTON":
                case "ANVIL":
                case "BARRIER":
                case "BIRCH_BOAT":
                case "BIRCH_BUTTON":
                case "BLACK_BANNER":
                case "BLACK_WALL_BANNER":
                case "BLUE_BANNER":
                case "BLUE_WALL_BANNER":
                case "BOAT":
                case "BROWN_BANNER":
                case "BROWN_WALL_BANNER":
                case "CACTUS":
                case "COBWEB":
                case "CREEPER_HEAD":
                case "CREEPER_WALL_HEAD":
                case "CYAN_BANNER":
                case "CYAN_WALL_BANNER":
                case "DARK_OAK_BOAT":
                case "DARK_OAK_BUTTON":
                case "DOUBLE_PLANT":
                case "DRAGON_HEAD":
                case "DRAGON_WALL_HEAD":
                case "ENDER_PORTAL":
                case "END_PORTAL":
                case "FIRE":
                case "FLOWER_POT":
                case "GRAY_BANNER":
                case "GRAY_WALL_BANNER":
                case "GREEN_BANNER":
                case "GREEN_WALL_BANNER":
                case "JUNGLE_BOAT":
                case "JUNGLE_BUTTON":
                case "LADDER":
                case "LAVA":
                case "LEVER":
                case "LIGHT_BLUE_BANNER":
                case "LIGHT_BLUE_WALL_BANNER":
                case "LIGHT_GRAY_BANNER":
                case "LIGHT_GRAY_WALL_BANNER":
                case "LILAC":
                case "LIME_BANNER":
                case "LIME_WALL_BANNER":
                case "LONG_GRASS":
                case "MAGENTA_BANNER":
                case "MAGENTA_WALL_BANNER":
                case "MOVING_PISTON":
                case "OAK_BOAT":
                case "OAK_BUTTON":
                case "ORANGE_BANNER":
                case "ORANGE_WALL_BANNER":
                case "PEONY":
                case "PINK_BANNER":
                case "PINK_WALL_BANNER":
                case "PISTON_EXTENSION":
                case "PISTON_HEAD":
                case "PISTON_MOVING_PIECE":
                case "PLAYER_HEAD":
                case "PLAYER_WALL_HEAD":
                case "PURPLE_BANNER":
                case "PURPLE_WALL_BANNER":
                case "RED_BANNER":
                case "RED_WALL_BANNER":
                case "ROSE_BUSH":
                case "SIGN":
                case "SIGN_POST":
                case "SKELETON_SKULL":
                case "SKELETON_WALL_SKULL":
                case "SKULL":
                case "SPRUCE_BOAT":
                case "SPRUCE_BUTTON":
                case "STANDING_BANNER":
                case "STATIONARY_LAVA":
                case "STATIONARY_WATER":
                case "STONE_BUTTON":
                case "SUNFLOWER":
                case "TALL_GRASS":
                case "TORCH":
                case "TRIPWIRE":
                case "WALL_SIGN":
                case "WATER":
                case "WEB":
                case "WHITE_BANNER":
                case "WHITE_WALL_BANNER":
                case "WITHER_SKELETON_SKULL":
                case "WITHER_SKELETON_WALL_SKULL":
                case "WOOD_BUTTON":
                case "YELLOW_BANNER":
                case "YELLOW_WALL_BANNER":
                case "ZOMBIE_HEAD":
                case "ZOMBIE_WALL_HEAD":
                //Block is dangerous
                break;
                case "NETHER_PORTAL":
                case "PORTAL":
                if (portal) {
                    // A portal has been found, switch to non-portal mode now
                    portal = false;
                }
                break;
                default:
                    return safe(chunk, x, y, z, world);
                }
            }
        }
        return false;
    }

    private boolean safe(ChunkSnapshot chunk, int x, int y, int z, World world) {
        Vector newSpot = new Vector(chunk.getX() * 16 + x + 0.5D, y + 1, chunk.getZ() * 16 + z + 0.5D);
        if (portal) {
            if (bestSpot == null) {
                // Stash the best spot
                bestSpot = newSpot.toLocation(world);
            }
            return false;
        } else {
            teleportEntity(newSpot.toLocation(world));
            return true;
        }
    }




}