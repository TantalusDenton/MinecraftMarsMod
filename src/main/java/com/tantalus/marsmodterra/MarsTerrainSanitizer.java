package com.tantalus.marsmodterra;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Gives generated Mars terrain its material identity after vanilla's noise and
 * cave carvers run. Air is deliberately untouched, preserving all caverns.
 */
public final class MarsTerrainSanitizer
{
    private MarsTerrainSanitizer()
    {
    }

    @SubscribeEvent
    public static void onNewMarsChunk(ChunkEvent.Load event)
    {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level))
            return;

        ChunkAccess chunk = event.getChunk();
        BlockPos sample = new BlockPos(chunk.getPos().getMinBlockX() + 8, level.getSeaLevel(), chunk.getPos().getMinBlockZ() + 8);
        if (!MarsModTerra.isMarsWorld(level, sample))
            return;
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {
                for (int y = minY; y < maxY; y++)
                {
                    pos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    BlockState current = chunk.getBlockState(pos);
                    BlockState replacement = marsState(level, pos, current, y, minY);
                    if (replacement != null)
                        chunk.setBlockState(pos, replacement, false);
                }
            }
        }
    }

    private static BlockState marsState(ServerLevel level, BlockPos pos, BlockState current, int y, int minY)
    {
        if (current.isAir() || current.is(Blocks.BEDROCK) || current.is(MarsModTerra.CO2_ICE.get()))
            return null;

        if (y <= minY + 5)
            return Blocks.GRANITE.defaultBlockState();
        String biome = level.getBiome(pos).unwrapKey().map(key -> key.location().getPath()).orElse("");
        return switch (biome)
        {
            case "mars_icy", "cold_basin" -> y > 72 ? Blocks.ORANGE_TERRACOTTA.defaultBlockState() : Blocks.GRANITE.defaultBlockState();
            case "mars_canyons" -> y > 68 ? Blocks.RED_TERRACOTTA.defaultBlockState() : Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            case "mars_desert" -> y > 64 ? Blocks.RED_SAND.defaultBlockState() : Blocks.RED_TERRACOTTA.defaultBlockState();
            case "mars_mountains" -> y > 86 ? Blocks.GRANITE.defaultBlockState() : Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            case "mars_craters" -> y > 68 ? Blocks.REDSTONE_BLOCK.defaultBlockState() : Blocks.RED_TERRACOTTA.defaultBlockState();
          default -> Blocks.RED_TERRACOTTA.defaultBlockState();
        };
    }
}
