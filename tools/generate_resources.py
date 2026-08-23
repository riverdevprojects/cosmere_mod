#!/usr/bin/env python3
"""Generates the mod's assets and data from the metal tables.

The mod has twenty-one metals and four filler minerals, each with up to six items and three
blocks. Hand-authoring ~500 JSON files and ~200 sprites is not maintainable, and NeoForge's
datagen would mean writing the same tables again in Java. This script is the single source:
edit the tables at the top, re-run it, and every model, texture, tag, recipe, loot table and
ore feature follows.

Usage:  python3 tools/generate_resources.py
Run it from the repository root. It only writes inside src/main/resources.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import masks
from pngwriter import noise_field, overlay_blobs, render_mask, write_png

MODID = "cosmere"
ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", MODID)
DATA = os.path.join(ROOT, "data", MODID)

# (id, colour, source) -- must stay in step with Metal.java.
METALS = [
    ("iron", 0xD8D8D8, "vanilla"),
    ("steel", 0x8C8C8C, "alloy"),
    ("tin", 0xE4EEF2, "ore"),
    ("pewter", 0xA9A9B4, "alloy"),
    ("zinc", 0xBFC9CC, "ore"),
    ("brass", 0xD6B44A, "alloy"),
    ("copper", 0xC17E4C, "vanilla"),
    ("bronze", 0xB07B3A, "alloy"),
    ("cadmium", 0xB9C4B0, "ore"),
    ("bendalloy", 0xE0D9A8, "alloy"),
    ("gold", 0xF5D14A, "vanilla"),
    ("electrum", 0xF0E2A0, "alloy"),
    ("aluminum", 0xDCE2E5, "ore"),
    ("duralumin", 0xC9CED2, "alloy"),
    ("chromium", 0xB6C6CE, "ore"),
    ("nicrosil", 0xA8B8B0, "alloy"),
    ("atium", 0x6E6E86, "ore"),
    ("lerasium", 0xE8E4D0, "none"),
    ("malatium", 0x9A8E7A, "alloy"),
    ("harmonium", 0xD0D6E4, "alloy"),
    ("trellium", 0x8C4A4A, "none"),
]

MINERALS = [
    ("lead", 0x6E6E7A),
    ("silver", 0xE0E6EA),
    ("nickel", 0xC6C4A8),
    ("bismuth", 0xC0A0C8),
]

GOD_METALS = {"atium", "lerasium", "malatium", "harmonium", "trellium"}
DEEP_ONLY = {"atium"}

STONE = 0x7A7A7A
DEEPSLATE = 0x4A4A4E

lang = {}


# --------------------------------------------------------------------------- helpers

def title(text):
    return " ".join(word.capitalize() for word in text.split("_"))


def rgb(packed):
    return ((packed >> 16) & 0xFF, (packed >> 8) & 0xFF, packed & 0xFF)


def write_json(path, payload):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as handle:
        json.dump(payload, handle, indent=2)
        handle.write("\n")


def sprite(name, mask, colour):
    path = os.path.join(ASSETS, "textures", "item", name + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    write_png(path, render_mask(mask, rgb(colour)), 16, 16)


def block_texture(name, pixels):
    path = os.path.join(ASSETS, "textures", "block", name + ".png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    write_png(path, pixels, 16, 16)


def item_model(name, texture=None, parent="minecraft:item/generated"):
    write_json(os.path.join(ASSETS, "models", "item", name + ".json"), {
        "parent": parent,
        "textures": {"layer0": f"{MODID}:item/{texture or name}"},
    })


def handheld_model(name):
    item_model(name, parent="minecraft:item/handheld")


def block_item_model(name):
    write_json(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": f"{MODID}:block/{name}"})


def cube_all(name, texture=None):
    write_json(os.path.join(ASSETS, "models", "block", name + ".json"), {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"{MODID}:block/{texture or name}"},
    })
    write_json(os.path.join(ASSETS, "blockstates", name + ".json"), {
        "variants": {"": {"model": f"{MODID}:block/{name}"}}
    })


def simple_block_loot(name):
    write_json(os.path.join(DATA, "loot_table", "blocks", name + ".json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MODID}:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })


def ore_loot(name, drop, min_count=1, max_count=1):
    """Silk touch keeps the ore; anything else yields the raw drop, boosted by Fortune."""
    entry = {
        "type": "minecraft:item",
        "name": drop,
        "functions": [
            {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
             "formula": "minecraft:ore_drops"},
            {"function": "minecraft:explosion_decay"},
        ],
    }
    if max_count > 1:
        entry["functions"].insert(0, {
            "function": "minecraft:set_count",
            "count": {"type": "minecraft:uniform", "min": min_count, "max": max_count},
        })
    write_json(os.path.join(DATA, "loot_table", "blocks", name + ".json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{
                "type": "minecraft:alternatives",
                "children": [
                    {
                        "type": "minecraft:item",
                        "name": f"{MODID}:{name}",
                        "conditions": [{
                            "condition": "minecraft:match_tool",
                            "predicate": {"predicates": {"minecraft:enchantments": [
                                {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}
                            ]}},
                        }],
                    },
                    entry,
                ],
            }],
        }],
    })


def shaped(name, pattern, key, result, count=1, category="misc"):
    write_json(os.path.join(DATA, "recipe", name + ".json"), {
        "type": "minecraft:crafting_shaped",
        "category": category,
        "pattern": pattern,
        "key": {symbol: {"item": item} for symbol, item in key.items()},
        "result": {"id": result, "count": count},
    })


def shapeless(name, ingredients, result, count=1, category="misc"):
    write_json(os.path.join(DATA, "recipe", name + ".json"), {
        "type": "minecraft:crafting_shapeless",
        "category": category,
        "ingredients": [{"item": item} for item in ingredients],
        "result": {"id": result, "count": count},
    })


def cooking(name, kind, ingredient, result, experience=0.7, time=200):
    write_json(os.path.join(DATA, "recipe", name + ".json"), {
        "type": f"minecraft:{kind}",
        "category": "misc",
        "ingredient": {"item": ingredient},
        "result": {"id": result, "count": 1},
        "experience": experience,
        "cookingtime": time,
    })


# ------------------------------------------------------------------------ generation

def gen_metal(metal_id, colour, source):
    is_god = metal_id in GOD_METALS
    has_ingot = source != "vanilla"
    has_ore = source == "ore"

    if has_ingot:
        for suffix, mask in (("ingot", masks.INGOT), ("nugget", masks.NUGGET)):
            name = f"{metal_id}_{suffix}"
            sprite(name, mask, colour)
            item_model(name)
            lang[f"item.{MODID}.{name}"] = f"{title(metal_id)} {suffix.capitalize()}"

        block = f"{metal_id}_block"
        block_texture(block, noise_field(rgb(colour), hash(block) & 0xFFFF, spread=0.10))
        cube_all(block)
        block_item_model(block)
        simple_block_loot(block)
        lang[f"block.{MODID}.{block}"] = f"Block of {title(metal_id)}"

        shaped(f"{block}_from_ingots", ["III", "III", "III"], {"I": f"{MODID}:{metal_id}_ingot"},
               f"{MODID}:{block}", category="building")
        shapeless(f"{metal_id}_ingot_from_block", [f"{MODID}:{block}"], f"{MODID}:{metal_id}_ingot", 9)
        shaped(f"{metal_id}_ingot_from_nuggets", ["NNN", "NNN", "NNN"], {"N": f"{MODID}:{metal_id}_nugget"},
               f"{MODID}:{metal_id}_ingot")
        shapeless(f"{metal_id}_nugget_from_ingot", [f"{MODID}:{metal_id}_ingot"], f"{MODID}:{metal_id}_nugget", 9)

    if has_ore:
        raw = f"raw_{metal_id}"
        sprite(raw, masks.RAW, colour)
        item_model(raw)
        lang[f"item.{MODID}.{raw}"] = f"Raw {title(metal_id)}"

        variants = [] if metal_id in DEEP_ONLY else [("", STONE)]
        variants.append(("deepslate_", DEEPSLATE))
        for prefix, base in variants:
            ore = f"{prefix}{metal_id}_ore"
            pixels = overlay_blobs(noise_field(rgb(base), hash(ore) & 0xFFFF), rgb(colour),
                                   (hash(ore) >> 8) & 0xFFFF, count=11 if is_god else 9)
            block_texture(ore, pixels)
            cube_all(ore)
            block_item_model(ore)
            ore_loot(ore, f"{MODID}:{raw}", 1, 3 if is_god else 1)
            lang[f"block.{MODID}.{ore}"] = (
                f"Deepslate {title(metal_id)} Ore" if prefix else f"{title(metal_id)} Ore")

        cooking(f"{metal_id}_ingot_from_smelting", "smelting", f"{MODID}:{raw}", f"{MODID}:{metal_id}_ingot",
                1.0 if is_god else 0.7)
        cooking(f"{metal_id}_ingot_from_blasting", "blasting", f"{MODID}:{raw}", f"{MODID}:{metal_id}_ingot",
                1.0 if is_god else 0.7, 100)

    # Every metal gets a vial, a spike, a ring and a bracer. Lerasium is swallowed as a bead
    # instead of drunk, so it is the one metal without a vial.
    if metal_id != "lerasium":
        vial = f"{metal_id}_vial"
        sprite(vial, masks.VIAL, colour)
        item_model(vial)
        lang[f"item.{MODID}.{vial}"] = f"Vial of {title(metal_id)}"
        shapeless(vial, ["minecraft:glass_bottle", nugget_of(metal_id), nugget_of(metal_id), nugget_of(metal_id)],
                  f"{MODID}:{vial}")

    spike = f"{metal_id}_spike"
    sprite(spike, masks.SPIKE, colour)
    handheld_model(spike)
    lang[f"item.{MODID}.{spike}"] = f"{title(metal_id)} Spike"
    shaped(spike, ["N", "N"], {"N": nugget_of(metal_id)}, f"{MODID}:{spike}")

    ring = f"{metal_id}_ring"
    sprite(ring, masks.RING, colour)
    item_model(ring)
    lang[f"item.{MODID}.{ring}"] = f"{title(metal_id)} Ring"
    shaped(ring, [" N ", "N N", " N "], {"N": nugget_of(metal_id)}, f"{MODID}:{ring}")

    bracer = f"{metal_id}_bracer"
    sprite(bracer, masks.BRACER, colour)
    item_model(bracer)
    lang[f"item.{MODID}.{bracer}"] = f"{title(metal_id)} Bracer"
    shaped(bracer, ["III", "I I", "III"], {"I": ingot_of(metal_id)}, f"{MODID}:{bracer}")


def nugget_of(metal_id):
    return f"minecraft:{metal_id}_nugget" if metal_id in ("iron", "gold") else (
        f"{MODID}:{metal_id}_nugget" if metal_id != "copper" else f"{MODID}:copper_nugget")


def ingot_of(metal_id):
    return f"minecraft:{metal_id}_ingot" if metal_id in ("iron", "gold", "copper") else f"{MODID}:{metal_id}_ingot"


def gen_mineral(mineral_id, colour):
    for suffix, mask in (("ingot", masks.INGOT), ("nugget", masks.NUGGET)):
        name = f"{mineral_id}_{suffix}"
        sprite(name, mask, colour)
        item_model(name)
        lang[f"item.{MODID}.{name}"] = f"{title(mineral_id)} {suffix.capitalize()}"

    raw = f"raw_{mineral_id}"
    sprite(raw, masks.RAW, colour)
    item_model(raw)
    lang[f"item.{MODID}.{raw}"] = f"Raw {title(mineral_id)}"

    block = f"{mineral_id}_block"
    block_texture(block, noise_field(rgb(colour), hash(block) & 0xFFFF, spread=0.10))
    cube_all(block)
    block_item_model(block)
    simple_block_loot(block)
    lang[f"block.{MODID}.{block}"] = f"Block of {title(mineral_id)}"

    for prefix, base in (("", STONE), ("deepslate_", DEEPSLATE)):
        ore = f"{prefix}{mineral_id}_ore"
        pixels = overlay_blobs(noise_field(rgb(base), hash(ore) & 0xFFFF), rgb(colour), (hash(ore) >> 8) & 0xFFFF)
        block_texture(ore, pixels)
        cube_all(ore)
        block_item_model(ore)
        ore_loot(ore, f"{MODID}:{raw}")
        lang[f"block.{MODID}.{ore}"] = (
            f"Deepslate {title(mineral_id)} Ore" if prefix else f"{title(mineral_id)} Ore")

    cooking(f"{mineral_id}_ingot_from_smelting", "smelting", f"{MODID}:{raw}", f"{MODID}:{mineral_id}_ingot")
    cooking(f"{mineral_id}_ingot_from_blasting", "blasting", f"{MODID}:{raw}", f"{MODID}:{mineral_id}_ingot", 0.7, 100)
    shaped(f"{block}_from_ingots", ["III", "III", "III"], {"I": f"{MODID}:{mineral_id}_ingot"},
           f"{MODID}:{block}", category="building")
    shapeless(f"{mineral_id}_ingot_from_block", [f"{MODID}:{block}"], f"{MODID}:{mineral_id}_ingot", 9)
    shaped(f"{mineral_id}_ingot_from_nuggets", ["NNN", "NNN", "NNN"], {"N": f"{MODID}:{mineral_id}_nugget"},
           f"{MODID}:{mineral_id}_ingot")
    shapeless(f"{mineral_id}_nugget_from_ingot", [f"{MODID}:{mineral_id}_ingot"], f"{MODID}:{mineral_id}_nugget", 9)


def gen_misc_items():
    sprite("clip", masks.COIN, 0xC17E4C)
    item_model("clip")
    lang[f"item.{MODID}.clip"] = "Clip"
    sprite("boxing", masks.COIN, 0xF5D14A)
    item_model("boxing")
    lang[f"item.{MODID}.boxing"] = "Boxing"
    shapeless("clip_from_copper", ["minecraft:copper_ingot"], f"{MODID}:clip", 9)
    shapeless("boxing_from_gold", ["minecraft:gold_ingot"], f"{MODID}:boxing", 4)

    sprite("blindfold", masks.BLINDFOLD, 0x2B2B33)
    item_model("blindfold")
    lang[f"item.{MODID}.blindfold"] = "Blindfold"
    shaped("blindfold", ["SWS"], {"S": "minecraft:string", "W": "minecraft:black_wool"}, f"{MODID}:blindfold")

    sprite("spike_jar", masks.JAR, 0x7A2020)
    item_model("spike_jar")
    lang[f"item.{MODID}.spike_jar"] = "Jar of Spikes"
    shaped("spike_jar", [" G ", "GRG", " G "],
           {"G": "minecraft:glass", "R": "minecraft:redstone"}, f"{MODID}:spike_jar")

    sprite("koloss_skin", masks.SKIN, 0x3B6FA0)
    item_model("koloss_skin")
    lang[f"item.{MODID}.koloss_skin"] = "Koloss Skin"

    sprite("lerasium_bead", masks.BEAD, 0xE8E4D0)
    item_model("lerasium_bead")
    lang[f"item.{MODID}.lerasium_bead"] = "Bead of Lerasium"
    shapeless("lerasium_bead", [f"{MODID}:lerasium_nugget", f"{MODID}:lerasium_nugget"], f"{MODID}:lerasium_bead")

    sprite("atium_bead", masks.BEAD, 0x6E6E86)
    item_model("atium_bead")
    lang[f"item.{MODID}.atium_bead"] = "Bead of Atium"
    shapeless("atium_bead", [f"{MODID}:atium_nugget", f"{MODID}:atium_nugget"], f"{MODID}:atium_bead")

    weapons = [
        ("glass_dagger", masks.DAGGER, 0xBFE6E8, "Glass Dagger"),
        ("glass_sword", masks.SWORD, 0xBFE6E8, "Glass Sword"),
        ("obsidian_dagger", masks.DAGGER, 0x3A2E4A, "Obsidian Dagger"),
        ("obsidian_sword", masks.SWORD, 0x3A2E4A, "Obsidian Sword"),
        ("obsidian_axe", masks.AXE, 0x3A2E4A, "Obsidian Axe"),
        ("steel_dagger", masks.DAGGER, 0x8C8C8C, "Steel Dagger"),
        ("dueling_cane", masks.CANE, 0x8A6A3A, "Duelling Cane"),
    ]
    for name, mask, colour, display in weapons:
        sprite(name, mask, colour)
        handheld_model(name)
        lang[f"item.{MODID}.{name}"] = display

    # A dagger is a stick and one piece of material -- the cheapest real weapon in the mod.
    shapeless("glass_dagger", ["minecraft:stick", "minecraft:glass"], f"{MODID}:glass_dagger")
    shapeless("obsidian_dagger", ["minecraft:stick", "minecraft:obsidian"], f"{MODID}:obsidian_dagger")
    shapeless("steel_dagger", ["minecraft:stick", f"{MODID}:steel_ingot"], f"{MODID}:steel_dagger")
    shaped("glass_sword", ["G", "G", "S"], {"G": "minecraft:glass", "S": "minecraft:stick"}, f"{MODID}:glass_sword")
    shaped("obsidian_sword", ["O", "O", "S"], {"O": "minecraft:obsidian", "S": "minecraft:stick"},
           f"{MODID}:obsidian_sword")
    shaped("obsidian_axe", ["OO", "OS", " S"], {"O": "minecraft:obsidian", "S": "minecraft:stick"},
           f"{MODID}:obsidian_axe")
    shaped("dueling_cane", ["  S", " S ", "S  "], {"S": "minecraft:stick"}, f"{MODID}:dueling_cane")

    for egg in ("mistwraith", "kandra", "koloss", "wolfhound"):
        name = f"{egg}_spawn_egg"
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"),
                   {"parent": "minecraft:item/template_spawn_egg"})
        lang[f"item.{MODID}.{name}"] = f"{title(egg)} Spawn Egg"


def gen_tables():
    # Metallurgy Table: a crucible bench. Top, side and bottom differ like a crafting table.
    block_texture("metallurgy_table_top", overlay_blobs(
        noise_field(rgb(0x55565C), 0x1234, spread=0.12), rgb(0xE07030), 0x9876, count=4))
    block_texture("metallurgy_table_side", noise_field(rgb(0x43444A), 0x2345, spread=0.12))
    block_texture("metallurgy_table_bottom", noise_field(rgb(0x33343A), 0x3456, spread=0.10))
    write_json(os.path.join(ASSETS, "models", "block", "metallurgy_table.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "top": f"{MODID}:block/metallurgy_table_top",
            "side": f"{MODID}:block/metallurgy_table_side",
            "bottom": f"{MODID}:block/metallurgy_table_bottom",
        },
    })
    write_json(os.path.join(ASSETS, "blockstates", "metallurgy_table.json"),
               {"variants": {"": {"model": f"{MODID}:block/metallurgy_table"}}})
    block_item_model("metallurgy_table")
    simple_block_loot("metallurgy_table")
    lang[f"block.{MODID}.metallurgy_table"] = "Metallurgy Table"
    shaped("metallurgy_table", ["III", "SFS", "SSS"], {
        "I": "minecraft:iron_ingot", "S": "minecraft:stone_bricks", "F": "minecraft:furnace",
    }, f"{MODID}:metallurgy_table", category="building")

    # Hemalurgic Table: waist-high stone, channelled for blood.
    block_texture("hemalurgic_table_top", overlay_blobs(
        noise_field(rgb(0x6E6A66), 0x4567, spread=0.10), rgb(0x7A1F1F), 0x7654, count=6))
    block_texture("hemalurgic_table_side", noise_field(rgb(0x5C5854), 0x5678, spread=0.10))
    write_json(os.path.join(ASSETS, "models", "block", "hemalurgic_table.json"), {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": f"{MODID}:block/hemalurgic_table_side",
            "top": f"{MODID}:block/hemalurgic_table_top",
            "side": f"{MODID}:block/hemalurgic_table_side",
        },
        "elements": [{
            "from": [0, 0, 0],
            "to": [16, 12, 16],
            "faces": {
                "down": {"texture": "#side", "cullface": "down"},
                "up": {"texture": "#top"},
                "north": {"texture": "#side"},
                "south": {"texture": "#side"},
                "west": {"texture": "#side"},
                "east": {"texture": "#side"},
            },
        }],
    })
    write_json(os.path.join(ASSETS, "blockstates", "hemalurgic_table.json"),
               {"variants": {"": {"model": f"{MODID}:block/hemalurgic_table"}}})
    block_item_model("hemalurgic_table")
    simple_block_loot("hemalurgic_table")
    lang[f"block.{MODID}.hemalurgic_table"] = "Hemalurgic Table"
    shaped("hemalurgic_table", ["SSS", "IPI", "SSS"], {
        "S": "minecraft:polished_andesite", "I": "minecraft:iron_ingot", "P": "minecraft:redstone_block",
    }, f"{MODID}:hemalurgic_table", category="building")


def gen_entity_textures():
    """Flat 64x64 skins. Geometry is vanilla humanoid; only the palette says what it is."""
    palettes = {
        "mistwraith": (0xB8B2A6, 0x8A8479),
        "kandra": (0xE0D3B8, 0xA8917A),
        "koloss": (0x3B6FA0, 0x24466A),
    }
    for name, (light, dark) in palettes.items():
        pixels = []
        for y in range(64):
            for x in range(64):
                base = light if (x // 8 + y // 8) % 2 == 0 else dark
                r, g, b = rgb(base)
                jitter = ((x * 31 + y * 17) % 13 - 6) * 2
                pixels.append((max(0, min(255, r + jitter)),
                               max(0, min(255, g + jitter)),
                               max(0, min(255, b + jitter)), 255))
        path = os.path.join(ASSETS, "textures", "entity", name + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        write_png(path, pixels, 64, 64)


def gen_tags():
    mod_ores = []
    mod_blocks = []
    for metal_id, _, source in METALS:
        if source == "ore":
            if metal_id not in DEEP_ONLY:
                mod_ores.append(f"{MODID}:{metal_id}_ore")
            mod_ores.append(f"{MODID}:deepslate_{metal_id}_ore")
        if source != "vanilla":
            mod_blocks.append(f"{MODID}:{metal_id}_block")
    for mineral_id, _ in MINERALS:
        mod_ores.append(f"{MODID}:{mineral_id}_ore")
        mod_ores.append(f"{MODID}:deepslate_{mineral_id}_ore")
        mod_blocks.append(f"{MODID}:{mineral_id}_block")

    tables = [f"{MODID}:metallurgy_table", f"{MODID}:hemalurgic_table"]

    write_json(os.path.join(ROOT, "data", "minecraft", "tags", "block", "mineable", "pickaxe.json"),
               {"replace": False, "values": mod_ores + mod_blocks + tables})
    write_json(os.path.join(ROOT, "data", "minecraft", "tags", "block", "needs_stone_tool.json"),
               {"replace": False, "values": mod_ores})
    write_json(os.path.join(ROOT, "data", "minecraft", "tags", "block", "needs_iron_tool.json"),
               {"replace": False, "values": mod_blocks})

    # Fixtures an Allomancer can brace against but never shift.
    anchors = [
        "minecraft:iron_door", "minecraft:iron_trapdoor", "minecraft:iron_bars",
        "minecraft:heavy_weighted_pressure_plate", "minecraft:light_weighted_pressure_plate",
        "minecraft:chain", "minecraft:anvil", "minecraft:chipped_anvil", "minecraft:damaged_anvil",
        "minecraft:hopper", "minecraft:cauldron", "minecraft:water_cauldron", "minecraft:lava_cauldron",
        "minecraft:powder_snow_cauldron", "minecraft:lantern", "minecraft:soul_lantern",
        "minecraft:rail", "minecraft:powered_rail", "minecraft:detector_rail", "minecraft:activator_rail",
        "minecraft:blast_furnace", "minecraft:smithing_table", "minecraft:stonecutter",
        "minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "minecraft:bell",
    ]
    write_json(os.path.join(DATA, "tags", "block", "allomantic_anchor.json"),
               {"replace": False, "values": anchors})

    movable_blocks = [
        "minecraft:iron_block", "minecraft:gold_block", "minecraft:copper_block",
        "minecraft:exposed_copper", "minecraft:weathered_copper", "minecraft:oxidized_copper",
        "minecraft:raw_iron_block", "minecraft:raw_gold_block", "minecraft:raw_copper_block",
        "minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:gold_ore",
        "minecraft:deepslate_gold_ore", "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
    ] + mod_ores + mod_blocks
    write_json(os.path.join(DATA, "tags", "block", "allomantically_movable.json"),
               {"replace": False, "values": movable_blocks})

    movable_items = [
        "minecraft:iron_ingot", "minecraft:iron_nugget", "minecraft:gold_ingot", "minecraft:gold_nugget",
        "minecraft:copper_ingot", "minecraft:raw_iron", "minecraft:raw_gold", "minecraft:raw_copper",
        "minecraft:iron_sword", "minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:iron_shovel",
        "minecraft:iron_hoe", "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings",
        "minecraft:iron_boots", "minecraft:bucket", "minecraft:water_bucket", "minecraft:lava_bucket",
        "minecraft:shears", "minecraft:flint_and_steel", "minecraft:chain", "minecraft:iron_block",
        "minecraft:gold_block", "minecraft:minecart", "minecraft:cauldron", "minecraft:hopper",
        "minecraft:anvil", "minecraft:iron_door", "minecraft:iron_trapdoor", "minecraft:iron_bars",
        f"{MODID}:clip", f"{MODID}:boxing", f"{MODID}:steel_dagger", f"{MODID}:dueling_cane",
    ]
    for metal_id, _, source in METALS:
        if source != "vanilla":
            movable_items += [f"{MODID}:{metal_id}_ingot", f"{MODID}:{metal_id}_nugget",
                              f"{MODID}:{metal_id}_block", f"{MODID}:{metal_id}_spike",
                              f"{MODID}:{metal_id}_ring", f"{MODID}:{metal_id}_bracer"]
        else:
            movable_items += [f"{MODID}:{metal_id}_spike", f"{MODID}:{metal_id}_ring",
                              f"{MODID}:{metal_id}_bracer"]
        if source == "ore":
            movable_items.append(f"{MODID}:raw_{metal_id}")
    for mineral_id, _ in MINERALS:
        movable_items += [f"{MODID}:{mineral_id}_ingot", f"{MODID}:{mineral_id}_nugget",
                          f"{MODID}:{mineral_id}_block", f"{MODID}:raw_{mineral_id}"]
    movable_items.append(f"{MODID}:copper_nugget")
    write_json(os.path.join(DATA, "tags", "item", "allomantically_movable.json"),
               {"replace": False, "values": sorted(set(movable_items))})

    # Glass and obsidian: the assassin's answer to Allomancy.
    write_json(os.path.join(DATA, "tags", "item", "allomantically_inert.json"), {
        "replace": False,
        "values": [
            "minecraft:glass", "minecraft:obsidian", "minecraft:crying_obsidian",
            f"{MODID}:glass_dagger", f"{MODID}:glass_sword",
            f"{MODID}:obsidian_dagger", f"{MODID}:obsidian_sword", f"{MODID}:obsidian_axe",
        ],
    })


ORE_PLACEMENTS = {
    # id: (vein size, veins per chunk, min y, max y, deepslate only)
    "tin": (9, 8, -24, 64, False),
    "zinc": (8, 7, -16, 72, False),
    "cadmium": (6, 4, -48, 16, False),
    "aluminum": (7, 5, -32, 48, False),
    "chromium": (5, 3, -56, 8, False),
    "atium": (3, 2, -64, -8, True),
    "lead": (9, 8, -32, 64, False),
    "silver": (7, 5, -40, 40, False),
    "nickel": (6, 4, -48, 24, False),
    "bismuth": (5, 3, -56, 16, False),
}


def gen_worldgen():
    features = []
    for name, (size, count, min_y, max_y, deep_only) in ORE_PLACEMENTS.items():
        targets = []
        if not deep_only:
            targets.append({
                "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
                "state": {"Name": f"{MODID}:{name}_ore"},
            })
        targets.append({
            "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
            "state": {"Name": f"{MODID}:deepslate_{name}_ore"},
        })
        write_json(os.path.join(DATA, "worldgen", "configured_feature", f"ore_{name}.json"), {
            "type": "minecraft:ore",
            "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": targets},
        })
        write_json(os.path.join(DATA, "worldgen", "placed_feature", f"ore_{name}.json"), {
            "feature": f"{MODID}:ore_{name}",
            "placement": [
                {"type": "minecraft:count", "count": count},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {
                    "type": "minecraft:trapezoid",
                    "min_inclusive": {"absolute": min_y},
                    "max_inclusive": {"absolute": max_y},
                }},
                {"type": "minecraft:biome"},
            ],
        })
        features.append(f"{MODID}:ore_{name}")

    write_json(os.path.join(DATA, "neoforge", "biome_modifier", "add_scadrian_ores.json"), {
        "type": "neoforge:add_features",
        "biomes": "#minecraft:is_overworld",
        "features": features,
        "step": "underground_ores",
    })


ALLOMANCY_TEXT = {
    "iron": "Lurcher. Pulls on nearby metal, and shows you where all of it is.",
    "steel": "Coinshot. Pushes on nearby metal, and shows you where all of it is.",
    "tin": "Tineye. Opens the senses wide. Daylight becomes unbearable without a blindfold.",
    "pewter": "Thug. Enormous strength and endurance, paid for the moment you stop.",
    "zinc": "Rioter. Inflames the emotions of everything nearby.",
    "brass": "Soother. Dampens the emotions of everything nearby.",
    "copper": "Smoker. Hides you and yours from a Seeker.",
    "bronze": "Seeker. Hears the rhythms of Allomancy in use.",
    "cadmium": "Pulser. Drags time to a crawl.",
    "bendalloy": "Slider. Speeds time up around you and slows it for everyone else.",
    "gold": "Augur. Shows you who you might have been.",
    "electrum": "Oracle. Shows you the futures you have not taken yet.",
    "aluminum": "Aluminum Gnat. Scours every other metal out of you.",
    "duralumin": "Duralumin Gnat. Burns everything else at once, then goes out.",
    "chromium": "Leecher. Strips another Allomancer's metals at a touch.",
    "nicrosil": "Nicroburst. Dumps everything another Allomancer is burning, all at once.",
    "atium": "Sees a second into the future. Nothing living can touch you.",
    "lerasium": "The body of Preservation. Burning it makes you Mistborn.",
    "malatium": "The Eleventh Metal. Shows you who someone else used to be.",
    "harmonium": "Raw Investiture. Stands in for whatever metal you lack.",
    "trellium": "Shields the spiritweb from emotional Allomancy and from a Shard's attention.",
}

FERUCHEMY_TEXT = {
    "iron": "Stores physical weight.",
    "steel": "Stores physical speed.",
    "tin": "Stores the senses.",
    "pewter": "Stores physical strength.",
    "zinc": "Stores mental speed.",
    "brass": "Stores body warmth.",
    "copper": "Stores memories.",
    "bronze": "Stores wakefulness.",
    "cadmium": "Stores breath.",
    "bendalloy": "Stores calories.",
    "gold": "Stores health.",
    "electrum": "Stores determination.",
    "chromium": "Stores fortune.",
    "nicrosil": "Stores Investiture itself.",
    "aluminum": "Stores Identity. Fill a metalmind while storing and anyone can tap it.",
    "duralumin": "Stores spiritual Connection.",
    "atium": "Stores age.",
    "lerasium": "Stores Allomantic potential.",
    "malatium": "Stores the shape of another life.",
    "harmonium": "Stores Investiture from either Shard.",
    "trellium": "Stores nothing willingly.",
}

HEMALURGY_TEXT = {
    "iron": "Steals physical strength.",
    "steel": "Steals Physical Allomancy.",
    "tin": "Steals the senses.",
    "pewter": "Steals Physical Feruchemy.",
    "zinc": "Steals emotional fortitude.",
    "brass": "Steals Mental Feruchemy.",
    "copper": "Steals mental fortitude.",
    "bronze": "Steals Mental Allomancy.",
    "cadmium": "Steals Temporal Allomancy.",
    "bendalloy": "Steals Spiritual Feruchemy.",
    "gold": "Steals Hybrid Feruchemy.",
    "electrum": "Steals Enhancement Allomancy.",
    "chromium": "Steals fortune.",
    "nicrosil": "Steals raw Investiture, whatever its source.",
    "aluminum": "Steals everything from the recipient instead of granting anything.",
    "duralumin": "Steals Connection and Identity.",
    "atium": "Steals any attribute at all.",
    "lerasium": "Steals every ability the victim had.",
    "malatium": "Steals a life that was never lived.",
    "harmonium": "Steals whatever both Shards can reach.",
    "trellium": "Grants immunity to emotional Allomancy and hides you from Shards.",
}

SPIKE_SLOTS = {
    "left_ear": "Left Ear", "right_ear": "Right Ear", "left_eye": "Left Eye", "right_eye": "Right Eye",
    "left_shoulder": "Left Shoulder", "right_shoulder": "Right Shoulder", "left_arm": "Left Arm",
    "right_arm": "Right Arm", "chest": "Chest", "left_ribs": "Left Ribs", "right_ribs": "Right Ribs",
    "abdomen": "Abdomen", "spine": "Spine", "left_leg": "Left Leg", "right_leg": "Right Leg",
}

STOLEN_KINDS = {
    "allomantic_power": ("Allomancy", "%s Allomancy"),
    "feruchemic_power": ("Feruchemy", "%s Feruchemy"),
    "physical_strength": ("Physical Strength", "Physical Strength"),
    "senses": ("Senses", "Senses"),
    "emotional_fortitude": ("Emotional Fortitude", "Emotional Fortitude"),
    "mental_fortitude": ("Mental Fortitude", "Mental Fortitude"),
    "fortune": ("Fortune", "Fortune"),
    "investiture": ("Investiture", "Investiture"),
    "connection": ("Connection", "Connection"),
    "void": ("Nothing", "Nothing"),
    "everything": ("Everything", "Everything"),
}


def gen_lang():
    lang["itemGroup.cosmere.materials"] = "Cosmere: Materials"
    lang["itemGroup.cosmere.arts"] = "Cosmere: Metallic Arts"
    lang["itemGroup.cosmere.gear"] = "Cosmere: Gear"

    for metal_id, _, _ in METALS:
        lang[f"cosmere.metal.{metal_id}"] = title(metal_id)
        lang[f"cosmere.allomancy.{metal_id}"] = ALLOMANCY_TEXT[metal_id]
        lang[f"cosmere.feruchemy.{metal_id}"] = FERUCHEMY_TEXT[metal_id]
        lang[f"cosmere.hemalurgy.{metal_id}"] = HEMALURGY_TEXT[metal_id]

    for category in ("physical", "mental", "temporal", "enhancement", "spiritual", "hybrid", "god"):
        lang[f"cosmere.metal_category.{category}"] = title(category)

    for mode, text in (("off", "Off"), ("storing", "Storing"), ("tapping", "Tapping")):
        lang[f"cosmere.feruchemy.mode.{mode}"] = text

    for slot, text in SPIKE_SLOTS.items():
        lang[f"cosmere.spike_slot.{slot}"] = text

    for kind, (plain, of_form) in STOLEN_KINDS.items():
        lang[f"cosmere.hemalurgy.kind.{kind}"] = plain
        lang[f"cosmere.hemalurgy.kind.{kind}.of"] = of_form

    for blessing, text in (("potency", "Potency"), ("agility", "Agility"),
                           ("presence", "Presence"), ("awareness", "Awareness")):
        lang[f"cosmere.blessing.{blessing}"] = f"Blessing of {text}"

    for planet in ("scadrial", "roshar", "nalthis", "sel", "taldain"):
        lang[f"cosmere.planet.{planet}"] = title(planet)

    lang.update({
        "entity.cosmere.mistwraith": "Mistwraith",
        "entity.cosmere.kandra": "Kandra",
        "entity.cosmere.kandra.named": "Kandra of the %s",
        "entity.cosmere.koloss": "Koloss",
        "entity.cosmere.wolfhound": "Wolfhound",
        "entity.cosmere.coin_projectile": "Pushed Coin",

        "container.cosmere.metallurgy_table": "Metallurgy Table",
        "container.cosmere.spike_jar": "Jar of Spikes",

        "key.categories.cosmere": "Cosmere",
        "key.cosmere.toggle_armed": "Arm Push/Pull",
        "key.cosmere.burn_window": "Burn Window",
        "key.cosmere.flare": "Flare Metals",

        "cosmere.hud.armed": "ARMED",
        "cosmere.hud.pewter_drag": "Pewter is holding this back",

        "cosmere.screen.burn_window": "Metals",
        "cosmere.screen.burn_window.hint": "Left: burn. Right: store or tap.",
        "cosmere.screen.burn_window.no_powers": "You have no Investiture to spend.",
        "cosmere.screen.spike_jar.capacity": "Spikes: %s / %s",

        "cosmere.tooltip.dagger_backstab": "Devastating from behind.",
        "cosmere.tooltip.blindfold": "Blinds you, unless tin has already done it worse.",
        "cosmere.tooltip.vial": "Metal flakes in alcohol: %s",
        "cosmere.tooltip.metalmind_charge": "Charge: %s / %s",
        "cosmere.tooltip.metalmind_unkeyed": "Unkeyed - anyone may tap this",
        "cosmere.tooltip.metalmind_keyed": "Keyed to its owner's Identity",
        "cosmere.tooltip.spike_charged": "Charged: %s",
        "cosmere.tooltip.spike_blank": "Blank. It has taken nothing yet.",
        "cosmere.tooltip.spike_jar": "Spikes: %s / %s",
        "cosmere.tooltip.coin_value": "Worth %s emeralds to a trader",
        "cosmere.tooltip.koloss_skin": "Lay it over a spiked body on a Hemalurgic Table.",
        "cosmere.tooltip.lerasium": "Swallow it and you are Mistborn. There is no second bead.",

        "cosmere.message.armed": "Allomancy armed",
        "cosmere.message.disarmed": "Allomancy disarmed",
        "cosmere.message.became_mistborn": "The mists know your name. You are Mistborn.",
        "cosmere.message.became_koloss": "The skin takes. You are koloss now.",
        "cosmere.message.snapped": "Something tears loose inside you. You can burn %s.",
        "cosmere.message.table_empty": "Nothing is bound to the table.",
        "cosmere.message.table_ready": "%s is bound to the table.",
        "cosmere.message.spike_already_charged": "That spike already holds a charge.",
        "cosmere.message.spike_blank_no_victim": "A blank spike needs a victim on the table.",
        "cosmere.message.nothing_to_steal": "%s has nothing this spike can take.",
        "cosmere.message.no_room_for_spike": "Your body will not take another spike there.",
        "cosmere.message.spike_placed": "The spike goes into your %s.",
        "cosmere.message.spiritually_shielded": "Your Identity turns the spike aside.",
        "cosmere.message.spiritweb_torn": "Your spiritweb comes apart.",
        "cosmere.message.leech_blocked": "Their Identity is armoured. Nothing comes loose.",
        "cosmere.message.leeched": "Your metals are gone.",
        "cosmere.message.nicroburst": "Everything you were burning goes at once.",
        "cosmere.message.needs_iron_spikes": "You need %s more iron spikes through the ribs.",

        "cosmere.configuration.title": "Cosmere",
        "cosmere.configuration.section.cosmere.common.toml": "Cosmere",
        "cosmere.configuration.section.cosmere.common.toml.title": "Cosmere",
        "cosmere.configuration.mists": "The Mists",
        "cosmere.configuration.allomancy": "Allomancy",
        "cosmere.configuration.hemalurgy": "Hemalurgy",
        "cosmere.configuration.mistsEnabled": "Mists Enabled",
        "cosmere.configuration.snappingEnabled": "Snapping Enabled",
        "cosmere.configuration.mistwraithSpawnChance": "Mistwraith Spawn Chance",
        "cosmere.configuration.mistwraithCap": "Mistwraith Cap",
        "cosmere.configuration.pushStrength": "Push Strength",
        "cosmere.configuration.blueLineRange": "Blue Line Range",
        "cosmere.configuration.allowLeechingPlayers": "Allow Leeching Players",
        "cosmere.configuration.spikesKillOverLimit": "Spikes Kill Over Limit",
    })

    write_json(os.path.join(ASSETS, "lang", "en_us.json"), dict(sorted(lang.items())))


def gen_copper_nugget():
    """Vanilla has no copper nugget, and Feruchemy needs one for copperminds."""
    sprite("copper_nugget", masks.NUGGET, 0xC17E4C)
    item_model("copper_nugget")
    lang[f"item.{MODID}.copper_nugget"] = "Copper Nugget"
    shapeless("copper_nugget_from_ingot", ["minecraft:copper_ingot"], f"{MODID}:copper_nugget", 9)
    shaped("copper_ingot_from_nuggets", ["NNN", "NNN", "NNN"], {"N": f"{MODID}:copper_nugget"},
           "minecraft:copper_ingot")


def gen_entity_loot():
    """What the mod's mobs leave behind."""
    def table(name, pools):
        write_json(os.path.join(DATA, "loot_table", "entities", name + ".json"),
                   {"type": "minecraft:entity", "pools": pools})

    def pool(entries, rolls=1):
        return {"rolls": rolls, "bonus_rolls": 0, "entries": entries}

    def item(name, minimum, maximum, looting=True):
        functions = [{"function": "minecraft:set_count",
                      "count": {"type": "minecraft:uniform", "min": minimum, "max": maximum}}]
        if looting:
            functions.append({"function": "minecraft:enchanted_count_increase",
                              "enchantment": "minecraft:looting",
                              "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
        return {"type": "minecraft:item", "name": name, "functions": functions}

    # A mistwraith is a bag of other people's skeletons, and comes apart like one.
    table("mistwraith", [
        pool([item("minecraft:bone", 6, 12)]),
        pool([item("minecraft:rotten_flesh", 4, 9)]),
    ])
    table("kandra", [pool([item("minecraft:bone", 2, 5)])])
    table("koloss", [
        pool([item(f"{MODID}:koloss_skin", 1, 2)]),
        pool([item("minecraft:obsidian", 0, 2)]),
        pool([{"type": "minecraft:item", "name": f"{MODID}:iron_spike",
               "conditions": [{"condition": "minecraft:random_chance", "chance": 0.35}]}]),
    ])
    table("wolfhound", [pool([item("minecraft:bone", 0, 2)])])


def main():
    for metal_id, colour, source in METALS:
        gen_metal(metal_id, colour, source)
    for mineral_id, colour in MINERALS:
        gen_mineral(mineral_id, colour)
    gen_copper_nugget()
    gen_misc_items()
    gen_tables()
    gen_entity_textures()
    gen_tags()
    gen_entity_loot()
    gen_worldgen()
    gen_lang()
    print(f"generated {len(lang)} translation keys")


if __name__ == "__main__":
    main()
