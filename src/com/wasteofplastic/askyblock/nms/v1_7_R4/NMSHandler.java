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

package com.wasteofplastic.askyblock.nms.v1_7_R4;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.wasteofplastic.askyblock.XMaterial;
import com.wasteofplastic.askyblock.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.SpawnEgg;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;
import com.wasteofplastic.org.jnbt.CompoundTag;
import com.wasteofplastic.org.jnbt.ListTag;
import com.wasteofplastic.org.jnbt.StringTag;
import com.wasteofplastic.org.jnbt.Tag;

import net.minecraft.server.v1_7_R4.ChunkSection;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.NBTTagList;
import net.minecraft.server.v1_7_R4.NBTTagString;
import net.minecraft.server.v1_7_R4.TileEntityFlowerPot;

public class NMSHandler implements NMSAbstraction {

    @SuppressWarnings("deprecation")
    @Override
    public void setBlockSuperFast(Block b, int blockId, byte data, boolean applyPhysics) {
        net.minecraft.server.v1_7_R4.World w = ((CraftWorld) b.getWorld()).getHandle();
        net.minecraft.server.v1_7_R4.Chunk chunk = w.getChunkAt(b.getX() >> 4, b.getZ() >> 4);
        try {
            Field f = chunk.getClass().getDeclaredField("sections");
            f.setAccessible(true);
            ChunkSection[] sections = (ChunkSection[]) f.get(chunk);
            ChunkSection chunksection = sections[b.getY() >> 4];

            if (chunksection == null) {
                chunksection = sections[b.getY() >> 4] = new ChunkSection(b.getY() >> 4 << 4, !chunk.world.worldProvider.f);
            }
            net.minecraft.server.v1_7_R4.Block mb = net.minecraft.server.v1_7_R4.Block.getById(blockId);
            chunksection.setTypeId(b.getX() & 15, b.getY() & 15, b.getZ() & 15, mb);
            chunksection.setData(b.getX() & 15, b.getY() & 15, b.getZ() & 15, data);
            if (applyPhysics) {
                w.update(b.getX(), b.getY(), b.getZ(), mb);
            }
        } catch (Exception e) {
            //Bukkit.getLogger().info("Error");
            b.setType(XMaterial.matchXMaterial(blockId, (byte) 0).parseMaterial(), applyPhysics);
            Util.setBlockData(b, data, applyPhysics);
        }


    }

    @Override
    public void setBlockSuperFast(Block b, Material type, byte data, boolean applyPhysics) {
        setBlockSuperFast(b, type.getId(), data, applyPhysics);
    }

    @Override
    public ItemStack setBook(Tag item) {
        ItemStack chestItem = new ItemStack(Material.WRITTEN_BOOK);
        //Bukkit.getLogger().info("item data");
        //Bukkit.getLogger().info(item.toString());
        if (((CompoundTag) item).getValue().containsKey("tag")) {
            Map<String,Tag> contents = (Map<String,Tag>) ((CompoundTag) item).getValue().get("tag").getValue();
            //BookMeta bookMeta = (BookMeta) chestItem.getItemMeta();            
            String author = "";
            if (contents.containsKey("author")) {
                author = ((StringTag)contents.get("author")).getValue();
            }
            //Bukkit.getLogger().info("Author: " + author);
            //bookMeta.setAuthor(author);
            String title = "";
            if (contents.containsKey("title")) {
                title = ((StringTag)contents.get("title")).getValue();
            }
            //Bukkit.getLogger().info("Title: " + title);
            //bookMeta.setTitle(title);
            List<String> lore = new ArrayList<String>();
            if (contents.containsKey("display")) {
                Map<String,Tag> display = (Map<String, Tag>) (contents.get("display")).getValue();
                List<Tag> loreTag = ((ListTag)display.get("Lore")).getValue();
                for (Tag s: loreTag) {
                    lore.add(((StringTag)s).getValue());
                }
            }
            //Bukkit.getLogger().info("Lore: " + lore);
            net.minecraft.server.v1_7_R4.ItemStack stack = CraftItemStack.asNMSCopy(chestItem); 
            // Pages
            NBTTagCompound tag = new NBTTagCompound(); //Create the NMS Stack's NBT (item data)
            tag.setString("title", title); //Set the book's title
            tag.setString("author", author);
            if (contents.containsKey("pages")) {
                NBTTagList pages = new NBTTagList();
                List<Tag> pagesTag = ((ListTag)contents.get("pages")).getValue();
                for (Tag s: pagesTag) {
                    pages.add(new NBTTagString(((StringTag)s).getValue()));
                }
                tag.set("pages", pages); //Add the pages to the tag
            }
            stack.setTag(tag); //Apply the tag to the item
            chestItem = CraftItemStack.asCraftMirror(stack); 
            ItemMeta bookMeta = (ItemMeta) chestItem.getItemMeta();
            bookMeta.setLore(lore);
            chestItem.setItemMeta(bookMeta);
        }
        return chestItem;

    }

