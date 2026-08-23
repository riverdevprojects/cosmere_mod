"""Just enough NBT to write a Minecraft structure template.

Structure templates are the only part of the mod's data that cannot be expressed as JSON, and
authoring one by hand in a level editor would make it unreviewable. Writing the NBT directly
keeps the Well of Ascension in source control as readable code.
"""
import gzip
import struct

TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10


class Tag:
    def __init__(self, tag_id, value):
        self.id = tag_id
        self.value = value


def Byte(v):
    return Tag(TAG_BYTE, v)


def Int(v):
    return Tag(TAG_INT, v)


def String(v):
    return Tag(TAG_STRING, v)


def List(tag_id, values):
    return Tag(TAG_LIST, (tag_id, values))


def Compound(mapping):
    return Tag(TAG_COMPOUND, mapping)


def IntList(values):
    return List(TAG_INT, [Int(v) for v in values])


def _write_string(out, text):
    encoded = text.encode("utf-8")
    out += struct.pack(">H", len(encoded))
    out += encoded


def _write_payload(out, tag):
    if tag.id == TAG_BYTE:
        out += struct.pack(">b", tag.value)
    elif tag.id == TAG_SHORT:
        out += struct.pack(">h", tag.value)
    elif tag.id == TAG_INT:
        out += struct.pack(">i", tag.value)
    elif tag.id == TAG_LONG:
        out += struct.pack(">q", tag.value)
    elif tag.id == TAG_FLOAT:
        out += struct.pack(">f", tag.value)
    elif tag.id == TAG_DOUBLE:
        out += struct.pack(">d", tag.value)
    elif tag.id == TAG_STRING:
        _write_string(out, tag.value)
    elif tag.id == TAG_LIST:
        element_id, values = tag.value
        out += struct.pack(">b", element_id if values else TAG_END)
        out += struct.pack(">i", len(values))
        for value in values:
            _write_payload(out, value)
    elif tag.id == TAG_COMPOUND:
        for name, value in tag.value.items():
            out += struct.pack(">b", value.id)
            _write_string(out, name)
            _write_payload(out, value)
        out += struct.pack(">b", TAG_END)
    else:
        raise ValueError(f"unsupported tag id {tag.id}")


def write_nbt(path, root, root_name=""):
    """Writes a gzip-compressed NBT file with `root` as the top-level compound."""
    out = bytearray()
    out += struct.pack(">b", TAG_COMPOUND)
    _write_string(out, root_name)
    _write_payload(out, root)
    with gzip.open(path, "wb") as handle:
        handle.write(bytes(out))
