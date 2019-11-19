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

package com.wasteofplastic.askyblock.nms.v1_13_R2;

import com.wasteofplastic.askyblock.nms.NMSAbstraction;
import com.wasteofplastic.org.jnbt.CompoundTag;
import com.wasteofplastic.org.jnbt.ListTag;
import com.wasteofplastic.org.jnbt.StringTag;
import com.wasteofplastic.org.jnbt.Tag;
import net.minecraft.server.v1_13_R2.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NMSHandler implements NMSAbstraction {

    private static HashMap<EntityType, String> bToMConversion;
    private Method setTypeMethod;

    static {
        bToMConversion = new HashMap<EntityType, String> ();
        bToMConversion.put(EntityType.MUSHROOM_COW, "mooshroom");
        bToMConversion.put(EntityType.PIG_ZOMBIE, "zombie_pigman");
    }

    public NMSHandler() {

    }

    @Override
    public void setBlockSuperFast(Block b, int blockId, byte data, boolean applyPhysics) {
        World w = ((CraftWorld) b.getWorld()).getHandle();
        Chunk chunk = w.getChunkAt(b.getX() >> 4, b.getZ() >> 4);
        BlockPosition bp = new BlockPosition(b.getX(), b.getY(), b.getZ());
        int combined = blockId + (data << 12);
        IBlockData ibd = net.minecraft.server.v1_13_R2.Block.getByCombinedId(combined);
        if (applyPhysics) {
            w.setTypeAndData(bp, ibd, 3); 
        } else {
            w.setTypeAndData(bp, ibd, 2); 
        }
        setInChunk(chunk, bp, ibd);
    }

    @Override
    public void setBlockSuperFast(Block b, Material type, byte data, boolean applyPhysics) {
        World w = ((CraftWorld) b.getWorld()).getHandle();
        Chunk chunk = w.getChunkAt(b.getX() >> 4, b.getZ() >> 4);
        BlockPosition bp = new BlockPosition(b.getX(), b.getY(), b.getZ());
        IBlockData ibd = IRegistry.BLOCK.getOrDefault(new MinecraftKey(type.toString().toLowerCase())).getBlockData();
        if (applyPhysics) {
            w.setTypeAndData(bp, ibd, 3);
        } else {
            w.setTypeAndData(bp, ibd, 2);
        }
        setInChunk(chunk, bp, ibd);
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
            net.minecraft.server.v1_13_R2.ItemStack stack = CraftItemStack.asNMSCopy(chestItem); 
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
     */
    @Override
    public void setFlowerPotBlock(final Block block, final ItemStack itemStack) {
        // Doesn't use NMS anymore, but ohwell. :shrug:
        if (block.getType().name().startsWith("POTTED_") || block.getType().equals(Material.FLOWER_POT)) {
            block.setType(Material.valueOf("POTTED_" + itemStack.getType().name()));
        }
    }

    /* (non-Javadoc)
     * @see com.wasteofplastic.askyblock.nms.NMSAbstraction#isPotion(org.bukkit.inventory.ItemStack)
     */
    @Override
    public boolean isPotion(ItemStack item) {
        //Bukkit.getLogger().info("DEBUG:item = " + item);
        if (item.getType().equals(Material.POTION)) {
            net.minecraft.server.v1_13_R2.ItemStack stack = CraftItemStack.asNMSCopy(item);
            NBTTagCompound tag = stack.getTag();
            //Bukkit.getLogger().info("DEBUG: tag is " + tag);
            //Bukkit.getLogger().info("DEBUG: display is " + tag.getString("display"));
            /*
            for (String list : tag.c()) {
                Bukkit.getLogger().info("DEBUG: list = " + list);
            }*/
            if (tag != null && (!tag.getString("Potion").equalsIgnoreCase("minecraft:water") || tag.getString("Potion").isEmpty())) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.wasteofplastic.acidisland.nms.NMSAbstraction#setPotion(com.wasteofplastic.org.jnbt.Tag)
     */
    @SuppressWarnings({ "unchecked"})
    @Override
    public ItemStack setPotion(Material material, Tag itemTags, ItemStack chestItem) {
        Map<String,Tag> cont = (Map<String,Tag>) ((CompoundTag) itemTags).getValue();
        if (cont != null) {
            if (((CompoundTag) itemTags).getValue().containsKey("tag")) {
                Map<String,Tag> contents = (Map<String,Tag>)((CompoundTag) itemTags).getValue().get("tag").getValue();
                StringTag stringTag = ((StringTag)contents.get("Potion"));
                if (stringTag != null) {
                    String tag = ((StringTag)contents.get("Potion")).getValue();
                    //Bukkit.getLogger().info("DEBUG: potioninfo found: " + tag);
                    net.minecraft.server.v1_13_R2.ItemStack stack = CraftItemStack.asNMSCopy(chestItem);
                    NBTTagCompound tagCompound = stack.getTag();
                    if(tagCompound == null){
                        tagCompound = new NBTTagCompound();
                    }
                    tagCompound.setString("Potion", tag);
                    stack.setTag(tagCompound);
                    return CraftItemStack.asBukkitCopy(stack);
                }
            }
        }
        // Schematic is old, the potions do not have tags
        // Set it to zero so that the potion bottles don't look like giant purple and black blocks
        chestItem.setDurability((short)0);
        Bukkit.getLogger().warning("Potion in schematic is pre-V1.9 format and will just be water.");
        return chestItem;
    }

    /**
     * Get spawn egg
     * @param type
     * @param amount
     * @return
     */
    public ItemStack getSpawnEgg(EntityType type, int amount) {
        // Doesn't use NMS anymore, but ohwell. :shrug:
        return new ItemStack(Material.valueOf(type.getName() + "_SPAWN_EGG"), amount);
    }

    private void setInChunk(Chunk chunk, BlockPosition bp, IBlockData ibd) {
        try {
            if (setTypeMethod == null) { // Having to use reflection for this sucks, but they remapped the name of the method while using the same revision.
                setTypeMethod = Chunk.class.getDeclaredMethod(Bukkit.getServer().getVersion().contains("1.13.1") ? "a" : "setType", BlockPosition.class, IBlockData.class, boolean.class, boolean.class);
            }
            setTypeMethod.invoke(chunk, bp, ibd, false, true);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
}