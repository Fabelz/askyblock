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

package com.wasteofplastic.askyblock.schematics;

import java.util.HashMap;
import java.util.Map;

import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.XMaterial;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;
import com.wasteofplastic.org.jnbt.IntTag;
import com.wasteofplastic.org.jnbt.StringTag;
import com.wasteofplastic.org.jnbt.Tag;

/**
 * This class describes pots and is used in schematic importing pre-1.13.
 * 
 * @author SpyL1nk
 * 
 */
public class PotBlock {
    private Material potItem;
    private int potItemData;

    private static final Map<String, Material> potItemList;

    static {
        potItemList = new HashMap<>();
        potItemList.put("", Material.AIR);
        potItemList.put("minecraft:poppy", XMaterial.POPPY.parseMaterial());
        potItemList.put("minecraft:red_flower", XMaterial.POPPY.parseMaterial());
        potItemList.put("minecraft:dandelion", XMaterial.DANDELION.parseMaterial());
        potItemList.put("minecraft:yellow_flower", XMaterial.DANDELION.parseMaterial());
        potItemList.put("minecraft:oak_sapling", XMaterial.OAK_SAPLING.parseMaterial());
        potItemList.put("minecraft:sapling", XMaterial.OAK_SAPLING.parseMaterial());
        potItemList.put("minecraft:red_mushroom", Material.RED_MUSHROOM);
        potItemList.put("minecraft:brown_mushroom", Material.BROWN_MUSHROOM);
        potItemList.put("minecraft:cactus", Material.CACTUS);

        if (!ASkyBlock.getPlugin().isOnePointThirteen()) {
            potItemList.put("minecraft:deadbush", Material.valueOf("LONG_GRASS"));
            potItemList.put("minecraft:tallgrass", Material.valueOf("LONG_GRASS"));
        } else {
            potItemList.put("minecraft:dead_bush", Material.DEAD_BUSH);
            potItemList.put("minecraft:fern", Material.FERN);
        }
    }

    public boolean set(NMSAbstraction nms, Block block) {
        if(potItem != Material.AIR){
            nms.setFlowerPotBlock(block, new ItemStack(potItem, 1,(short) potItemData));
        }
        return true;
    }

    public boolean prep(Map<String, Tag> tileData) {
        // Initialize as default
        potItem = Material.AIR;
        potItemData = 0;
        try {
            if(tileData.containsKey("Item")){

                // Get the item in the pot
                if (tileData.get("Item") instanceof IntTag) {
                    // Item is a number, not a material
                    int id = ((IntTag) tileData.get("Item")).getValue();
                    potItem = XMaterial.matchXMaterial(id, (byte) 0).parseMaterial();
                    // Check it's a viable pot item
                    if (!potItemList.containsValue(potItem)) {
                        // No, so reset to AIR
                        potItem = Material.AIR;
                    }
                } else if (tileData.get("Item") instanceof StringTag) {
                    // Item is a material
                    String itemName = ((StringTag) tileData.get("Item")).getValue();
                    if (potItemList.containsKey(itemName)){
                        // Check it's a viable pot item
                        if (potItemList.containsKey(itemName)) {
                            potItem = potItemList.get(itemName);
                        }
                    }
                }

                if(tileData.containsKey("Data")){
                    int dataTag = ((IntTag) tileData.get("Data")).getValue();
                    // We should check data for each type of potItem 
                    if(potItem == XMaterial.POPPY.parseMaterial()){
                        if(dataTag >= 0 && dataTag <= 8){
                            potItemData = dataTag;
                        } else {
                            // Prevent hacks
                            potItemData = 0;
                        }
                    } else if(potItem == XMaterial.DANDELION.parseMaterial() ||
                            potItem == Material.RED_MUSHROOM ||
                            potItem == Material.BROWN_MUSHROOM ||
                            potItem == Material.CACTUS){
                        // Set to 0 anyway
                        potItemData = 0;
                    } else if(potItem == Material.OAK_SAPLING){
                        if(dataTag >= 0 && dataTag <= 4){
                            potItemData = dataTag;
                        } else {
                            // Prevent hacks
                            potItemData = 0;
                        }
                    } else if(potItem == Material.valueOf("LONG_GRASS")){
                        // Only 0 or 2
                        if(dataTag == 0 || dataTag == 2){
                            potItemData = dataTag;
                        } else {
                            potItemData = 0;
                        }
                    } else {
                        // ERROR ?
                        potItemData = 0;
                    }
                }
                else {
                    potItemData = 0;
                }
            }
            //Bukkit.getLogger().info("Debug: flowerpot item = " + potItem.toString());
            //Bukkit.getLogger().info("Debug: flowerpot item data = " + potItemData);
            //Bukkit.getLogger().info("Debug: flowerpot materialdata = " + new MaterialData(potItem,(byte) potItemData).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}