/*******************************************************************************
 * This file is part of ASkyBlock.
 *
 *     ASkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ASkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with ASkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.wasteofplastic.askyblock.generators;

import java.util.Random;

import com.wasteofplastic.askyblock.XMaterial;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author tastybento
 *         Populates the Nether with appropriate blocks
 * 
 */
public class NetherPopulator extends BlockPopulator {

    @Override
    public void populate(World world, Random random, Chunk source) {
        // Rough check - convert spawners to Nether spawners
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < world.getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    Block b = source.getBlock(x, y, z);
                    if (XMaterial.matchXMaterial(b.getType().name()) == XMaterial.SPAWNER) {
                        CreatureSpawner cs = (CreatureSpawner) b.getState();
                        switch (random.nextInt(3)) {
                        case 0:
                            cs.setSpawnedType(EntityType.BLAZE);
                            break;
                        case 1:
                            cs.setSpawnedType(EntityType.SKELETON);
                            break;
                        case 2:
                            cs.setSpawnedType(EntityType.MAGMA_CUBE);
                            break;
                        default:
                            cs.setSpawnedType(EntityType.BLAZE);
                        }
                    } else if (b.getType().equals(Material.OBSIDIAN)) {
                        b.setType(Material.CHEST);
                        Chest cs = (Chest) b.getState();
                        Inventory chestInv = cs.getInventory();
                        // Fill it with random goodies
                        /*
                         * 2 to 5 stacks of any of the following
                         * Diamonds 1 - 3 6.85% (5/73)
                         * Iron Ingots 1 - 5 6.85% (5/73)
                         * Gold Ingots 1 - 3 20.5% (15/73)
                         * Golden Sword 1 6.85% (5/73)
                         * Golden Chestplate 1 6.85% (5/73)
                         * Flint and Steel 1 6.85% (5/73)
                         * Nether Wart 3 - 7 6.85% (5/73)
                         * Saddle 1 13.7% (10/73)
                         * Golden Horse Armor 1 11.0% (8/73)
                         * Iron Horse Armor 1 6.85% (5/73)
                         * Diamond Horse Armor 1 4.11% (3/73)
                         * Obsidian 2 - 4 2.74% (2/73)
                         */
                        // Pick how many stacks
                        int numOfStacks = 2 + random.nextInt(3);
                        // Pick the stacks
                        for (int i = 0; i < numOfStacks; i++) {
                            // Pick a random inventory slot
                            int slot = random.nextInt(chestInv.getSize());
                            // Try a few times to find an empty slot (avoids an
                            // infinite loop potential)
                            for (int j = 0; j < chestInv.getSize(); j++) {
                                if (chestInv.getItem(slot) == null) {
                                    break;
                                }
                                slot = random.nextInt(chestInv.getSize());
                            }
                            int choice = random.nextInt(73);
                            if (choice < 5) {
                                chestInv.setItem(slot, new ItemStack(Material.DIAMOND, random.nextInt(2) + 1));
                            } else if (choice < 10) {
                                chestInv.setItem(slot, new ItemStack(Material.IRON_INGOT, random.nextInt(4) + 1));
                            } else if (choice < 25) {
                                chestInv.setItem(slot, new ItemStack(Material.GOLD_INGOT, random.nextInt(2) + 1));
                            } else if (choice < 30) {
                                chestInv.setItem(slot, XMaterial.GOLDEN_SWORD.parseItem());
                            } else if (choice < 35) {
                                chestInv.setItem(slot, XMaterial.GOLDEN_CHESTPLATE.parseItem());
                            } else if (choice < 40) {
                                chestInv.setItem(slot, new ItemStack(Material.FLINT_AND_STEEL, 1));
                            } else if (choice < 45) {
                                chestInv.setItem(slot, XMaterial.NETHER_WART.parseItem());
                            } else if (choice < 55) {
                                chestInv.setItem(slot, new ItemStack(Material.SADDLE, 1));
                            } else if (choice < 63) {
                                chestInv.setItem(slot, XMaterial.GOLDEN_HORSE_ARMOR.parseItem());
                            } else if (choice < 68) {
                                chestInv.setItem(slot, XMaterial.IRON_HORSE_ARMOR.parseItem());
                            } else if (choice < 71) {
                                chestInv.setItem(slot, XMaterial.DIAMOND_HORSE_ARMOR.parseItem());
                            } else {
                                chestInv.setItem(slot, new ItemStack(Material.OBSIDIAN, random.nextInt(3) + 1));
                            }
                        }

                    } else if (b.getType().equals(Material.STONE)) {
                        b.setType(XMaterial.NETHER_QUARTZ_ORE.parseMaterial());
                    } else if (b.getType().equals(Material.DIRT)) {
                        world.generateTree(source.getBlock(x, y + 1, z).getLocation(), TreeType.BROWN_MUSHROOM);
                        b.setType(Material.SOUL_SAND);
                    } else if (b.getType().equals(Material.SOUL_SAND) && b.getRelative(BlockFace.UP).getType().equals(Material.AIR)) {
                        //Bukkit.getLogger().info("DEBUG: soul sand found!");
                        if (random.nextInt(9) == 1) {
                            //Bukkit.getLogger().info("DEBUG: Setting to NETHER_WARTS");
                            b.getRelative(BlockFace.UP).setType(XMaterial.NETHER_WART_BLOCK.parseMaterial());
                        }
                    }
                    // Mob spawn
                    /*
                     * if (y == Settings.island_level &&
                     * b.getType().equals(Material.NETHERRACK)) {
                     * Entity e =
                     * world.spawnEntity(b.getRelative(BlockFace.UP,1)
                     * .getLocation(), EntityType.PIG_ZOMBIE);
                     * Bukkit.getLogger().info(e.toString());
                     * //}
                     * }
                     */
                }
            }
        }
    }

}
