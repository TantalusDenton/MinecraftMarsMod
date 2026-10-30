package com.tantalus.marsmodterra;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Integration checks run in an isolated GameTest save, never in a player's world. */
@GameTestHolder(MarsModTerra.MODID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = MarsModTerra.MODID)
public final class MarsWorldgenTests {
    @SubscribeEvent
    public static void template(ServerStartingEvent event) {
        ServerLevel level = event.getServer().overworld();
        level.getStructureManager().getOrCreate(new ResourceLocation(MarsModTerra.MODID, "empty"))
                .fillFromWorld(level, new BlockPos(0, 250, 0), new Vec3i(1, 1, 1), false, Blocks.STRUCTURE_VOID);
    }

    @GameTest(template = "empty", batch = "mars", timeoutTicks = 1200)
    public static void dryTerrainAndCaves(GameTestHelper helper) {
        ServerLevel mars = helper.getLevel().getServer().getLevel(MarsModTerra.MARS_LEVEL);
        helper.assertTrue(mars != null, "Mars dimension must load");
        var generator = mars.getChunkSource().getGenerator();
        var sampler = mars.getChunkSource().randomState().sampler();
        Map<String, BlockPos> samples = new HashMap<>();
        for (int x = -4096; x <= 4096 && samples.size() < 6; x += 128) {
            for (int z = -4096; z <= 4096 && samples.size() < 6; z += 128) {
                var biome = generator.getBiomeSource().getNoiseBiome(x >> 2, 20, z >> 2, sampler);
                String name = biome.unwrapKey().orElseThrow().location().getPath();
                samples.putIfAbsent(name, new BlockPos(x, 80, z));
                for (var category : MobCategory.values())
                    helper.assertTrue(biome.value().getMobSettings().getMobs(category).isEmpty(),
                            "Spawn entries in " + name);
            }
        }
        helper.assertTrue(samples.size() == 6, "Expected all six biomes, found " + samples.keySet());
        Set<Block> palette = Set.of(Blocks.GRANITE, Blocks.RED_SAND, Blocks.RED_TERRACOTTA,
                Blocks.ORANGE_TERRACOTTA, Blocks.REDSTONE_BLOCK, MarsModTerra.CO2_ICE.get());
        int undergroundAir = 0;
        for (var sample : samples.entrySet()) {
            var chunk = mars.getChunkAt(sample.getValue());
            var pos = new BlockPos.MutableBlockPos();
            int solids = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = mars.getMinBuildHeight(); y < mars.getMaxBuildHeight(); y++) {
                    pos.set(chunk.getPos().getMinBlockX() + x, y, chunk.getPos().getMinBlockZ() + z);
                    var state = chunk.getBlockState(pos);
                    helper.assertTrue(state.getFluidState().isEmpty(), "Fluid at " + pos);
                    if (!state.isAir()) {
                        helper.assertTrue(palette.contains(state.getBlock()), "Unexpected " + state + " in " + sample.getKey());
                        solids++;
                    } else if (y > -55 && y < surface - 12) undergroundAir++;
                }
            }
            helper.assertTrue(solids > 1000, "Missing solid terrain in " + sample.getKey());
        }
        helper.assertTrue(undergroundAir > 100, "Expected underground caverns");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mars")
    public static void co2SublimatesWithoutWater(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(MarsModTerra.MARS_LEVEL);
        ServerLevel clock = helper.getLevel().getServer().overworld();
        BlockPos pos = new BlockPos(8, 310, 8);
        long previousTime = level.getDayTime();
        var previousBlock = level.getBlockState(pos);
        try {
            level.setBlockAndUpdate(pos, MarsModTerra.CO2_ICE.get().defaultBlockState());
            clock.setDayTime(6000);
            level.getBlockState(pos).randomTick(level, pos, level.random);
            helper.assertTrue(level.getBlockState(pos).is(MarsModTerra.CO2_ICE.get()), "Ice must persist at midday");
            clock.setDayTime(0);
            level.getBlockState(pos).randomTick(level, pos, level.random);
            helper.assertTrue(level.getBlockState(pos).isAir(), "Ice must sublimate at sunrise");
            helper.assertTrue(level.getFluidState(pos).isEmpty(), "CO2 must never leave water");
        } finally {
            clock.setDayTime(previousTime);
            level.setBlockAndUpdate(pos, previousBlock);
        }
        helper.succeed();
    }
}
