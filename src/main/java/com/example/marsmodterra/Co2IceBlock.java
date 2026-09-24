package com.example.marsmodterra;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Frozen carbon dioxide.  At the first light of a Martian day, exposed ice
 * quietly turns into thin smoke and vanishes back into the atmosphere.
 */
public final class Co2IceBlock extends Block
{
    public Co2IceBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        long timeOfDay = level.getDayTime() % 24000L;
        boolean sunrise = timeOfDay >= 23000L || timeOfDay <= 1000L;
        if (!sunrise || !level.canSeeSky(pos.above()) || level.getBrightness(LightLayer.SKY, pos.above()) < 11)
            return;

        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                5, 0.22D, 0.12D, 0.22D, 0.008D);
        level.removeBlock(pos, false);
    }
}
