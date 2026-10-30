package com.tantalus.marsmodterra.worldgen.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;

public final class CraterCarverConfiguration extends CarverConfiguration {
  public static final Codec<CraterCarverConfiguration> CODEC = RecordCodecBuilder.create(instance ->
    instance.group(
      CarverConfiguration.CODEC.forGetter(config -> config),
      Codec.intRange(1, 112).fieldOf("min_radius").forGetter(config -> config.minRadius),
      Codec.intRange(1, 112).fieldOf("max_radius").forGetter(config -> config.maxRadius),
      Codec.floatRange(0.01F, 1.0F).fieldOf("depth_factor").forGetter(config -> config.depthFactor),
      Codec.floatRange(0.0F, 1.0F).fieldOf("rim_width_factor").forGetter(config -> config.rimWidthFactor),
      Codec.floatRange(0.0F, 1.0F).fieldOf("rim_height_factor").forGetter(config -> config.rimHeightFactor),
      Codec.floatRange(0.0F, 0.5F).fieldOf("irregularity").forGetter(config -> config.irregularity)
    ).apply(instance, CraterCarverConfiguration::new)
  );

  public final int minRadius;
  public final int maxRadius;
  public final float depthFactor;
  public final float rimWidthFactor;
  public final float rimHeightFactor;
  public final float irregularity;

  public CraterCarverConfiguration(
    CarverConfiguration base,
    int minRadius,
    int maxRadius,
    float depthFactor,
    float rimWidthFactor,
    float rimHeightFactor,
    float irregularity
  ) {
    super(
      base.probability,
      base.y,
      base.yScale,
      base.lavaLevel,
      base.debugSettings,
      base.replaceable
    );

    this.minRadius = minRadius;
    this.maxRadius = maxRadius;
    this.depthFactor = depthFactor;
    this.rimWidthFactor = rimWidthFactor;
    this.rimHeightFactor = rimHeightFactor;
    this.irregularity = irregularity;
  }
}
