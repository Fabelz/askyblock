package com.wasteofplastic.askyblock;

/*
* InexorableBiome - A cross-version biome library intended to be used to retrieve biomes regardless of server versions.
* Biome enum names are to be called what they are in the latest Bukkit server release.
*
* Caching, while it could be added, isn't needed due to the small size of the overall enum.

Created by Fabelz.
Last updated for: 1.14.3
 */

import org.bukkit.block.Biome;

public enum InexorableBiome {
    OCEAN,
    PLAINS,
    DESERT,
    MOUNTAINS("EXTREME_HILLS"),
    FOREST,
    TAIGA,
    SWAMP("SWAMPLAND"),
    RIVER,
    NETHER("HELL"),
    THE_END("SKY"),
    FROZEN_OCEAN,
    FROZEN_RIVER,
    SNOWY_TUNDRA("ICE_FLATS"),
    SNOWY_MOUNTAINS("ICE_MOUNTAINS"),
    MUSHROOM_FIELDS("MUSHROOM_ISLAND"),
    MUSHROOM_FIELD_SHORE("MUSHROOM_FIELD_SHORE"),
    BEACH,
    DESERT_HILLS,
    WOODED_HILLS("FOREST_HILLS"),
    TAIGA_HILLS,
    MOUNTAIN_EDGE("SMALLER_EXTREME_HILLS"),
    JUNGLE,
    JUNGLE_HILLS,
    JUNGLE_EDGE,
    DEEP_OCEAN,
    STONE_SHORE("STONE_BEACH"),
    SNOWY_BEACH("COLD_BEACH"),
    BIRCH_FOREST,
    BIRCH_FOREST_HILLS,
    DARK_FOREST("ROOFED_FOREST"),
    SNOWY_TAIGA("TAIGA_COLD"),
    SNOWY_TAIGA_HILLS("TAIGA_COLD_HILLS"),
    GIANT_TREE_TAIGA("REDWOOD_TAIGA"),
    GIANT_TREE_TAIGA_HILLS("REDWOOD_TAIGA_HILLS"),
    WOODED_MOUNTAINS("EXTREME_HILLS_WITH_TREES"),
    SAVANNA,
    SAVANNA_PLATEAU("SAVANNA_ROCK"),
    BADLANDS("MESA"),
    WOODED_BADLANDS_PLATEAU("MESA_ROCK"),
    BADLANDS_PLATEAU("MESA_CLEAR_ROCK"),
    SMALL_END_ISLANDS,
    END_MIDLANDS,
    END_HIGHLANDS,
    END_BARRENS,
    WARM_OCEAN,
    LUKEWARM_OCEAN,
    COLD_OCEAN,
    DEEP_WARM_OCEAN,
    DEEP_LUKEWARM_OCEAN,
    DEEP_COLD_OCEAN,
    DEEP_FROZEN_OCEAN,
    THE_VOID("VOID"),
    SUNFLOWER_PLAINS("MUTATED_PLAINS"),
    DESERT_LAKES("MUTATED_DESERT"),
    GRAVELLY_MOUNTAINS("MUTATED_EXTREME_HILLS"),
    FLOWER_FOREST("MUTATED_FOREST"),
    TAIGA_MOUNTAINS("MUTATED_TAIGA"),
    SWAMP_HILLS("MUTATED_SWAMPLAND"),
    ICE_SPIKES("MUTATED_ICE_FLATS"),
    MODIFIED_JUNGLE("MUTATED_JUNGLE"),
    MODIFIED_JUNGLE_EDGE("MUTATED_JUNGLE_EDGE"),
    TALL_BIRCH_FOREST("MUTATED_BIRCH_FOREST"),
    TALL_BIRCH_HILLS("MUTATED_BIRCH_FOREST_HILLS"),
    DARK_FOREST_HILLS("MUTATED_ROOFED_FOREST"),
    SNOWY_TAIGA_MOUNTAINS("MUTATED_TAIGA_COLD"),
    GIANT_SPRUCE_TAIGA("MUTATED_REDWOOD_TAIGA"),
    GIANT_SPRUCE_TAIGA_HILLS("MUTATED_REDWOOD_TAIGA_HILLS"),
    MODIFIED_GRAVELLY_MOUNTAINS("MUTATED_EXTREME_HILLS_WITH_TREES"),
    SHATTERED_SAVANNA("MUTATED_SAVANNA"),
    SHATTERED_SAVANNA_PLATEAU("MUTATED_SAVANNA_ROCK"),
    ERODED_BADLANDS("MUTATED_MESA"),
    MODIFIED_WOODED_BADLANDS_PLATEAU("MUTATED_MESA_ROCK"),
    MODIFIED_BADLANDS_PLATEAU("MUTATED_MESA_CLEAR_ROCK"),
    BAMBOO_JUNGLE,
    BAMBOO_JUNGLE_HILLS;

    private String[] previousNames;

    InexorableBiome(String... previousNames) {
        this.previousNames = previousNames;
    }

    /**
     * Parses the InexorableBiome as a {@link Biome}.
     *
     * @return the biome. Null if it doesn't exist for that server version.
     */
    public Biome retrieveBiome() {
        for (Biome biome : Biome.values()) {
            if (biome.name().equals(this.name())) {
                return biome;
            } else {
                for (String previousName : previousNames) {
                    if (biome.name().equals(previousName)) {
                        return biome;
                    }
                }
            }
        }

        return null;
    }
}