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
import java.util.HashSet;
import com.tantalus.marsmodterra.worldgen.carver.CraterCarverConfiguration;
import com.tantalus.marsmodterra.worldgen.carver.ModWorldCarvers;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;

/** Integration checks run in an isolated GameTest save, never in a player's world. */
@GameTestHolder(MarsModTerra.MODID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = MarsModTerra.MODID)
public final class MarsWorldgenTests {
    @GameTest(template = "empty", batch = "mars")
    public static void craterBiomeAssignments(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        Map<String, Set<String>> expected = Map.of(
                "mars_craters", Set.of("small_crater", "medium_crater", "large_crater", "huge_crater"),
                "mars_desert", Set.of("small_crater"),
                "mars_canyons", Set.of("small_crater", "medium_crater"),
                "mars_icy", Set.of("small_frozen_crater", "medium_frozen_crater"),
                "cold_basin", Set.of("small_frozen_crater", "medium_frozen_crater", "large_frozen_crater", "huge_frozen_crater"),
                "mars_mountains", Set.of());
        expected.forEach((name, wanted) -> {
            var biome = biomes.get(new ResourceLocation(MarsModTerra.MODID, name));
            Set<String> actual = new HashSet<>();
            for (var carver : biome.getGenerationSettings().getCarvers(GenerationStep.Carving.AIR)) {
                String path = carver.unwrapKey().orElseThrow().location().getPath();
                if (path.endsWith("_crater")) {
                    actual.add(path);
                    helper.assertTrue(carver.value().worldCarver() == (path.contains("frozen")
                            ? ModWorldCarvers.FROZEN_CRATER.get() : ModWorldCarvers.CRATER.get()),
                            "Wrong registered carver for " + path);
                }
            }
            helper.assertTrue(actual.equals(wanted), name + ": expected " + wanted + ", found " + actual);
        });
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "mars", timeoutTicks = 1200)
    @SuppressWarnings("unchecked")
    public static void configuredCratersOpenBowlsAndBuildRims(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(MarsModTerra.MARS_LEVEL);
        var registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_CARVER);
        for (String size : new String[]{"small", "medium", "large", "huge"}) {
            for (boolean frozen : new boolean[]{false, true}) {
                String name = size + (frozen ? "_frozen_crater" : "_crater");
                var configured = (ConfiguredWorldCarver<CraterCarverConfiguration>) registry.get(
                        new ResourceLocation(MarsModTerra.MODID, name));
                long seed = 42;
                var preview = RandomSource.create(seed);
                int centerX = preview.nextInt(16);
                int centerZ = preview.nextInt(16);
                int radius = configured.config().minRadius + preview.nextInt(
                        configured.config().maxRadius - configured.config().minRadius + 1);
                ProtoChunk center = flatChunk(level, new ChunkPos(0, 0));
                // These carvers only consume the configuration, chunk, random, origin, and mask.
                configured.carve(null, center, level::getBiome, RandomSource.create(seed), null,
                        new ChunkPos(0, 0), new CarvingMask(level.getHeight(), level.getMinBuildHeight()));
                BlockPos top = new BlockPos(centerX, 80, centerZ);
                helper.assertTrue(center.getBlockState(top).isAir(), name + " must open the surface");
                int floor = center.getHeight(Heightmap.Types.WORLD_SURFACE_WG, centerX, centerZ);
                helper.assertTrue(floor < 80, name + " must excavate a bowl");
                helper.assertTrue(center.getBlockState(new BlockPos(centerX, floor, centerZ)).is(
                        frozen ? MarsModTerra.CO2_ICE.get() : Blocks.GRANITE), name + " has the wrong floor material");
                if (!size.equals("small")) {
                    ProtoChunk rim = flatChunk(level, new ChunkPos((centerX + radius) >> 4, centerZ >> 4));
                    configured.carve(null, rim, level::getBiome, RandomSource.create(seed), null,
                            new ChunkPos(0, 0), new CarvingMask(level.getHeight(), level.getMinBuildHeight()));
                    boolean raised = false;
                    for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                        int y = rim.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                        if (y > 80) {
                            raised = true;
                            helper.assertTrue(rim.getBlockState(new BlockPos(
                                    rim.getPos().getMinBlockX() + x, y, rim.getPos().getMinBlockZ() + z))
                                    .is(frozen ? MarsModTerra.CO2_ICE.get() : Blocks.RED_SAND),
                                    name + " has the wrong rim material");
                        }
                    }
                    helper.assertTrue(raised, name + " must build a raised rim");
                }
            }
        }
        helper.succeed();
    }

    private static ProtoChunk flatChunk(ServerLevel level, ChunkPos chunkPos) {
        var chunk = new ProtoChunk(chunkPos, UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        var pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 40; y <= 80; y++) {
            pos.set(chunkPos.getMinBlockX() + x, y, chunkPos.getMinBlockZ() + z);
            chunk.setBlockState(pos, Blocks.GRANITE.defaultBlockState(), false);
        }
        return chunk;
    }

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
