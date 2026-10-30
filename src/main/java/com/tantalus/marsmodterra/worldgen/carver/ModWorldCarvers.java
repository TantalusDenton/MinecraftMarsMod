package com.tantalus.marsmodterra.worldgen.carver;

import com.tantalus.marsmodterra.MarsModTerra;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModWorldCarvers {
  public static final DeferredRegister<WorldCarver<?>> WORLD_CARVERS =
    DeferredRegister.create(
      ForgeRegistries.WORLD_CARVERS,
      MarsModTerra.MODID
    );

  public static final RegistryObject<WorldCarver<CraterCarverConfiguration>> CRATER =
    WORLD_CARVERS.register(
      "crater",
      CraterWorldCarver::new
    );

  public static final RegistryObject<WorldCarver<CraterCarverConfiguration>> FROZEN_CRATER =
    WORLD_CARVERS.register(
      "frozen_crater",
      FrozenCraterWorldCarver::new
    );
}
