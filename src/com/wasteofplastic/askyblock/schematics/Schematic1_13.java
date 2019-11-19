package com.wasteofplastic.askyblock.schematics;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.SignBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.wasteofplastic.askyblock.ASkyBlock;
import com.wasteofplastic.askyblock.Settings;
import com.wasteofplastic.askyblock.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

public class Schematic1_13 extends Schematic {
    private File file;
    private ASkyBlock plugin;

    public Schematic1_13(ASkyBlock plugin) {
        super(plugin);

        this.plugin = plugin;
    }

    public Schematic1_13(ASkyBlock plugin, File file) throws IOException {
        super(plugin, file);

        this.file = file;
        this.plugin = plugin;

        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                Clipboard clipboard = reader.read();

                setWidth(clipboard.getDimensions().getBlockX());
                setHeight(clipboard.getDimensions().getBlockY());
                setLength(clipboard.getDimensions().getBlockZ());

                for (int x = clipboard.getMinimumPoint().getX(); x <= clipboard.getMaximumPoint().getX(); ++x) {
                    for (int y = clipboard.getMinimumPoint().getY(); y <= clipboard.getMaximumPoint().getY(); ++y) {
                        for (int z = clipboard.getMinimumPoint().getZ(); z <= clipboard.getMaximumPoint().getZ(); ++z) {
                            BlockState blockState = clipboard.getBlock(BlockVector3.at(x, y, z));
                            XMaterial xMaterial = XMaterial.matchXMaterial(blockState.getBlockType().getId(), (byte) 0);
                            if (xMaterial == XMaterial.BEDROCK) {
                                // Last bedrock
                                if (getBedrock() == null || getBedrock().getY() < y) {
                                    setBedrock(new Vector(x - clipboard.getMinimumPoint().getX(), y - clipboard.getMinimumPoint().getY(), z - clipboard.getMinimumPoint().getZ()));
                                }
                            } else if (xMaterial == XMaterial.CHEST) {
                                // Last chest
                                if (getChest() == null || getChest().getY() < y) {
                                    setChest(new Vector(x - clipboard.getMinimumPoint().getX(), y - clipboard.getMinimumPoint().getY(), z - clipboard.getMinimumPoint().getZ()));
                                }
                            } else if (xMaterial == XMaterial.GRASS_BLOCK) {
                                // Grass
                                grassBlocks.add(new Vector(x - clipboard.getMinimumPoint().getX(), y - clipboard.getMinimumPoint().getY(), z - clipboard.getMinimumPoint().getZ()));
                            } else if (xMaterial == XMaterial.OAK_WALL_SIGN || xMaterial == XMaterial.OAK_SIGN || blockState.getBlockType().getName().equalsIgnoreCase("Sign") || blockState.getBlockType().getLegacyId() == 63 || clipboard.getFullBlock(BlockVector3.at(x, y, z)) instanceof SignBlock) { // Apparently signs are hella buggy in 1.13 :shrug:
                                // Sign
                                if (getWelcomeSign() == null || getWelcomeSign().getY() < y) {
                                    setWelcomeSign(new Vector(x - clipboard.getMinimumPoint().getX(), y - clipboard.getMinimumPoint().getY(), z - clipboard.getMinimumPoint().getZ()));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            super.prepareSchematic();
        } catch (IOException e) {
            Bukkit.getLogger().severe("Could not load island schematic! Error in file.");
            e.printStackTrace();

            throw new IOException(); // To prevent the code from continuing.
        }
    }

    @Override
    public void pasteSchematic(final Location loc, final Player player, boolean teleport, final PasteReason reason)  {
        Objects.requireNonNull(loc);

        // If this is not a file schematic, paste the default island
        if (file == null) {
            if (Settings.GAMETYPE == Settings.GameType.ACIDISLAND) {
                generateIslandBlocks(loc,player, reason);
            } else {
                loc.getBlock().setType(Material.BEDROCK);
                ASkyBlock.getPlugin().getLogger().severe("Missing schematic - using bedrock block only");
            }
            return;
        }

        Location blockLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()).subtract(getBedrock());

        if (plugin.isFastAsyncWorldEditEnabled()) {
            try {
                com.boydti.fawe.object.schematic.Schematic schematic = com.boydti.fawe.FaweAPI.load(file);
                schematic.getClipboard().setOrigin(schematic.getClipboard().getMinimumPoint());
                schematic.paste(com.boydti.fawe.FaweAPI.getEditSessionBuilder(com.boydti.fawe.FaweAPI.getWorld(loc.getWorld().getName())).allowedRegionsEverywhere().checkMemory(false).changeSetNull().fastmode(true).build(), BlockVector3.at(blockLoc.getX(), blockLoc.getY(), blockLoc.getZ()), isPasteAir());
            } catch (IOException e) {
                Bukkit.getLogger().severe("It appears that the schematic file was modified.");
                e.printStackTrace();
            }
        } else {
            EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(loc.getWorld()), -1);
            editSession.setFastMode(true);
            try {
                ClipboardFormat format = ClipboardFormats.findByFile(file);
                try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                    Clipboard clipboard = reader.read();
                    clipboard.setOrigin(clipboard.getMinimumPoint());
                    Operation operation = new ClipboardHolder(clipboard)
                            .createPaste(editSession)
                            .copyEntities(isPasteEntities())
                            .to(BlockVector3.at(blockLoc.getX(), blockLoc.getY(), blockLoc.getZ()))
                            .ignoreAirBlocks(isPasteAir())
                            .build();
                    Operations.complete(operation);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        super.pasteSchematic(loc, player, teleport, reason);
    }

    /**
     * @param playerSpawnBlock the playerSpawnBlock to set
     * @return true if block is found otherwise false
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean setPlayerSpawnBlock(Material playerSpawnBlock) {
        if (getBedrock() == null) {
            return false;
        }
        this.playerSpawn = null;
        // Run through the schematic and try and find the spawnBlock

        Clipboard clipboard;
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                clipboard = reader.read();

                for (int x = clipboard.getMinimumPoint().getX(); x < clipboard.getMaximumPoint().getX(); x++) {
                    for (int y = clipboard.getMinimumPoint().getY(); y < clipboard.getMaximumPoint().getY(); y++) {
                        for (int z = clipboard.getMinimumPoint().getZ(); z < clipboard.getMaximumPoint().getZ(); z++) {
                            com.sk89q.worldedit.world.block.BlockState blockState = clipboard.getBlock(com.sk89q.worldedit.math.BlockVector3.at(x, y, z));
                            XMaterial xMaterial = XMaterial.matchXMaterial(blockState.getBlockType().getId(), (byte) 0);
                            if (xMaterial == XMaterial.matchXMaterial(playerSpawnBlock)) {
                                playerSpawn = new Vector(x - clipboard.getMinimumPoint().getX(), y - clipboard.getMinimumPoint().getY(), z - clipboard.getMinimumPoint().getZ()).subtract(getBedrock());
                                // Set the block to air
                                clipboard.setBlock(com.sk89q.worldedit.math.BlockVector3.at(x, y, z), com.sk89q.worldedit.world.block.BlockTypes.AIR.getDefaultState());
                            }
                        }
                    }
                }
            }

            try (ClipboardWriter writer = format.getWriter(new FileOutputStream(file))) {
                writer.write(clipboard);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
