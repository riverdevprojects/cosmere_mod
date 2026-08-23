"""Minimal PNG writer and sprite helpers, so the mod's art can be generated rather than drawn.

Only the standard library is used: Minecraft textures are small, flat, and indexed-colour
friendly, so a hand-rolled RGBA encoder is simpler than a Pillow dependency.
"""
import struct
import zlib

TRANSPARENT = (0, 0, 0, 0)


def write_png(path, pixels, width, height):
    """pixels: flat list of (r, g, b, a) tuples, row-major."""
    raw = bytearray()
    for y in range(height):
        raw.append(0)  # filter type 0
        for x in range(width):
            r, g, b, a = pixels[y * width + x]
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        out = struct.pack(">I", len(data)) + tag + data
        return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as handle:
        handle.write(png)


def shade(colour, factor):
    r, g, b = colour
    return (
        max(0, min(255, int(r * factor))),
        max(0, min(255, int(g * factor))),
        max(0, min(255, int(b * factor))),
        255,
    )


def render_mask(mask, colour, size=16):
    """Turns an ASCII mask into a shaded sprite.

    '#' is body, '.' is transparent. Edge pixels are darkened and the upper-left of each run is
    lightened, which is enough to read as a bevelled metal object at 16x16.
    """
    pixels = [TRANSPARENT] * (size * size)
    rows = mask.strip("\n").split("\n")
    for y in range(min(size, len(rows))):
        row = rows[y]
        for x in range(min(size, len(row))):
            if row[x] != "#":
                continue
            up = y > 0 and x < len(rows[y - 1]) and rows[y - 1][x] == "#"
            down = y + 1 < len(rows) and x < len(rows[y + 1]) and rows[y + 1][x] == "#"
            left = x > 0 and row[x - 1] == "#"
            right = x + 1 < len(row) and row[x + 1] == "#"
            if not (up and left):
                factor = 1.25
            elif not (down and right):
                factor = 0.7
            else:
                factor = 1.0
            pixels[y * size + x] = shade(colour, factor)
    return pixels


def noise_field(colour, seed, size=16, spread=0.18):
    """A flat tile with deterministic per-pixel jitter, for stone and metal block faces."""
    pixels = []
    state = seed & 0xFFFFFFFF
    for _ in range(size * size):
        state = (state * 1103515245 + 12345) & 0x7FFFFFFF
        jitter = 1.0 + ((state >> 16) % 1000 / 1000.0 - 0.5) * 2 * spread
        pixels.append(shade(colour, jitter))
    return pixels


def overlay_blobs(pixels, colour, seed, size=16, count=9):
    """Speckles a texture with ore-coloured clusters."""
    state = seed & 0xFFFFFFFF
    out = list(pixels)
    for _ in range(count):
        state = (state * 1103515245 + 12345) & 0x7FFFFFFF
        cx = (state >> 8) % size
        state = (state * 1103515245 + 12345) & 0x7FFFFFFF
        cy = (state >> 8) % size
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if abs(dx) + abs(dy) > 1:
                    continue
                x, y = cx + dx, cy + dy
                if 0 <= x < size and 0 <= y < size:
                    factor = 1.15 if (dx, dy) == (-1, -1) else (0.85 if (dx, dy) == (1, 1) else 1.0)
                    out[y * size + x] = shade(colour, factor)
    return out