    /* (non-Javadoc)
     * @see com.wasteofplastic.askyblock.nms.NMSAbstraction#setBlock(org.bukkit.block.Block, org.bukkit.inventory.ItemStack)
     * Credis: Mister_Frans (THANK YOU VERY MUCH !)
     */
    @Override
    public void setFlowerPotBlock(Block block, ItemStack itemStack) {
        Location loc = block.getLocation();
        CraftWorld cw = (CraftWorld)block.getWorld();
        TileEntityFlowerPot te = (TileEntityFlowerPot)cw.getHandle().getTileEntity(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        //Bukkit.getLogger().info("Debug: flowerpot materialdata = " + (new ItemStack(potItem, 1,(short) potItemData).toString()));
        net.minecraft.server.v1_7_R4.ItemStack cis = CraftItemStack.asNMSCopy(itemStack);
        te.a(cis.getItem(), cis.getData());
        te.update();
    }

    @Override
    public boolean isPotion(ItemStack item) {
        if (item.getType().equals(Material.POTION) && item.getDurability() != 0) {
            return true;
        }
        return false;
    }
    
    @Override
    public ItemStack setPotion(Material itemMaterial, Tag itemTags,
            ItemStack chestItem) {
        // Try some backwards compatibility with new 1.9 schematics
        Map<String,Tag> cont = (Map<String,Tag>) ((CompoundTag) itemTags).getValue();
        if (cont != null) {
            if (((CompoundTag) itemTags).getValue().containsKey("tag")) {
                Map<String,Tag> contents = (Map<String,Tag>)((CompoundTag) itemTags).getValue().get("tag").getValue();
                StringTag stringTag = ((StringTag)contents.get("Potion"));
                if (stringTag != null) {
                    String tag = stringTag.getValue().replace("minecraft:", "");
                    PotionType type = null;
                    boolean strong = tag.contains("strong");
                    boolean _long = tag.contains("long");
                    //Bukkit.getLogger().info("tag = " + tag);
                    if(tag.equals("fire_resistance") || tag.equals("long_fire_resistance")){
                        type = PotionType.FIRE_RESISTANCE;
                    }else if(tag.equals("harming") || tag.equals("strong_harming")){
                        type = PotionType.INSTANT_DAMAGE;
                    }else if(tag.equals("healing") || tag.equals("strong_healing")){
                        type = PotionType.INSTANT_HEAL;
                    }else if(tag.equals("invisibility") || tag.equals("long_invisibility")){
                        type = PotionType.INVISIBILITY;
                    }else if(tag.equals("leaping") || tag.equals("long_leaping") || tag.equals("strong_leaping")){
                        type = PotionType.JUMP;
                    }else if(tag.equals("night_vision") || tag.equals("long_night_vision")){
                        type = PotionType.NIGHT_VISION;
                    }else if(tag.equals("poison") || tag.equals("long_poison") || tag.equals("strong_poison")){
                        type = PotionType.POISON;
                    }else if(tag.equals("regeneration") || tag.equals("long_regeneration") || tag.equals("strong_regeneration")){
                        type = PotionType.REGEN;
                    }else if(tag.equals("slowness") || tag.equals("long_slowness")){
                        type = PotionType.SLOWNESS;
                    }else if(tag.equals("swiftness") || tag.equals("long_swiftness") || tag.equals("strong_swiftness")){
                        type = PotionType.SPEED;
                    }else if(tag.equals("strength") || tag.equals("long_strength") || tag.equals("strong_strength")){
                        type = PotionType.STRENGTH;
                    }else if(tag.equals("water_breathing") || tag.equals("long_water_breathing")){
                        type = PotionType.WATER_BREATHING;
                    }else if(tag.equals("water")){
                        type = PotionType.WATER;
                    }else if(tag.equals("weakness") || tag.equals("long_weakness")){
                        type = PotionType.WEAKNESS;
                    }else{
                        return chestItem;
                    }
                    Potion potion = new Potion(type);
                    potion.setHasExtendedDuration(_long);
                    potion.setLevel(strong ? 2 : 1);
                    chestItem = potion.toItemStack(chestItem.getAmount());
                }
            }
        }

        return chestItem;
    }
    
    /* (non-Javadoc)
     * @see com.wasteofplastic.askyblock.nms.NMSAbstraction#getSpawnEgg(org.bukkit.entity.EntityType, int)
     */
    @Override
    public ItemStack getSpawnEgg(EntityType type, int amount) {
        SpawnEgg egg = new SpawnEgg(type);
        return egg.toItemStack(amount);
    }
}
