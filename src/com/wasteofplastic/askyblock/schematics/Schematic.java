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

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.InexorableBiome;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.XMaterial;
import com.wasteofplastic.askyblock.commands.IslandCmd;
import com.wasteofplastic.askyblock.util.Util;
import com.wasteofplastic.askyblock.util.VaultHelper;
import com.wasteofplastic.org.jnbt.Tag;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.DirectionalContainer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Schematic {
    private ASkyBlock plugin;
    private int width;
    private int length;
    private int height;
    private Map<BlockVector, Map<String, Tag>> tileEntitiesMap = new HashMap<BlockVector, Map<String, Tag>>();
    private File file;
    private String heading;
    private String name;
    private String perm;
    private String description;
    private int rating;
    private boolean useDefaultChest;
    private Material icon;    
    private Biome biome;
    private boolean pasteEntities;
    private boolean visible;
    private int order;
    // These hashmaps enable translation between WorldEdit strings and Bukkit names
    //private HashMap<String, EntityType> WEtoME = new HashMap<String, EntityType>();
    private List<EntityType> islandCompanion;
    private List<String> companionNames;
    private ItemStack[] defaultChestItems;
    // Name of a schematic this one is paired with
    private String partnerName = "";
    // Key blocks
    private Vector bedrock;
    private Vector chest;
    private Vector welcomeSign;
    private Vector topGrass;
    protected Vector playerSpawn;
    //private boolean pasteAir;
    private int durability;
    private int levelHandicap;
    private double cost;
    private boolean pasteAir;
    private Method pasteMethod;
    protected List<Vector> grassBlocks = new ArrayList<Vector>();
    private Method getMinimumPoint;
    private Method setOrigin;
    private int finishTickChecks;
    // The reason why this schematic is being pasted
    public enum PasteReason {
        /**
         * This is a new island
         */
        NEW_ISLAND,
        /**
         * This is a partner island
         */
        PARTNER,
        /**
         * This is a reset
         */
        RESET
    };

    public Schematic(ASkyBlock plugin) {
        this.plugin = plugin;
        // Initialize 
        name = "";
        heading = "";
        description = "Default Island";
        perm = "";
        icon = Material.MAP;
        rating = 50;
        useDefaultChest = true;	
        biome = Settings.defaultBiome;
        file = null;
        islandCompanion = new ArrayList<EntityType>();
        islandCompanion.add(Settings.islandCompanion);
        companionNames = Settings.companionNames;
        defaultChestItems = Settings.chestItems;
        visible = true;
        order = 0;
        bedrock = null;
        chest = null;
        welcomeSign = null;
        topGrass = null;
        playerSpawn = null;
        //playerSpawnBlock = null;
        partnerName = "";
        finishTickChecks = 40;

        if (!plugin.isOnePointThirteen() && plugin.isFastAsyncWorldEditEnabled()) {
            try {
                pasteMethod = com.boydti.fawe.object.schematic.Schematic.class.getMethod("paste", com.sk89q.worldedit.world.World.class, com.sk89q.worldedit.Vector.class, boolean.class, boolean.class, Transform.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace(); // shouldn't happen but y'know.
            }
        }
    }

    @SuppressWarnings("deprecation")
    public Schematic(ASkyBlock plugin, File file) throws IOException {
        this.plugin = plugin;
        // Initialize
        short[] blocks;
        byte[] data;
        name = file.getName();
        heading = "";
        description = "";
        perm = "";
        icon = Material.MAP;
        rating = 50;
        useDefaultChest = true;
        biome = Settings.defaultBiome;
        islandCompanion = new ArrayList<EntityType>();
        islandCompanion.add(Settings.islandCompanion);
        companionNames = Settings.companionNames;
        defaultChestItems = Settings.chestItems;
        pasteEntities = false;
        visible = true;
        order = 0;
        bedrock = null;
        chest = null;
        welcomeSign = null;
        topGrass = null;
        playerSpawn = null;
        //playerSpawnBlock = null;
        partnerName = "";
        finishTickChecks = 40;

        this.file = file;
        // Test the file, and, assuming it works, begin pre-loading schematic data.
        try {
            if (!plugin.isOnePointThirteen()) {
                try {
                    com.sk89q.worldedit.CuboidClipboard clipboard = com.sk89q.worldedit.CuboidClipboard.loadSchematic(file);

                    width = clipboard.getWidth();
                    height = clipboard.getHeight();
                    length = clipboard.getLength();

                    for (int x = 0; x < width; ++x) {
                        for (int y = 0; y < height; ++y) {
                            for (int z = 0; z < length; ++z) {
                                com.sk89q.worldedit.blocks.BaseBlock baseBlock = clipboard.getBlock(new com.sk89q.worldedit.Vector(x, y, z));
                                XMaterial xMaterial = XMaterial.matchXMaterial(baseBlock.getType(), (byte) baseBlock.getData());
                                if (xMaterial == XMaterial.BEDROCK) {
                                    // Last bedrock
                                    if (bedrock == null || bedrock.getY() < y) {
                                        bedrock = new Vector(x, y, z);
                                    }
                                } else if (xMaterial == XMaterial.CHEST) {
                                    // Last chest
                                    if (chest == null || chest.getY() < y) {
                                        chest = new Vector(x, y, z);
                                    }
                                } else if (baseBlock.getId() == 63 || xMaterial == XMaterial.OAK_WALL_SIGN) {
                                    // Sign
                                    if (welcomeSign == null || welcomeSign.getY() < y) {
                                        welcomeSign = new Vector(x, y, z);
                                    }
                                } else if (xMaterial == XMaterial.GRASS_BLOCK) {
                                    // Grass
                                    grassBlocks.add(new Vector(x, y, z));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!(this instanceof Schematic1_13)) {
                prepareSchematic();
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not load island schematic! Error in file.");
            e.printStackTrace();

            throw new IOException(); // To prevent the code from continuing.
        }

        if (!plugin.isOnePointThirteen() && plugin.isFastAsyncWorldEditEnabled()) {
            try {
                pasteMethod = com.boydti.fawe.object.schematic.Schematic.class.getMethod("paste", com.sk89q.worldedit.world.World.class, com.sk89q.worldedit.Vector.class, boolean.class, boolean.class, Transform.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace(); // shouldn't happen but y'know.
            }
        }

        try {
            getMinimumPoint = Clipboard.class.getMethod("getMinimumPoint");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void prepareSchematic() throws IOException {
        if (bedrock == null) {
            Bukkit.getLogger().severe("Schematic must have at least one bedrock in it!");
            throw new IOException();
        }
        // Find other key blocks
        if (!grassBlocks.isEmpty()) {
            // Sort by height
            List<Vector> sorted = new ArrayList<Vector>();
            for (Vector v : grassBlocks) {
                boolean inserted = false;
                for (int i = 0; i < sorted.size(); i++) {
                    if (v.getBlockY() > sorted.get(i).getBlockY()) {
                        sorted.add(i, v);
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    sorted.add(v);
                }
            }
            topGrass = sorted.get(0);
        } else {
            topGrass = null;
        }

        if (!plugin.isOnePointThirteen()) {
            try {
                setOrigin = Clipboard.class.getMethod("setOrigin", com.sk89q.worldedit.Vector.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return the biome
     */
    public Biome getBiome() {
        return biome;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the file
     */
    public File getFile() {
        return file;
    }

    /**
     * @return the heading
     */
    public String getHeading() {
        return heading;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the icon
     */
    public Material getIcon() {
        return icon;
    }

    /**
     * @return the durability of the icon
     */
    public int getDurability() {
        return durability;
    }

    /**
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the perm
     */
    public String getPerm() {
        return perm;
    }

    /**
     * @return the rating
     */
    public int getRating() {
        return rating;
    }

    /**
     * @return the tileEntitiesMap
     */
    public Map<BlockVector, Map<String, Tag>> getTileEntitiesMap() {
        return tileEntitiesMap;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the bedrock's vector
     */
    protected Vector getBedrock() {
        return bedrock;
    }

    /**
     * @return the chest's vector
     */
    protected Vector getChest() { return chest; }

    /**
     * @return the welcomeSign's vector
     */
    protected Vector getWelcomeSign() { return welcomeSign; }

    /**
     * @return the useDefaultChest
     */
    public boolean isUseDefaultChest() { return useDefaultChest; }

    /**
     * @return the finishTickChecks
     */
    public int getFinishTickChecks() { return finishTickChecks; }

    public void pasteSchematic(final Location loc, final Player player, boolean teleport, final PasteReason reason)  {
        Objects.requireNonNull(loc);

        // If this is not a file schematic, paste the default island
        if (this.file == null) {
            if (Settings.GAMETYPE == Settings.GameType.ACIDISLAND) {
                generateIslandBlocks(loc,player, reason);
            } else {
                loc.getBlock().setType(Material.BEDROCK);
                ASkyBlock.getPlugin().getLogger().severe("Missing schematic - using bedrock block only");
            }
            return;
        }

        Location blockLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()).subtract(bedrock);

        if (plugin.isFastAsyncWorldEditEnabled()) {
            try {
                if (!plugin.isOnePointThirteen()) {
                    com.boydti.fawe.object.schematic.Schematic schematic = com.boydti.fawe.FaweAPI.load(file);
                    setOrigin.invoke(schematic.getClipboard(), getMinimumPoint.invoke(schematic.getClipboard()));
                    pasteMethod.invoke(schematic, com.boydti.fawe.FaweAPI.getWorld(loc.getWorld().getName()), BukkitUtil.toVector(blockLoc), false, isPasteAir(), null);
                }
            } catch (IOException | IllegalAccessException | InvocationTargetException e) {
                Bukkit.getLogger().severe("It appears that the schematic file was modified.");
                e.printStackTrace();
            }
        } else {
            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(blockLoc.getWorld()), -1);
            editSession.setFastMode(true);
            if (!plugin.isOnePointThirteen()) {
                com.sk89q.worldedit.Vector WEorigin = BukkitUtil.toVector(blockLoc);
                try {
                    com.sk89q.worldedit.CuboidClipboard cc = com.sk89q.worldedit.CuboidClipboard.loadSchematic(file);
                    cc.setOffset(new com.sk89q.worldedit.Vector(0, 0, 0));
                    if (pasteEntities) {
                        cc.pasteEntities(WEorigin);
                    }
                    cc.paste(editSession, WEorigin, pasteAir);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (teleport) {
            new BukkitRunnable() {
                int i = 0;

                @Override
                public void run() {
                    Location home = plugin.getGrid().getSafeHomeLocation(player.getUniqueId(), 1);
                    if (home != null) {
                        World world = loc.getWorld();

                        // Find the grass spot
                        final Location grass;
                        if (topGrass != null) {
                            Location gr = topGrass.clone().toLocation(loc.getWorld()).subtract(bedrock);
                            gr.add(loc.toVector());
                            gr.add(new Vector(0.5D,1.1D,0.5D)); // Center of block and a bit up so the animal drops a bit
                            grass = gr;
                        } else {
                            grass = null;
                        }

                        Block blockToChange = null;
                        if (welcomeSign != null) {
                            Vector ws = welcomeSign.clone().subtract(bedrock);
                            ws.add(loc.toVector());
                            blockToChange = ws.toLocation(world).getBlock();
                            BlockState signState = blockToChange.getState();
                            if (signState instanceof Sign) {
                                Sign sign = (Sign) signState;
                                if (sign.getLine(0).isEmpty()) {
                                    sign.setLine(0, plugin.myLocale(player.getUniqueId()).signLine1.replace("[player]", player.getName()));
                                }
                                if (sign.getLine(1).isEmpty()) {
                                    sign.setLine(1, plugin.myLocale(player.getUniqueId()).signLine2.replace("[player]", player.getName()));
                                }
                                if (sign.getLine(2).isEmpty()) {
                                    sign.setLine(2, plugin.myLocale(player.getUniqueId()).signLine3.replace("[player]", player.getName()));
                                }
                                if (sign.getLine(3).isEmpty()) {
                                    sign.setLine(3, plugin.myLocale(player.getUniqueId()).signLine4.replace("[player]", player.getName()));
                                }
                                sign.update(true, false);
                            }
                        }
                        if (chest != null) {
                            Vector ch = chest.clone().subtract(bedrock);
                            ch.add(loc.toVector());
                            blockToChange = ch.toLocation(world).getBlock();
                            if (useDefaultChest) {
                                // Fill the chest
                                if (blockToChange.getType() == Material.CHEST) {
                                    final Chest islandChest = (Chest) blockToChange.getState();
                                    DoubleChest doubleChest = null;
                                    InventoryHolder iH = islandChest.getInventory().getHolder();
                                    if (iH instanceof DoubleChest) {
                                        //Bukkit.getLogger().info("DEBUG: double chest");
                                        doubleChest = (DoubleChest) iH;
                                    }
                                    if (doubleChest != null) {
                                        Inventory inventory = doubleChest.getInventory();
                                        inventory.clear();
                                        inventory.setContents(defaultChestItems);
                                    } else {
                                        Inventory inventory = islandChest.getInventory();
                                        inventory.clear();
                                        inventory.setContents(defaultChestItems);
                                    }
                                }
                            }
                        }
                        plugin.getPlayers().setInTeleport(player.getUniqueId(), true);
                        //player.setInvulnerable(true);
                        // Check distance. If it's too close, warp to spawn to try to clear the client's cache
                        //plugin.getLogger().info("DEBUG: view dist = " + plugin.getServer().getViewDistance());
                        if (player.getWorld().equals(world)) {
                            //plugin.getLogger().info("DEBUG: same world");
                            int distSq = (int)((player.getLocation().distanceSquared(loc) - ((double)Settings.islandDistance * Settings.islandDistance)/16));
                            //plugin.getLogger().info("DEBUG:  distsq = " + distSq);
                            if (plugin.getServer().getViewDistance() * plugin.getServer().getViewDistance() < distSq) {
                                //plugin.getLogger().info("DEBUG: teleporting");
                                player.teleport(world.getSpawnLocation());
                            }
                        }

                        plugin.getGrid().homeTeleport(player);
                        plugin.getPlayers().setInTeleport(player.getUniqueId(), false);
                        // Reset any inventory, etc. This is done AFTER the teleport because other plugins may switch out inventory based on world
                        plugin.resetPlayer(player);
                        // Reset money if required
                        if (Settings.resetMoney) {
                            resetMoney(player);
                        }
                        // Show fancy titles!
                        if (!Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
                            if (!plugin.myLocale(player.getUniqueId()).islandSubTitle.isEmpty()) {
                                //plugin.getLogger().info("DEBUG: title " + player.getName() + " subtitle {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandSubTitle + "\", \"color\":\"" + plugin.myLocale(player.getUniqueId()).islandSubTitleColor + "\"}");
                                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                                        "minecraft:title " + player.getName() + " subtitle {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandSubTitle.replace("[player]", player.getName()) + "\", \"color\":\"" + plugin.myLocale(player.getUniqueId()).islandSubTitleColor + "\"}");
                            }
                            if (!plugin.myLocale(player.getUniqueId()).islandTitle.isEmpty()) {
                                //plugin.getLogger().info("DEBUG: title " + player.getName() + " title {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandTitle + "\", \"color\":\"" + plugin.myLocale(player.getUniqueId()).islandTitleColor + "\"}");
                                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                                        "minecraft:title " + player.getName() + " title {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandTitle.replace("[player]", player.getName()) + "\", \"color\":\"" + plugin.myLocale(player.getUniqueId()).islandTitleColor + "\"}");
                            }
                            if (!plugin.myLocale(player.getUniqueId()).islandDonate.isEmpty() && !plugin.myLocale(player.getUniqueId()).islandURL.isEmpty()) {
                                //plugin.getLogger().info("DEBUG: tellraw " + player.getName() + " {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandDonate + "\",\"color\":\"" + plugin.myLocale(player.getUniqueId()).islandDonateColor + "\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\""
                                //                + plugin.myLocale(player.getUniqueId()).islandURL + "\"}}");
                                plugin.getServer().dispatchCommand(
                                        plugin.getServer().getConsoleSender(),
                                        "minecraft:tellraw " + player.getName() + " {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandDonate.replace("[player]", player.getName()) + "\",\"color\":\"" + plugin.myLocale(player.getUniqueId()).islandDonateColor + "\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\""
                                                + plugin.myLocale(player.getUniqueId()).islandURL + "\"}}");
                            }
                        }
                        if (reason.equals(PasteReason.NEW_ISLAND)) {
                            // Run any commands that need to be run at the start
                            //plugin.getLogger().info("DEBUG: First time");
                            if (!player.hasPermission(Settings.PERMPREFIX + "command.newexempt")) {
                                //plugin.getLogger().info("DEBUG: Executing new island commands");
                                IslandCmd.runCommands(Settings.startCommands, player);
                            }
                        } else if (reason.equals(PasteReason.RESET)) {
                            // Run any commands that need to be run at reset
                            //plugin.getLogger().info("DEBUG: Reset");
                            if (!player.hasPermission(Settings.PERMPREFIX + "command.resetexempt")) {
                                //plugin.getLogger().info("DEBUG: Executing reset island commands");
                                IslandCmd.runCommands(Settings.resetCommands, player);
                            }
                        }
                        if (!islandCompanion.isEmpty() && grass != null) {
                            spawnCompanion(player, grass);
                        }
                        this.cancel();
                    } else if (i >= finishTickChecks) {
                        ASkyBlock.getPlugin().getLogger().severe("Schematic didn't paste correctly/didn't have a place to put player");
                        this.cancel();
                    }
                    i++;
                }
            }.runTaskTimer(plugin, 0, 1);

        }
    }

    /**
     * @param biome the biome to set
     */
    public void setBiome(Biome biome) {
        this.biome = biome;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param heading the heading to set
     */
    public void setHeading(String heading) {
        this.heading = heading;
    }

    public void setIcon(Material icon, int damage) {
        this.icon = icon;
        this.durability = damage;    
    }
    /**
     * @param icon the icon to set
     */
    public void setIcon(Material icon) {
        this.icon = icon;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param perm the perm to set
     */
    public void setPerm(String perm) {
        this.perm = perm;
    }

    /**
     * @param rating the rating to set
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * @param useDefaultChest the useDefaultChest to set
     */
    public void setUseDefaultChest(boolean useDefaultChest) {
        this.useDefaultChest = useDefaultChest;
    }

    /**
     * @param height the height to set
     */
    protected void setHeight(int height) { this.height = height; }

    /**
     * @param length the length to set
     */
    protected void setLength(int length) { this.length = length; }

    /**
     * @param bedrock the bedrock to set
     */
    protected void setBedrock(Vector bedrock) { this.bedrock = bedrock; }

    /**
     * @param chest the chest to set
     */
    protected void setChest(Vector chest) { this.chest = chest; }

    /**
     * @param welcomeSign the welcomeSign to set
     */
    protected void setWelcomeSign(Vector welcomeSign) { this.welcomeSign = welcomeSign; }

    /**
     * @param finishTickChecks the finishTickChecks to set
     */
    public void setFinishTickChecks(int finishTickChecks) { this.finishTickChecks = finishTickChecks; }

    /**
     * @param width the width to set
     */
    protected void setWidth(int width) {
        this.width = width;
    }

    /**
     * Removes all the air blocks if they are not to be pasted.
     * @param pasteAir the pasteAir to set
     */
    public void setPasteAir(boolean pasteAir) { this.pasteAir = pasteAir; }

    /**
     * Creates the AcidIsland default island block by block
     * @param islandLoc
     * @param player
     * @param reason 
     */
    @SuppressWarnings("deprecation")
    public void generateIslandBlocks(final Location islandLoc, final Player player, PasteReason reason) {
        // AcidIsland
        // Build island layer by layer
        // Start from the base
        // half sandstone; half sand
        int x = islandLoc.getBlockX();
        int z = islandLoc.getBlockZ();
        World world = islandLoc.getWorld();
        int y = 0;
        for (int x_space = x - 4; x_space <= x + 4; x_space++) {
            for (int z_space = z - 4; z_space <= z + 4; z_space++) {
                final Block b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.BEDROCK);
                b.setBiome(biome);
            }
        }
        for (y = 1; y < Settings.islandHeight + 5; y++) {
            for (int x_space = x - 4; x_space <= x + 4; x_space++) {
                for (int z_space = z - 4; z_space <= z + 4; z_space++) {
                    final Block b = world.getBlockAt(x_space, y, z_space);
                    if (y < (Settings.islandHeight / 2)) {
                        b.setType(Material.SANDSTONE);
                    } else {
                        b.setType(Material.SAND);
                        Util.setBlockData(b, (byte) 0);
                    }
                }
            }
        }
        // Then cut off the corners to make it round-ish
        for (y = 0; y < Settings.islandHeight + 5; y++) {
            for (int x_space = x - 4; x_space <= x + 4; x_space += 8) {
                for (int z_space = z - 4; z_space <= z + 4; z_space += 8) {
                    final Block b = world.getBlockAt(x_space, y, z_space);
                    b.setType(XMaterial.WATER.parseMaterial());
                }
            }
        }
        // Add some grass
        for (y = Settings.islandHeight + 4; y < Settings.islandHeight + 5; y++) {
            for (int x_space = x - 2; x_space <= x + 2; x_space++) {
                for (int z_space = z - 2; z_space <= z + 2; z_space++) {
                    final Block blockToChange = world.getBlockAt(x_space, y, z_space);
                    blockToChange.setType(Material.GRASS);
                }
            }
        }
        // Place bedrock - MUST be there (ensures island are not
        // overwritten
        Block b = world.getBlockAt(x, Settings.islandHeight, z);
        b.setType(Material.BEDROCK);
        // Then add some more dirt in the classic shape
        y = Settings.islandHeight + 3;
        for (int x_space = x - 2; x_space <= x + 2; x_space++) {
            for (int z_space = z - 2; z_space <= z + 2; z_space++) {
                b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.DIRT);
            }
        }
        b = world.getBlockAt(x - 3, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x + 3, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z - 3);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z + 3);
        b.setType(Material.DIRT);
        y = Settings.islandHeight + 2;
        for (int x_space = x - 1; x_space <= x + 1; x_space++) {
            for (int z_space = z - 1; z_space <= z + 1; z_space++) {
                b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.DIRT);
            }
        }
        b = world.getBlockAt(x - 2, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x + 2, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z - 2);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z + 2);
        b.setType(Material.DIRT);
        y = Settings.islandHeight + 1;
        b = world.getBlockAt(x - 1, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x + 1, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z - 1);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z + 1);
        b.setType(Material.DIRT);

        // Add island items
        y = Settings.islandHeight;
        // Add tree (natural)
        final Location treeLoc = new Location(world, x, y + 5D, z);
        world.generateTree(treeLoc, TreeType.ACACIA);
        // Place the cow
        final Location location = new Location(world, x, (Settings.islandHeight + 5), z - 2);

        // Place a helpful sign in front of player
        Block blockToChange = world.getBlockAt(x, Settings.islandHeight + 5, z + 3);
        blockToChange.setType(Material.SIGN);
        Sign sign = (Sign) blockToChange.getState();
        sign.setLine(0, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine1.replace("[player]", player.getName()));
        sign.setLine(1, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine2.replace("[player]", player.getName()));
        sign.setLine(2, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine3.replace("[player]", player.getName()));
        sign.setLine(3, ASkyBlock.getPlugin().myLocale(player.getUniqueId()).signLine4.replace("[player]", player.getName()));
        ((org.bukkit.material.Sign) sign.getData()).setFacingDirection(BlockFace.NORTH);
        sign.update(true, false);
        // Place the chest - no need to use the safe spawn function
        // because we
        // know what this island looks like
        blockToChange = world.getBlockAt(x, Settings.islandHeight + 5, z + 1);
        blockToChange.setType(Material.CHEST);
        // Only set if the config has items in it
        if (Settings.chestItems.length > 0) {
            final InventoryHolder chest = (InventoryHolder) blockToChange.getState();
            final Inventory inventory = chest.getInventory();
            //inventory.clear();
            inventory.setContents(Settings.chestItems);
        }
        // Fill the chest and orient it correctly (1.8 faces it north!
        DirectionalContainer dc = (DirectionalContainer) blockToChange.getState().getData();
        dc.setFacingDirection(BlockFace.SOUTH);
        Util.setBlockData(blockToChange, dc.getData());
        // Teleport player
        plugin.getGrid().homeTeleport(player);
        // Reset any inventory, etc. This is done AFTER the teleport because other plugins may switch out inventory based on world
        plugin.resetPlayer(player);
        // Reset money if required
        if (Settings.resetMoney) {
            resetMoney(player);
        }
        // Show fancy titles!
        if (!Bukkit.getServer().getVersion().contains("(MC: 1.7")) {
            if (!plugin.myLocale(player.getUniqueId()).islandSubTitle.isEmpty()) {
                //plugin.getLogger().info("DEBUG: title " + player.getName() + " subtitle {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandSubTitle + "\", \"color\":\"" + plugin.myLocale(player.getUniqueId()).islandSubTitleColor + "\"}");
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                        "minecraft:title " + player.getName() + " subtitle {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandSubTitle.replace("[player]", player.getName()) + "\", \"color\":\"" + plugin.myLocale(player.getUniqueId()).islandSubTitleColor + "\"}");
            }
            if (!plugin.myLocale(player.getUniqueId()).islandTitle.isEmpty()) {
                //plugin.getLogger().info("DEBUG: title " + player.getName() + " title {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandTitle + "\", \"color\":\"" + plugin.myLocale(player.getUniqueId()).islandTitleColor + "\"}");
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                        "minecraft:title " + player.getName() + " title {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandTitle.replace("[player]", player.getName()) + "\", \"color\":\"" + plugin.myLocale(player.getUniqueId()).islandTitleColor + "\"}");
            }
            if (!plugin.myLocale(player.getUniqueId()).islandDonate.isEmpty() && !plugin.myLocale(player.getUniqueId()).islandURL.isEmpty()) {
                //plugin.getLogger().info("DEBUG: tellraw " + player.getName() + " {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandDonate + "\",\"color\":\"" + plugin.myLocale(player.getUniqueId()).islandDonateColor + "\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\""
                //                + plugin.myLocale(player.getUniqueId()).islandURL + "\"}}");
                plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        "minecraft:tellraw " + player.getName() + " {\"text\":\"" + plugin.myLocale(player.getUniqueId()).islandDonate.replace("[player]", player.getName()) + "\",\"color\":\"" + plugin.myLocale(player.getUniqueId()).islandDonateColor + "\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\""
                                + plugin.myLocale(player.getUniqueId()).islandURL + "\"}}");
            }
        }
        if (reason.equals(PasteReason.NEW_ISLAND)) {
            // Run any commands that need to be run at the start
            //plugin.getLogger().info("DEBUG: First time 2");
            if (!player.hasPermission(Settings.PERMPREFIX + "command.newexempt")) {
                //plugin.getLogger().info("DEBUG: Executing new island commands 2");
                IslandCmd.runCommands(Settings.startCommands, player);
            }
        } else if (reason.equals(PasteReason.RESET)) {
            // Run any commands that need to be run at reset
            //plugin.getLogger().info("DEBUG: Reset");
            if (!player.hasPermission(Settings.PERMPREFIX + "command.resetexempt")) {
                //plugin.getLogger().info("DEBUG: Executing reset island commands");
                IslandCmd.runCommands(Settings.resetCommands, player);
            }
        }
        if (!islandCompanion.isEmpty()) {
            Bukkit.getServer().getScheduler().runTaskLater(ASkyBlock.getPlugin(), () -> spawnCompanion(player, location), 40L);
        }
    }
    /**
     * Spawns a random companion for the player with a random name at the location given
     * @param player
     * @param location
     */
    protected void spawnCompanion(Player player, Location location) {
        // Older versions of the server require custom names to only apply to Living Entities
        //Bukkit.getLogger().info("DEBUG: spawning compantion at " + location);
        if (!islandCompanion.isEmpty() && location != null) {
            Random rand = new Random();
            int randomNum = rand.nextInt(islandCompanion.size());
            EntityType type = islandCompanion.get(randomNum);
            if (type != null) {
                LivingEntity companion = (LivingEntity) location.getWorld().spawnEntity(location, type);
                if (!companionNames.isEmpty()) {
                    randomNum = rand.nextInt(companionNames.size());
                    String name = companionNames.get(randomNum).replace("[player]", player.getName());
                    //plugin.getLogger().info("DEBUG: name is " + name);
                    companion.setCustomName(name);
                    companion.setCustomNameVisible(true);
                } 
            }
        }
    }

    /**
     * @param islandCompanion the islandCompanion to set
     */
    public void setIslandCompanion(List<EntityType> islandCompanion) {
        this.islandCompanion = islandCompanion;
    }

    /**
     * @param companionNames the companionNames to set
     */
    public void setCompanionNames(List<String> companionNames) {
        this.companionNames = companionNames;
    }

    /**
     * @param defaultChestItems the defaultChestItems to set
     */
    public void setDefaultChestItems(ItemStack[] defaultChestItems) {
        this.defaultChestItems = defaultChestItems;
    }

    /**
     * @return if Biome is HELL, this is true
     */
    public boolean isInNether() {
        if (biome == InexorableBiome.NETHER.retrieveBiome()) {
            return true;
        }
        return false;
    }

    /**
     * @return the partnerName
     */
    public String getPartnerName() {
        return partnerName;
    }

    /**
     * @param partnerName the partnerName to set
     */
    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    /**
     * @return the pasteAir
     */
    public boolean isPasteAir() {
        return pasteAir;
    }

    /**
     * @return the pasteEntities
     */
    public boolean isPasteEntities() {
        return pasteEntities;
    }

    /**
     * @param pasteEntities the pasteEntities to set
     */
    public void setPasteEntities(boolean pasteEntities) {
        this.pasteEntities = pasteEntities;
    }

    /**
     * Whether the schematic is visible or not
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets if the schematic can be seen in the schematics GUI or not by the player
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }


    /**
     * @return true if player spawn exists in this schematic
     */
    public boolean isPlayerSpawn() {
        if (playerSpawn == null) {
            return false;
        }
        return true;
    }

    /**
     * @return the playerSpawn Location given a paste location
     */
    public Location getPlayerSpawn(Location pasteLocation) {
        return pasteLocation.clone().add(playerSpawn);
    }

    /**
     * @param playerSpawnBlock the playerSpawnBlock to set
     * @return true if block is found otherwise false
     */
    @SuppressWarnings("deprecation")
    public boolean setPlayerSpawnBlock(Material playerSpawnBlock) {
        if (bedrock == null) {
            return false;
        }
        playerSpawn = null;
        // Run through the schematic and try and find the spawnBlock
        if (!plugin.isOnePointThirteen()) {
            try {
                com.sk89q.worldedit.CuboidClipboard clipboard = com.sk89q.worldedit.CuboidClipboard.loadSchematic(file);

                for (int x = 0; x < width; ++x) {
                    for (int y = 0; y < height; ++y) {
                        for (int z = 0; z < length; ++z) {
                            com.sk89q.worldedit.blocks.BaseBlock baseBlock = clipboard.getBlock(new com.sk89q.worldedit.Vector(x, y, z));
                            XMaterial xMaterial = XMaterial.matchXMaterial(baseBlock.getType(), (byte) baseBlock.getData());
                            if (xMaterial == XMaterial.matchXMaterial(playerSpawnBlock)) {
                                playerSpawn = new Vector(x - clipboard.getOffset().getX(), y - clipboard.getOffset().getY(), z - clipboard.getOffset().getZ()).subtract(bedrock);
                                // Set the block to air
                                clipboard.setBlock(new com.sk89q.worldedit.Vector(x, y, z), baseBlock);
                            }
                        }
                    }
                }

                clipboard.saveSchematic(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /**
     * @return the levelHandicap
     */
    public int getLevelHandicap() {
        return levelHandicap;
    }

    /**
     * @param levelHandicap the levelHandicap to set
     */
    public void setLevelHandicap(int levelHandicap) {
        this.levelHandicap = levelHandicap;
    }

    /**
     * Set the cost
     * @param cost
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * @return the cost
     */
    public double getCost() {
        return cost;
    }

    private void resetMoney(Player player) {
        if (!Settings.useEconomy) {
            return;
        }
        // Set player's balance in acid island to the starting balance
        try {
            // plugin.getLogger().info("DEBUG: " + player.getName() + " " +
            // Settings.general_worldName);
            if (VaultHelper.econ == null) {
                // plugin.getLogger().warning("DEBUG: econ is null!");
                VaultHelper.setupEconomy();
            }
            Double playerBalance = VaultHelper.econ.getBalance(player, Settings.worldName);
            // plugin.getLogger().info("DEBUG: playerbalance = " +
            // playerBalance);
            // Round the balance to 2 decimal places and slightly down to
            // avoid issues when withdrawing the amount later
            BigDecimal bd = new BigDecimal(playerBalance);
            bd = bd.setScale(2, RoundingMode.HALF_DOWN);
            playerBalance = bd.doubleValue();
            // plugin.getLogger().info("DEBUG: playerbalance after rounding = "
            // + playerBalance);
            if (playerBalance != Settings.startingMoney) {
                if (playerBalance > Settings.startingMoney) {
                    Double difference = playerBalance - Settings.startingMoney;
                    EconomyResponse response = VaultHelper.econ.withdrawPlayer(player, Settings.worldName, difference);
                    // plugin.getLogger().info("DEBUG: withdrawn");
                    if (response.transactionSuccess()) {
                        plugin.getLogger().info(
                                "FYI:" + player.getName() + " had " + VaultHelper.econ.format(playerBalance) + " when they typed /island and it was set to "
                                        + Settings.startingMoney);
                    } else {
                        plugin.getLogger().warning(
                                "Problem trying to withdraw " + playerBalance + " from " + player.getName() + "'s account when they typed /island!");
                        plugin.getLogger().warning("Error from economy was: " + response.errorMessage);
                    }
                } else {
                    Double difference = Settings.startingMoney - playerBalance;
                    EconomyResponse response = VaultHelper.econ.depositPlayer(player, Settings.worldName, difference);
                    if (response.transactionSuccess()) {
                        plugin.getLogger().info(
                                "FYI:" + player.getName() + " had " + VaultHelper.econ.format(playerBalance) + " when they typed /island and it was set to "
                                        + Settings.startingMoney);
                    } else {
                        plugin.getLogger().warning(
                                "Problem trying to deposit " + playerBalance + " from " + player.getName() + "'s account when they typed /island!");
                        plugin.getLogger().warning("Error from economy was: " + response.errorMessage);
                    }

                }
            }
        } catch (final Exception e) {
            plugin.getLogger().severe("Error trying to zero " + player.getName() + "'s account when they typed /island!");
            plugin.getLogger().severe(e.getMessage());
        }

    }
}