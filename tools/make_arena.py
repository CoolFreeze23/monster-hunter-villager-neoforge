"""Writes the GameTest arena template: a flat stone floor (y=0) under open air.

Usage: python tools/make_arena.py <out.nbt> [size_x] [size_y] [size_z]
"""
import gzip, struct, sys

def tag_string(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b

def named(tag_id, name, payload):
    return bytes([tag_id]) + tag_string(name) + payload

def compound(entries):
    return b"".join(entries) + b"\x00"

def int_list(values):
    return bytes([3]) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)

def compound_list(items):
    return bytes([10]) + struct.pack(">i", len(items)) + b"".join(items)

out = sys.argv[1]
sx, sy, sz = (int(a) for a in sys.argv[2:5]) if len(sys.argv) >= 5 else (24, 8, 24)
blocks = [compound([named(9, "pos", int_list([x, 0, z])), named(3, "state", struct.pack(">i", 0))])
          for x in range(sx) for z in range(sz)]
palette = [compound([named(8, "Name", tag_string("minecraft:stone"))])]
root = compound([
    named(3, "DataVersion", struct.pack(">i", 3955)),  # 1.21.1
    named(9, "size", int_list([sx, sy, sz])),
    named(9, "palette", compound_list(palette)),
    named(9, "blocks", compound_list(blocks)),
    named(9, "entities", bytes([10]) + struct.pack(">i", 0)),
])
with gzip.open(out, "wb") as f:
    f.write(named(10, "", root))
print(f"wrote {out}: {sx}x{sy}x{sz}, {len(blocks)} floor blocks")
