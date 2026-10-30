# MarsModTerra

MarsModTerra is a Minecraft Forge 1.20.1 world-generation mod that turns a new world into a silent, lifeless Mars. Choose **Mars — Lifeless Red Planet** from the Create New World screen, or use `/mars` to travel to the standalone Mars dimension.

## World features

- Five core biomes: icy plains, canyons, red desert, granite mountains, and impact craters, plus the cold-basin variant from PR #2.
- Natural caves and canyon carving remain active beneath the surface.
- Terrain uses only granite, red sand, red terracotta, orange terracotta, redstone blocks, and CO₂ ice.
- No grass, dirt, water, plants, or animals are generated or allowed to spawn.
- CO₂ ice uses Minecraft's ice texture. Exposed CO₂ ice releases a light smoke puff and sublimates at sunrise.

## Requirements

- Minecraft 1.20.1
- Minecraft Forge 47.x
- Java 17

## Build

On Windows:

```powershell
./gradlew.bat build
```

On Linux or macOS:

```bash
./gradlew build
```

The finished jar is written to `build/libs/`.

## Terrain generation and customization

Mars uses Minecraft's built-in datapack world-generation system. Both the Mars world preset and standalone dimension use `marsmodterra:mars` noise settings. There is no chunk-load terrain replacement.

Resources under `src/main/resources/data/marsmodterra/` control generation:

- `worldgen/noise_settings/mars.json`: dry granite base terrain, biome surface materials, disabled aquifers and ore veins.
- `worldgen/density_function/`: terrain elevation and ridges.
- `worldgen/configured_carver/`: vanilla dry cave/canyon carvers and the PR's small, medium, large, and huge ordinary/frozen crater configurations.
- `worldgen/biome/`: climate, empty spawn lists, caves, and surface ice placement.
- `worldgen/configured_feature/` and `worldgen/placed_feature/`: vanilla simple-block features that place CO₂ ice in icy regions.

The PR's `CraterWorldCarver` and `FrozenCraterWorldCarver` are registered with Minecraft's carver system and selected by biome JSON during generation. Their replaceable-material tag includes the Martian terrain palette. Frozen crater floors and rims use CO₂ ice; ordinary rims use red sand, red terracotta, and orange terracotta.

| Biome | Crater configurations |
| --- | --- |
| Impact craters | Small, medium, large, huge ordinary |
| Desert | Small ordinary |
| Canyons | Small and medium ordinary |
| Icy plains | Small and medium frozen |
| Cold basin | Small, medium, large, huge frozen |
| Mountains | None |

Noise settings and surface rules still generate the base terrain directly. Java also handles the CO₂ block's sunrise behavior, registration, teleportation, and mob prevention.

Create a new world to see the generation changes consistently. Existing chunks retain their terrain; newly explored chunks use the new rules and may have visible borders.

Run the integration checks with `./gradlew.bat runGameTestServer`. They load the datapack registries, check crater assignments in every biome, exercise every configured crater size on fresh terrain, generate a chunk in each of the six biomes, check the block palette and absence of fluids, verify caverns, and exercise CO₂ sublimation. Test saves live under `build/gametest/`.

## License

Mod code: All Rights Reserved, as declared in the mod metadata. [LICENSE.txt](LICENSE.txt) and [CREDITS.txt](CREDITS.txt) retain the Forge MDK's third-party notices.
