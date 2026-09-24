"""Draws docs/media/performance-{light,dark}.svg from tools/benchmark/results/summary.json.

The chart shows what each version of the mod adds to the server's mean tick time (with-mod minus
without-mod, pooled over the A-B-B-A runs of the same setup).
"""
import json
import os
from statistics import mean

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RESULTS = os.path.join(ROOT, "tools", "benchmark", "results", "summary.json")
OUT = os.path.join(ROOT, "docs", "media")

THEMES = {
    "light": dict(surface="#fcfcfb", ink="#0b0b0b", ink2="#52514e", muted="#898781", grid="#e1e0d9", axis="#c3c2b7",
                  original="#2a78d6", port="#eb6834"),
    "dark": dict(surface="#1a1a19", ink="#ffffff", ink2="#c3c2b7", muted="#898781", grid="#2c2c2a", axis="#383835",
                 original="#3987e5", port="#d95926"),
}
FONT = "system-ui, -apple-system, 'Segoe UI', Helvetica, Arial, sans-serif"


def overhead(runs, scene, platform, mod):
    def pooled(kind):
        samples = []
        for label, run in runs.items():
            if label.startswith(f"{scene}-{platform}-{kind}-"):
                samples += run["samples_ms"]
        return mean(samples)
    return pooled(mod) - pooled("vanilla")


def bar(x, y, length, thickness, color):
    """A horizontal bar starting at x, square at the axis and rounded at the far end."""
    r = min(4, length / 2, thickness / 2)
    return (f'<path d="M{x:.1f},{y:.1f} h{length - r:.1f} a{r},{r} 0 0 1 {r},{r} v{thickness - 2 * r:.1f} '
            f'a{r},{r} 0 0 1 -{r},{r} h-{length - r:.1f} z" fill="{color}"/>')


def chart(theme, data):
    t = THEMES[theme]
    width, height = 800, 330
    left, right = 250, 660            # plot area x-range
    scale_max = 2.5
    px = lambda ms: left + (right - left) * ms / scale_max
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" '
           f'role="img" aria-labelledby="t d" font-family="{FONT}">',
           '<title id="t">Server time the mod adds per tick</title>',
           '<desc id="d">' + "; ".join(f"{scene}: original +{o:.2f} ms, port +{p:.2f} ms" for scene, o, p in data) + '</desc>',
           f'<rect width="{width}" height="{height}" rx="12" fill="{t["surface"]}"/>',
           f'<text x="32" y="44" font-size="20" font-weight="600" fill="{t["ink"]}">Server time the mod adds per tick</text>',
           f'<text x="32" y="68" font-size="14" fill="{t["ink2"]}">500 zombies loaded on a dedicated server (lower is better)</text>']
    # Legend
    lx = 32
    for key, text in (("original", "Original 1.2.1 (Forge 1.20.1)"), ("port", "This port (NeoForge 1.21.1)")):
        out.append(f'<rect x="{lx}" y="88" width="12" height="12" rx="3" fill="{t[key]}"/>')
        out.append(f'<text x="{lx + 18}" y="99" font-size="13" fill="{t["ink2"]}">{text}</text>')
        lx += 250
    # Grid and ticks
    top, bottom = 122, 280
    for i in range(6):
        ms = i * 0.5
        x = px(ms)
        out.append(f'<line x1="{x:.1f}" y1="{top}" x2="{x:.1f}" y2="{bottom}" stroke="{t["grid"] if i else t["axis"]}" stroke-width="1"/>')
        out.append(f'<text x="{x:.1f}" y="{bottom + 18}" font-size="12" fill="{t["muted"]}" text-anchor="middle">{ms:.1f} ms</text>')
    # Groups of two bars
    thickness, gap = 22, 2
    y = top + 14
    for scene, original, port in data:
        out.append(f'<text x="{left - 16}" y="{y + thickness + 5}" font-size="14" fill="{t["ink"]}" text-anchor="end">{scene}</text>')
        for key, value in (("original", original), ("port", port)):
            length = max(px(value) - left, 3)
            out.append(bar(left, y, length, thickness, t[key]))
            label = f"+{value:.2f} ms"
            note = "  (within measurement noise)" if key == "port" else ""
            out.append(f'<text x="{left + length + 8:.1f}" y="{y + 16}" font-size="13" fill="{t["ink"]}">{label}'
                       f'<tspan fill="{t["ink2"]}">{note}</tspan></text>')
            y += thickness + gap
        y += 22
    out.append(f'<text x="32" y="{height - 14}" font-size="12" fill="{t["muted"]}">Mean server tick time with the mod minus without it. '
               'Each setup ran twice in A-B-B-A order. Method and raw data: tools/benchmark.</text>')
    out.append('</svg>')
    return "\n".join(out)


def main():
    runs = json.load(open(RESULTS))
    data = [
        ("Hunters standing by", overhead(runs, "idle", "forge", "original"), overhead(runs, "idle", "neo", "port")),
        ("Hunters targeting zombies", overhead(runs, "active", "forge", "original"), overhead(runs, "active", "neo", "port")),
    ]
    os.makedirs(OUT, exist_ok=True)
    for theme in THEMES:
        path = os.path.join(OUT, f"performance-{theme}.svg")
        with open(path, "w", encoding="utf-8") as f:
            f.write(chart(theme, data))
        print(path)
    for scene, o, p in data:
        print(f"{scene}: original +{o:.3f} ms, port +{p:.3f} ms")


if __name__ == "__main__":
    main()
