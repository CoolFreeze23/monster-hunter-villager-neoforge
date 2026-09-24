"""Turns the showcase recording (run/showcase/screenshots) into README media in docs/media.

    python tools/media/make_media.py sheets <out_dir>   # contact sheets of every recorded GIF, for review
    python tools/media/make_media.py build [name ...]   # GIFs + stills into docs/media (all, or just the named ones)

GIF frames were captured once per game tick (20 fps). Each GIF gets its own palette
(ffmpeg palettegen/paletteuse) and is scaled to 800 px wide.
"""
import os
import subprocess
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SHOTS = os.path.join(ROOT, "run", "showcase", "screenshots")
OUT = os.path.join(ROOT, "docs", "media")

# name -> (first frame, last frame exclusive or None, crop as fractions (left, top, right, bottom) or None[, fps[, width, colours]]).
# Crops frame the action so small mobs stay readable at README width.
GIFS = {
    "gif_sticky": (0, None, (0.04, 0.22, 0.84, 0.92)),
    "gif_sharpened": (0, None, (0.15, 0.22, 0.85, 0.92)),
    "gif_soul_chain": (0, None, (0.15, 0.2, 0.85, 0.9)),
    "gif_hunt": (0, 200, (0.15, 0.2, 0.85, 0.9)),
    "gif_knife": (0, 50, (0.1, 0.12, 0.8, 0.82)),
    "gif_dodge": (0, None, (0.15, 0.22, 0.85, 0.92)),
    "gif_throw": (0, None, (0.1, 0.12, 0.9, 1.0), 15, 640, 112),  # full-screen turns redraw every pixel of grass, so trim it
    "gif_pickup": (0, None, None),
    "gif_craft": (0, None, (0.31, 0.0, 0.97, 0.82)),
    # Release-note pairs from the 'fixes' section: the _before frames were recorded on a build with that fix reverted.
    "gif_fix_kill_before": (0, None, (0.2, 0.45, 0.8, 0.9), 20, 640),
    "gif_fix_kill_after": (0, None, (0.2, 0.45, 0.8, 0.9), 20, 640),
    "gif_fix_creative_before": (0, None, (0.315, 0.915, 0.685, 1.0), 20, 592),
    "gif_fix_creative_after": (0, None, (0.315, 0.915, 0.685, 1.0), 20, 592),
}
# still -> (output name, crop box as fractions (left, top, right, bottom) or None, format)
STILLS = {
    "hero": ("hero", None, "jpg"),
    "traps": ("traps", None, "jpg"),
    "hunter_workbench": ("hunter_workbench", None, "jpg"),
    "tents": ("tents", None, "jpg"),
    "traps_night": ("traps_night", None, "jpg"),
    "soul_chain_night": ("soul_chain_night", None, "jpg"),
    "trades": ("trades", None, "png"),
    "inventory": ("inventory", None, "png"),
    "jei_recipes": ("jei_recipes", None, "png"),
}
WIDTH = 800


def frames(name):
    folder = os.path.join(SHOTS, name)
    return sorted(f for f in os.listdir(folder) if f.endswith(".png"))


def sheets(out_dir, every=10, columns=4, thumb=400):
    os.makedirs(out_dir, exist_ok=True)
    for name in GIFS:
        if not os.path.isdir(os.path.join(SHOTS, name)):
            print("missing", name)
            continue
        picked = frames(name)[::every]
        images = [Image.open(os.path.join(SHOTS, name, f)).convert("RGB") for f in picked]
        h = int(images[0].height * thumb / images[0].width)
        rows = (len(images) + columns - 1) // columns
        sheet = Image.new("RGB", (columns * thumb, rows * (h + 18)), "white")
        from PIL import ImageDraw
        draw = ImageDraw.Draw(sheet)
        for i, (img, fname) in enumerate(zip(images, picked)):
            x, y = (i % columns) * thumb, (i // columns) * (h + 18)
            sheet.paste(img.resize((thumb, h)), (x, y + 18))
            draw.text((x + 4, y + 2), fname, fill="black")
        path = os.path.join(out_dir, name + "_sheet.jpg")
        sheet.save(path, quality=80)
        print(path, len(picked), "thumbs")


def gif(name, first, last, crop, fps=20, width=WIDTH, colours=160):
    folder = os.path.join(SHOTS, name)
    names = frames(name)[first:last]
    listing = os.path.join(folder, "_frames.txt")
    with open(listing, "w") as f:
        for n in names:
            f.write(f"file '{n}'\nduration {1 / 20:.4f}\n")
    out = os.path.join(OUT, name.replace("gif_", "") + ".gif")
    cut = ""
    if crop:
        cut = f"crop=iw*{crop[2] - crop[0]:.3f}:ih*{crop[3] - crop[1]:.3f}:iw*{crop[0]:.3f}:ih*{crop[1]:.3f},"
    vf = (f"fps={fps},{cut}scale={width}:-1:flags=lanczos,split[a][b];"
          f"[a]palettegen=max_colors={colours}:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", listing,
                    "-vf", vf, "-loop", "0", out], check=True)
    os.remove(listing)
    print(f"{out}: {len(names)} frames, {os.path.getsize(out) / 1e6:.1f} MB")


def still(name, out_name, crop, fmt):
    img = Image.open(os.path.join(SHOTS, name + ".png")).convert("RGB")
    if crop:
        w, h = img.size
        img = img.crop((int(crop[0] * w), int(crop[1] * h), int(crop[2] * w), int(crop[3] * h)))
    out = os.path.join(OUT, f"{out_name}.{fmt}")
    if fmt == "jpg":
        img.save(out, quality=88, optimize=True, progressive=True)
    else:
        img.save(out, optimize=True)
    print(f"{out}: {img.size[0]}x{img.size[1]}, {os.path.getsize(out) / 1e6:.2f} MB")


def build(only=()):
    os.makedirs(OUT, exist_ok=True)
    for name, spec in GIFS.items():
        if (not only or name in only) and os.path.isdir(os.path.join(SHOTS, name)):
            gif(name, *spec)
    for name, (out_name, crop, fmt) in STILLS.items():
        if (not only or name in only) and os.path.exists(os.path.join(SHOTS, name + ".png")):
            still(name, out_name, crop, fmt)


if __name__ == "__main__":
    if sys.argv[1] == "sheets":
        sheets(sys.argv[2])
    else:
        build(sys.argv[2:])
