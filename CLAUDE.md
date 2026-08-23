# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A [NeoForge](https://neoforged.net/) mod for **Minecraft 1.21.1**, built on the NeoForge MDK template. It is a Cosmere-themed mod: mod id `cosmere`, package `com.cosmere`. The content is still largely the MDK example (an example block, item, and creative tab) awaiting replacement with real Cosmere features.

- Java 21 (Mojang ships Java 21 for 1.21.1; the toolchain enforces this)
- NeoForge `21.1.248`, Parchment mappings `2024.11.17` for readable Minecraft parameter names
- Gradle 9.2.1 via the [ModDevGradle](https://github.com/neoforged/ModDevGradle) (`net.neoforged.moddev`) plugin

## Commands

Use the Gradle wrapper (`./gradlew`) for everything.

- `./gradlew build` — compile, run data generation checks, and produce the mod jar in `build/libs/`
- `./gradlew runClient` — launch a Minecraft client with the mod loaded (run dir: `run/`)
- `./gradlew runServer` — launch a dedicated server (`--nogui`)
- `./gradlew runData` — run data generators; output goes to `src/generated/resources/` (this dir is folded into the main resources source set)
- `./gradlew runGameTestServer` — run all registered game tests headlessly and exit
- `./gradlew --refresh-dependencies` — refresh the dependency cache if the IDE is missing libraries
- `./gradlew clean` — reset build outputs (does not touch your source)

### Tests / Game tests

Tests are NeoForge **game tests**, not JUnit. They run inside a Minecraft instance. The enabled namespaces are set by `neoforge.enabledGameTestNamespaces` (currently `mod_id`). Run the full suite with `./gradlew runGameTestServer`, or use the in-game `/test` command inside `runClient`/`runServer`. Individual tests are selected via the `/test` command in-game rather than a Gradle flag.

## Architecture

### Registration pattern

Content is registered through NeoForge's `DeferredRegister` system, all keyed to the mod id namespace. See `Cosmere` for the canonical pattern:

- Static `DeferredRegister.Blocks` / `.Items` / `DeferredRegister<CreativeModeTab>` fields hold registry objects.
- Each is registered to the **mod event bus** in the `@Mod`-annotated class constructor. Objects are not live until the registry events fire, so always access them via `.get()` inside setup/runtime code, never at class-init time.
- The `@Mod(MODID)` class constructor is the entry point; FML injects `IEventBus` (mod event bus) and `ModContainer` automatically.

### Two mod entry points (dist separation)

- `Cosmere` — the main `@Mod` class, common (both client and server). Registers content, config, and lifecycle listeners.
- `CosmereClient` — `@Mod(value = ..., dist = Dist.CLIENT)`, loaded **only on the client**. Put client-only code here (rendering, screens, `Minecraft.getInstance()`). It also uses `@EventBusSubscriber(value = Dist.CLIENT)` to auto-register static `@SubscribeEvent` methods.

Two event buses exist and are easy to confuse:
- **Mod event bus** (`IEventBus` passed to the constructor): lifecycle/registration events (`FMLCommonSetupEvent`, `FMLClientSetupEvent`, `BuildCreativeModeTabContentsEvent`, registry events).
- **`NeoForge.EVENT_BUS`** (global): gameplay/server events (`ServerStartingEvent`, etc.). A class must be registered to it explicitly (or via `@EventBusSubscriber`) to receive these.

### Config

`Config.java` builds a `ModConfigSpec` (COMMON type) registered in the mod constructor via `modContainer.registerConfig(...)`. NeoForge generates and loads the TOML file automatically (see `run/config/cosmere-common.toml`). `CosmereClient` wires up `IConfigScreenFactory` so the config is editable from the in-game Mods screen — new config options need matching keys in `lang/en_us.json`.

### Mod metadata is generated, not static

There is **no** static `neoforge.mods.toml`. `src/main/templates/META-INF/neoforge.mods.toml` is a template; the `generateModMetadata` task in `build.gradle` expands `${...}` placeholders (mod id, version, MC/Neo version ranges, license) from `gradle.properties` at build/IDE-sync time. **Edit the template and `gradle.properties`, not any generated file under `build/`.**

## Naming (keep in sync)

The mod id `cosmere` is threaded through several coupled places. If it ever changes again, update all of them together:

1. `gradle.properties` — `mod_id`, `mod_name`, `mod_group_id`
2. `MODID` constant in `Cosmere` (must equal `mod_id`)
3. The Java package `com.cosmere` and source directory layout
4. `src/main/resources/assets/cosmere/lang/en_us.json` and any other `assets/cosmere/...` paths
5. Data gen output namespace under `src/generated/resources/`

`mod_id` must match the regex `[a-z][a-z0-9_]{1,63}`.

## CI

`.github/workflows/build.yml` runs `./gradlew build` on every push and pull request using JDK 21 (Temurin).
