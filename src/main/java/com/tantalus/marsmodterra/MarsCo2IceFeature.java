package com.tantalus.marsmodterra;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Adds sparse CO2 ice patches to the icy Mars biome during terrain decoration. */
public final class MarsCo2IceFeature extends Feature<NoneFeatureConfiguration>
{
    public MarsCo2IceFeature(Codec<NoneFeatureConfiguration> codec)
    {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState co2Ice = MarsModTerra.CO2_ICE.get().defaultBlockState();
        boolean placed = false;

        for (int i = 0; i < 5; i++)
        {
            int x = origin.getX() + random.nextInt(9) - 4;
            int z = origin.getZ() + random.nextInt(9) - 4;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos target = new BlockPos(x, y, z);
            if (level.isEmptyBlock(target) && level.getBlockState(target.below()).isSolid())
            {
                level.setBlock(target, co2Ice, 2);
                placed = true;
            }
        }

        return placed;
    }
}
