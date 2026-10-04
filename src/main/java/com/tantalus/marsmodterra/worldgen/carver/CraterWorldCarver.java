package com.tantalus.marsmodterra.worldgen.carver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

import java.util.function.Function;

public final class CraterWorldCarver extends WorldCarver<CraterCarverConfiguration> {
  private static final double TWO_PI = Math.PI * 2.0;

  public CraterWorldCarver() {
    super(CraterCarverConfiguration.CODEC);
  }

  @Override
  public boolean isStartChunk(CraterCarverConfiguration config, RandomSource random) {
    return random.nextFloat() <= config.probability;
  }

  @Override
  public int getRange() {
    return 8;
  }

  @Override
  public boolean carve(
    CarvingContext context,
    CraterCarverConfiguration config,
    ChunkAccess chunk,
    Function<BlockPos, Holder<Biome>> biomeAccessor,
    RandomSource random,
    Aquifer aquifer,
    ChunkPos originChunk,
    CarvingMask carvingMask
  ) {
    int centerX = originChunk.getMinBlockX() + random.nextInt(16);
    int centerZ = originChunk.getMinBlockZ() + random.nextInt(16);

    int radiusRange = config.maxRadius - config.minRadius + 1;
    int radius = config.minRadius + random.nextInt(radiusRange);

    double depthVariation = 0.85 + random.nextDouble() * 0.30;
    double maxDepth = radius * config.depthFactor * depthVariation;
    double maxRimHeight = radius * config.rimHeightFactor;

    double phaseA = random.nextDouble() * TWO_PI;
    double phaseB = random.nextDouble() * TWO_PI;
    double phaseC = random.nextDouble() * TWO_PI;

    double maximumOuterRadius = radius * (1.0 + config.irregularity + config.rimWidthFactor);

    ChunkPos targetChunk = chunk.getPos();
    int chunkMinX = targetChunk.getMinBlockX();
    int chunkMinZ = targetChunk.getMinBlockZ();
    int chunkMaxX = chunkMinX + 15;
    int chunkMaxZ = chunkMinZ + 15;

    int minX = Math.max(chunkMinX, (int) Math.floor(centerX - maximumOuterRadius));
    int maxX = Math.min(chunkMaxX, (int) Math.ceil(centerX + maximumOuterRadius));
    int minZ = Math.max(chunkMinZ, (int) Math.floor(centerZ - maximumOuterRadius));
    int maxZ = Math.min(chunkMaxZ, (int) Math.ceil(centerZ + maximumOuterRadius));

    boolean changed = false;
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    for (int worldX = minX; worldX <= maxX; worldX++) {
      for (int worldZ = minZ; worldZ <= maxZ; worldZ++) {
        double dx = worldX + 0.5 - centerX;
        double dz = worldZ + 0.5 - centerZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double angle = Math.atan2(dz, dx);

        double edgeVariation =
          Math.sin(angle * 5.0 + phaseA) * 0.50
            + Math.sin(angle * 9.0 + phaseB) * 0.30
            + Math.sin(angle * 17.0 + phaseC) * 0.20;

        double effectiveRadius = radius * (1.0 + edgeVariation * config.irregularity);
        double normalizedDistance = distance / effectiveRadius;

        int localX = worldX - chunkMinX;
        int localZ = worldZ - chunkMinZ;

        int surfaceY = chunk.getHeight(
          Heightmap.Types.WORLD_SURFACE_WG,
          localX,
          localZ
        ) - 1;

        if (surfaceY < chunk.getMinBuildHeight()) {
          continue;
        }

        if (normalizedDistance <= 1.0) {
          double bowl = 1.0 - normalizedDistance * normalizedDistance;
          bowl = Math.pow(bowl, 1.35);

          int depth = Math.max(1, (int) Math.round(maxDepth * bowl));

          if (carveColumn(
            config,
            chunk,
            carvingMask,
            pos,
            worldX,
            worldZ,
            localX,
            localZ,
            surfaceY,
            depth
          )) {
            changed = true;
          }
        } else {
          double outerRadius = effectiveRadius * (1.0 + config.rimWidthFactor);

          if (distance <= outerRadius && config.rimWidthFactor > 0.0F) {
            double rimProgress =
              (distance - effectiveRadius)
                / (outerRadius - effectiveRadius);

            rimProgress = Math.max(0.0, Math.min(1.0, rimProgress));

            double rimShape = Math.sin(Math.PI * rimProgress);
            int rimHeight = (int) Math.round(maxRimHeight * rimShape);

            if (rimHeight > 0 && buildRimColumn(
              config,
              chunk,
              pos,
              worldX,
              worldZ,
              surfaceY,
              rimHeight
            )) {
              changed = true;
            }
          }
        }
      }
    }

    return changed;
  }

  private boolean carveColumn(
    CraterCarverConfiguration config,
    ChunkAccess chunk,
    CarvingMask carvingMask,
    BlockPos.MutableBlockPos pos,
    int worldX,
    int worldZ,
    int localX,
    int localZ,
    int surfaceY,
    int depth
  ) {
    int bottomY = Math.max(
      chunk.getMinBuildHeight(),
      surfaceY - depth + 1
    );

    boolean changed = false;

    for (int y = surfaceY + 1; y >= bottomY; y--) {
      pos.set(worldX, y, worldZ);

      chunk.setBlockState(pos, AIR, false);
      carvingMask.set(localX, y, localZ);
      changed = true;
    }

    return changed;
  }

  private boolean buildRimColumn(
    CraterCarverConfiguration config,
    ChunkAccess chunk,
    BlockPos.MutableBlockPos pos,
    int worldX,
    int worldZ,
    int surfaceY,
    int rimHeight
  ) {
    int newSurfaceY = Math.min(
      chunk.getMaxBuildHeight() - 1,
      surfaceY + rimHeight
    );

    int rebuildBottom = Math.max(
      chunk.getMinBuildHeight(),
      surfaceY - 3
    );

    boolean changed = false;

    for (int y = rebuildBottom; y <= newSurfaceY; y++) {
      pos.set(worldX, y, worldZ);

      BlockState existing = chunk.getBlockState(pos);

      if (y <= surfaceY && !canReplaceBlock(config, existing)) {
        continue;
      }

      int depthBelowSurface = newSurfaceY - y;
      BlockState replacement = rimState(depthBelowSurface);

      if (!existing.equals(replacement)) {
        chunk.setBlockState(pos, replacement, false);
        changed = true;
      }
    }

    return changed;
  }

  private BlockState rimState(int depthBelowSurface) {
    if (depthBelowSurface == 0) {
      return Blocks.RED_SAND.defaultBlockState();
    }

    if (depthBelowSurface <= 3) {
      return Blocks.RED_SANDSTONE.defaultBlockState();
    }

    return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
  }
}