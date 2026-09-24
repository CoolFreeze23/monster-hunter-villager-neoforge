import re, sys, collections
def load(path):
    text = open(path, encoding="utf-8", errors="replace").read()
    events = text.split("jdk.ExecutionSample {")[1:]
    stacks = []
    for ev in events:
        if 'sampledThread = "Server thread"' not in ev:
            continue
        frames = re.findall(r"^\s+([\w$.<>]+\.[\w$<>]+)\(", ev, re.M)
        stacks.append(frames)
    return stacks
def summarize(stacks):
    incl = collections.Counter(); top = collections.Counter()
    for fr in stacks:
        if fr: top[fr[0]] += 1
        for m in set(fr): incl[m] += 1
    return incl, top
v = load(sys.argv[1]); p = load(sys.argv[2])
vi, vt = summarize(v); pi, pt = summarize(p)
print(f"server-thread samples: vanilla={len(v)} port={len(p)}")
print("\n== inclusive samples containing mod / event-bus code (port run) ==")
for key in ["net.monsterhuntervillager", "net.neoforged.bus", "EntityTickEvent", "EventHooks.fireEntityTick", "attachment"]:
    n = sum(1 for fr in p if any(key in f for f in fr)); nv = sum(1 for fr in v if any(key in f for f in fr))
    print(f"  {key:32s} port={n:5d} ({100*n/max(1,len(p)):.1f}%)  vanilla={nv:5d} ({100*nv/max(1,len(v)):.1f}%)")
print("\n== biggest inclusive increases, port vs vanilla (as % of each run's server-thread samples) ==")
rows = []
for m in set(vi) | set(pi):
    a = 100 * vi[m] / max(1, len(v)); b = 100 * pi[m] / max(1, len(p))
    rows.append((b - a, m, a, b))
for d, m, a, b in sorted(rows, reverse=True)[:25]:
    print(f"  +{d:5.1f}%  {m}  ({a:.1f}% -> {b:.1f}%)")
print("\n== mod frames by inclusive count (port) ==")
for m, n in sorted(pi.items(), key=lambda kv: -kv[1]):
    if "monsterhuntervillager" in m:
        print(f"  {n:5d}  {m}")
