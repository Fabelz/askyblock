package com.wasteofplastic.askyblock.generators;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.noise.PerlinOctaveGenerator;

import java.util.Random;

public class ChunkGeneratorWorld1_13 extends ChunkGeneratorWorld {
    private Random rand = new Random();
    private PerlinOctaveGenerator gen;
    
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        // Bukkit.getLogger().info("DEBUG: world environment = " +
        // world.getEnvironment().toString());
        if (world.getEnvironment().equals(World.Environment.NETHER)) {
            return generateNetherChunkData(world, random, chunkX, chunkZ, biomeGrid);
        }

        ChunkData chunkData = createChunkData(world);

        byte[][] result = new byte[world.getMaxHeight() / 16][];
        if (Settings.seaHeight == 0) {
            return chunkData;
        } else {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < Settings.seaHeight; y++) {
                        chunkData.setBlock(x, y, z, Material.WATER); // Stationary
                        // Water
                        // Allows stuff to fall through into oblivion, thus
                        // keeping lag to a minimum
                    }
                }
            }
            return chunkData;
        }
    }

    /*
     * Nether Section
     */
    private ChunkData generateNetherChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        // Bukkit.getLogger().info("DEBUG: world environment(nether) = " +
        // world.getEnvironment().toString());
        ChunkData chunkData = createChunkData(world);
        rand.setSeed(world.getSeed());
        gen = new PerlinOctaveGenerator((long) (random.nextLong() * random.nextGaussian()), 8);
        // This is a nether generator
        if (!world.getEnvironment().equals(World.Environment.NETHER)) {
            return chunkData;
        }
        if (Settings.netherRoof) {
            // Make the roof - common across the world
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    // Do the ceiling
                    // Bukkit.getLogger().info("debug: " + x + ", " +
                    // (world.getMaxHeight()-1) + ", " + z);
                    int maxHeight = world.getMaxHeight();
                    chunkData.setBlock(x, (maxHeight - 1), z, Material.BEDROCK);
                    // Next three layers are a mix of bedrock and netherrack
                    for (int y = 2; y < 5; y++) {
                        double r = gen.noise(x, (maxHeight - y), z, 0.5, 0.5);
                        if (r > 0D) {
                            chunkData.setBlock(x, (maxHeight - y), z, Material.BEDROCK);
                        } else {
                            chunkData.setBlock(x, (maxHeight - y), z, Material.NETHERRACK);
                        }
                    }
                    // Next three layers are a mix of netherrack and air
                    for (int y = 5; y < 8; y++) {
                        double r = gen.noise(x, (double)maxHeight - y, z, 0.5, 0.5);
                        if (r > 0D) {
                            chunkData.setBlock(x, (maxHeight - y), z, Material.NETHERRACK);
                        } else {
                            chunkData.setBlock(x, (maxHeight - y), z, Material.AIR);
                        }
                    }
                    // Layer 8 may be glowstone
                    double r = gen.noise(x, (double)maxHeight - 8, z, random.nextFloat(), random.nextFloat());
                    if (r > 0.5D) {
                        // Have blobs of glowstone
                        switch (random.nextInt(4)) {
                            case 1:
                                // Single block
                                chunkData.setBlock(x, (maxHeight - 8), z, Material.GLOWSTONE);
                                if (x < 14 && z < 14) {
                                    chunkData.setBlock(x + 1, (maxHeight - 8), z + 1, Material.GLOWSTONE);
                                    chunkData.setBlock(x + 2, (maxHeight - 8), z + 2, Material.GLOWSTONE);
                                    chunkData.setBlock(x + 1, (maxHeight - 8), z + 2, Material.GLOWSTONE);
                                    chunkData.setBlock(x + 1, (maxHeight - 8), z + 2, Material.GLOWSTONE);
                                }
                                break;
                            case 2:
                                // Stalatite
                                for (int i = 0; i < random.nextInt(10); i++) {
                                    chunkData.setBlock(x, (maxHeight - 8 - i), z, Material.GLOWSTONE);
                                }
                                break;
                            case 3:
                                chunkData.setBlock(x, (maxHeight - 8), z, Material.GLOWSTONE);
                                if (x > 3 && z > 3) {
                                    for (int xx = 0; xx < 3; xx++) {
                                        for (int zz = 0; zz < 3; zz++) {
                                            chunkData.setBlock(x - xx, (maxHeight - 8 - random.nextInt(2)), z - xx, Material.GLOWSTONE);
                                        }
                                    }
                                }
                                break;
                            default:
                                chunkData.setBlock(x, (maxHeight - 8), z, Material.GLOWSTONE);
                        }
                        chunkData.setBlock(x, (maxHeight - 8), z, Material.GLOWSTONE);
                    } else {
                        chunkData.setBlock(x, (maxHeight - 8), z, Material.AIR);
                    }
                }
            }
        }
        return chunkData;

    }
}
