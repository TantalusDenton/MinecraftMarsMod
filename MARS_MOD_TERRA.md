# MarsModTerra — The Lifeless Red Planet

MarsModTerra adds a selectable **Mars — Lifeless Red Planet** world type to the Create New World screen and also provides a standalone Mars dimension, entered with `/mars`.

The world is deliberately silent and dry. There is no water, dirt, grass, vegetation, passive wildlife, hostile wildlife, or biome decoration. Natural cave and canyon carving remains active, leaving open caverns beneath the surface.

Its five world-generation biomes use only the Mars palette:

- **Mars Icy Plains** — granite and orange terracotta polar ground, scattered with CO2 ice.
- **Mars Canyons** — deep red terracotta cuts and orange terracotta strata.
- **Mars Red Desert** — loose red sand over red terracotta.
- **Mars Granite Mountains** — high granite ridges with orange terracotta lower slopes.
- **Mars Impact Craters** — red terracotta basins with exposed redstone-block impact scars.
- **Cold Basin** — the pull request's cold-region variant, with orange terracotta and CO₂ ice over granite.

Base terrain is generated directly by Minecraft's datapack noise settings, density functions, and biome surface rules. The PR's ordinary and frozen crater carvers run during Minecraft's carving stage, with sizes selected by biome JSON. Frozen crater floors and rims use CO₂ ice. There is no terrain sanitizer.

CO2 ice uses Minecraft's ice texture. At sunrise, blocks exposed to the sky release a brief puff of light smoke and sublimate away.
