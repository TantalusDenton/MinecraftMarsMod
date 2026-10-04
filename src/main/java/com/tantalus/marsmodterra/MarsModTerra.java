package com.tantalus.marsmodterra;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import static com.tantalus.marsmodterra.worldgen.carver.ModWorldCarvers.WORLD_CARVERS;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MarsModTerra.MODID)
public class MarsModTerra
{
    /** The registry namespace for the MarsModTerra content package. */
    public static final String MODID = "marsmodterra";
    public static final ResourceKey<net.minecraft.world.level.Level> MARS_LEVEL = ResourceKey.create(
            Registries.DIMENSION, new ResourceLocation(MODID, "mars"));

    /** True for the standalone Mars dimension and a Mars world selected at world creation. */
    public static boolean isMarsWorld(net.minecraft.world.level.Level level, BlockPos pos)
    {
        return level.dimension().equals(MARS_LEVEL) || level.getBiome(pos).unwrapKey()
                .map(key -> key.location().getNamespace().equals(MODID)).orElse(false);
    }
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Carbon dioxide ice intentionally reuses Minecraft's familiar ice texture.
    public static final RegistryObject<Block> CO2_ICE = BLOCKS.register("co2_ice", () ->
            new Co2IceBlock(BlockBehaviour.Properties.copy(Blocks.ICE).mapColor(MapColor.ICE).randomTicks()));
    public static final RegistryObject<Item> CO2_ICE_ITEM = ITEMS.register("co2_ice", () ->
            new BlockItem(CO2_ICE.get(), new Item.Properties()));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> MARS_CO2_ICE_FEATURE = FEATURES.register(
            "mars_co2_ice", () -> new MarsCo2IceFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<CreativeModeTab> MARS_TERRA_TAB = CREATIVE_MODE_TABS.register("mars_terra", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .title(net.minecraft.network.chat.Component.translatable("itemGroup.marsmodterra.mars_terra"))
            .icon(() -> CO2_ICE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(CO2_ICE_ITEM.get());
            }).build());

    public MarsModTerra(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        FEATURES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        WORLD_CARVERS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(MarsTerrainSanitizer.class);

        modEventBus.addListener(this::addCreative);

    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("MarsModTerra is ready: a silent world of five Mars biomes.");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
        {
            event.accept(CO2_ICE_ITEM);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("mars")
                .executes(context -> {
                    var player = context.getSource().getPlayerOrException();
                    var mars = player.server.getLevel(MARS_LEVEL);
                    if (mars == null)
                        return 0;

                    int surfaceY = mars.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 0, 0);
                    player.teleportTo(mars, 0.5D, surfaceY + 1.0D, 0.5D, player.getYRot(), player.getXRot());
                    return 1;
                }));
    }

    @SubscribeEvent
    public void onMarsMobSpawn(MobSpawnEvent.SpawnPlacementCheck event)
    {
        if (isMarsWorld(event.getLevel().getLevel(), event.getPos()))
            event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public void onMarsEntityJoin(EntityJoinLevelEvent event)
    {
        // Also blocks spawners or modded spawn systems from introducing mobs into lifeless Mars.
        if (isMarsWorld(event.getLevel(), event.getEntity().blockPosition())
                && event.getEntity() instanceof net.minecraft.world.entity.Mob)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("MarsModTerra server world generation is active.");
    }

}
