#!/usr/bin/env python3
"""Builds the Well of Ascension structure template and the data that places it.

The Well is meant to sit under Luthadel. Luthadel is not generated yet, so for now the Well is
a rare standalone underground structure -- when the city arrives, only the structure_set's
placement needs to change and the template itself carries over unaltered.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from nbt import Byte, Compound, Int, IntList, List, String, Tag, TAG_COMPOUND, write_nbt
from generate_resources import DATA, MODID, write_json

DATA_VERSION = 3955  # 1.21.1

WIDTH, HEIGHT, DEPTH = 15, 11, 15


class Template:
    """Accumulates blocks and de-duplicates them into a palette on write."""

    def __init__(self, size=(WIDTH, HEIGHT, DEPTH)):
        self.size = size
        self.palette = []
        self.palette_index = {}
        self.blocks = {}

    def state_id(self, name, properties=None):
        key = (name, tuple(sorted((properties or {}).items())))
        if key not in self.palette_index:
            entry = {"Name": String(name)}
            if properties:
                entry["Properties"] = Compound({k: String(v) for k, v in properties.items()})
            self.palette_index[key] = len(self.palette)
            self.palette.append(Compound(entry))
        return self.palette_index[key]

    def set(self, x, y, z, name, properties=None, nbt=None):
        self.blocks[(x, y, z)] = (self.state_id(name, properties), nbt)

    def fill(self, x1, y1, z1, x2, y2, z2, name, properties=None):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    self.set(x, y, z, name, properties)

    def to_nbt(self):
        block_tags = []
        for (x, y, z), (state, nbt) in sorted(self.blocks.items()):
            entry = {"pos": IntList([x, y, z]), "state": Int(state)}
            if nbt is not None:
                entry["nbt"] = nbt
            block_tags.append(Compound(entry))
        return Compound({
            "DataVersion": Int(DATA_VERSION),
            "size": IntList(list(self.size)),
            "palette": List(TAG_COMPOUND, self.palette),
            "blocks": List(TAG_COMPOUND, block_tags),
            "entities": List(TAG_COMPOUND, []),
        })


def build_well():
    t = Template()
    wall = "minecraft:stone_bricks"
    accent = "minecraft:polished_andesite"

    # Shell: floor, ceiling and four walls. Everything inside is carved out afterwards.
    t.fill(0, 0, 0, WIDTH - 1, 0, DEPTH - 1, wall)
    t.fill(0, HEIGHT - 1, 0, WIDTH - 1, HEIGHT - 1, DEPTH - 1, wall)
    for y in range(1, HEIGHT - 1):
        for x in range(WIDTH):
            for z in range(DEPTH):
                on_edge = x in (0, WIDTH - 1) or z in (0, DEPTH - 1)
                t.set(x, y, z, wall if on_edge else "minecraft:air")

    # Floor pattern, so the room does not read as a plain box.
    t.fill(1, 0, 1, WIDTH - 2, 0, DEPTH - 2, accent)
    for x in range(2, WIDTH - 2, 2):
        for z in range(2, DEPTH - 2, 2):
            t.set(x, 0, z, "minecraft:polished_blackstone")

    # The well itself: a black pool sunk into a blackstone dais, atium in the floor beneath it.
    t.fill(5, 0, 5, 9, 0, 9, "minecraft:polished_blackstone")
    t.fill(6, 0, 6, 8, 0, 8, "minecraft:water")
    t.set(7, 0, 7, f"{MODID}:atium_block")
    for x, z in ((5, 5), (5, 9), (9, 5), (9, 9)):
        t.set(x, 1, z, "minecraft:polished_blackstone_wall")
        t.set(x, 2, z, "minecraft:soul_lantern", {"hanging": "false"})

    # Four pillars holding the roof up, lit from the top.
    for x, z in ((3, 3), (3, 11), (11, 3), (11, 11)):
        t.fill(x, 1, z, x, HEIGHT - 2, z, accent)
        t.set(x, HEIGHT - 2, z, "minecraft:sea_lantern")

    # Veins of atium showing through the walls -- the Pits are not far away.
    for y, z in ((3, 2), (5, 12), (7, 4), (4, 10)):
        t.set(0, y, z, f"{MODID}:deepslate_atium_ore")
        t.set(WIDTH - 1, y, DEPTH - 1 - z, f"{MODID}:deepslate_atium_ore")

    chest_nbt = Compound({
        "id": String("minecraft:chest"),
        "LootTable": String(f"{MODID}:chests/well_of_ascension"),
    })
    t.set(2, 1, 7, "minecraft:chest", {"facing": "east"}, chest_nbt)
    t.set(WIDTH - 3, 1, 7, "minecraft:chest", {"facing": "west"}, chest_nbt)

    # Jigsaw anchor. A structure with no jigsaw block still generates, but keeping one makes
    # the piece extensible when Luthadel is built around it.
    t.set(7, 1, 1, "minecraft:air")
    return t


def build_test_platform():
    """A 5x5x5 box with a stone floor, for game tests to stand in."""
    t = Template(size=(5, 5, 5))
    for x in range(5):
        for z in range(5):
            t.set(x, 0, z, "minecraft:stone")
            for y in range(1, 5):
                t.set(x, y, z, "minecraft:air")
    return t


def main():
    template = build_well()
    path = os.path.join(DATA, "structure", "well_of_ascension.nbt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    write_nbt(path, template.to_nbt())

    test_path = os.path.join(DATA, "structure", "test_platform.nbt")
    write_nbt(test_path, build_test_platform().to_nbt())

    write_json(os.path.join(DATA, "worldgen", "template_pool", "well_of_ascension", "start.json"), {
        "name": f"{MODID}:well_of_ascension/start",
        "fallback": "minecraft:empty",
        "elements": [{
            "weight": 1,
            "element": {
                "element_type": "minecraft:single_pool_element",
                "projection": "rigid",
                "processors": "minecraft:empty",
                "location": f"{MODID}:well_of_ascension",
            },
        }],
    })

    write_json(os.path.join(DATA, "worldgen", "structure", "well_of_ascension.json"), {
        "type": "minecraft:jigsaw",
        "biomes": "#minecraft:is_overworld",
        "step": "underground_structures",
        "terrain_adaptation": "beard_thin",
        "spawn_overrides": {},
        "start_pool": f"{MODID}:well_of_ascension/start",
        "size": 1,
        "start_height": {"absolute": -20},
        "max_distance_from_center": 80,
        "use_expansion_hack": False,
    })

    write_json(os.path.join(DATA, "worldgen", "structure_set", "well_of_ascension.json"), {
        "structures": [{"structure": f"{MODID}:well_of_ascension", "weight": 1}],
        "placement": {
            "type": "minecraft:random_spread",
            "spacing": 96,
            "separation": 40,
            "salt": 761233,
        },
    })

    write_json(os.path.join(DATA, "loot_table", "chests", "well_of_ascension.json"), {
        "type": "minecraft:chest",
        "pools": [
            {
                "rolls": {"type": "minecraft:uniform", "min": 2, "max": 4},
                "entries": [
                    {"type": "minecraft:item", "name": f"{MODID}:atium_bead", "weight": 3},
                    {"type": "minecraft:item", "name": f"{MODID}:lerasium_nugget", "weight": 1},
                    {"type": "minecraft:item", "name": f"{MODID}:spike_jar", "weight": 4},
                    {"type": "minecraft:item", "name": f"{MODID}:atium_ingot", "weight": 2},
                    {"type": "minecraft:item", "name": f"{MODID}:steel_ingot", "weight": 8,
                     "functions": [{"function": "minecraft:set_count",
                                    "count": {"type": "minecraft:uniform", "min": 2, "max": 6}}]},
                    {"type": "minecraft:item", "name": f"{MODID}:boxing", "weight": 10,
                     "functions": [{"function": "minecraft:set_count",
                                    "count": {"type": "minecraft:uniform", "min": 4, "max": 16}}]},
                ],
            },
        ],
    })
    print("wrote Well of Ascension structure and placement data")


if __name__ == "__main__":
    main()
