"""Server tick-time benchmark: the same scene on Forge 1.20.1 and NeoForge 1.21.1, with and without the mod.

Scene (active hunt): flat world, night frozen, mob spawning off, 500 NoAI zombies in a grid and
8 NoAI Monster Hunter villagers ringing the grid 3-4 blocks outside it: close enough to spot and
target zombies every tick (the expensive path), too far to knife them, so the zombie count holds.
After a warm-up the script samples the server's own mean tick time (last 100 ticks) every 5 s.

Usage: python bench_active.py <forge1201|neo1211> <mod jar or 'none'> <label>
"""
import json, os, re, shutil, subprocess, sys, threading, time

HERE = os.path.dirname(os.path.abspath(__file__))
# Java executables for the two servers. Both default to `java` on the PATH.
JAVA17 = os.environ.get("JAVA17", "java")
JAVA21 = os.environ.get("JAVA21", "java")
SERVERS = {
    "forge1201": (JAVA17, r"libraries/net/minecraftforge/forge/1.20.1-47.4.2/win_args.txt", "forge tps"),
    "neo1211": (JAVA21, r"libraries/net/neoforged/neoforge/21.1.248/win_args.txt", "neoforge tps"),
}
WARMUP_S, SAMPLES, SAMPLE_EVERY_S = 40, 12, 5

server, jar, label = sys.argv[1], sys.argv[2], sys.argv[3]
java, args_file, tps_cmd = SERVERS[server]
root = os.path.join(HERE, server)

for path in ("world", "mods", "logs", "config", "defaultconfigs"):
    shutil.rmtree(os.path.join(root, path), ignore_errors=True)
os.makedirs(os.path.join(root, "mods"))
if jar != "none":
    shutil.copy(jar, os.path.join(root, "mods"))
with open(os.path.join(root, "eula.txt"), "w") as f:
    f.write("eula=true\n")
with open(os.path.join(root, "server.properties"), "w") as f:
    f.write("\n".join([
        "level-type=minecraft\\:flat", "generate-structures=false", "online-mode=false",
        "spawn-monsters=false", "spawn-animals=false",
        # spawn-npcs must stay true: with it off the server discards every villager on its first tick.
        "spawn-npcs=true", "difficulty=easy",
        "view-distance=6", "simulation-distance=6", "max-tick-time=-1", "spawn-protection=0",
        "server-port=25590", "sync-chunk-writes=false", ""]))
with open(os.path.join(root, "user_jvm_args.txt"), "w") as f:
    f.write("-Xms4G\n-Xmx4G\n")
    if os.environ.get("BENCH_JFR"):
        # Record the measurement window only (after start-up, summoning and warm-up).
        recording = os.path.join(HERE, label + ".jfr").replace("\\", "/")
        f.write(f"-XX:StartFlightRecording=delay=75s,duration=60s,settings=profile,filename={recording}\n")

proc = subprocess.Popen([java, "@user_jvm_args.txt", "@" + args_file, "nogui"], cwd=root,
                        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                        text=True, encoding="utf-8", errors="replace", bufsize=1)
lines = []
ready = threading.Event()

def reader():
    for line in proc.stdout:
        lines.append(line.rstrip())
        if "Done (" in line:
            ready.set()

threading.Thread(target=reader, daemon=True).start()

def send(cmd):
    proc.stdin.write(cmd + "\n")
    proc.stdin.flush()

if not ready.wait(600):
    proc.kill()
    sys.exit("server never finished starting:\n" + "\n".join(lines[-40:]))

for cmd in ["gamerule doDaylightCycle false", "time set 18000", "gamerule doMobSpawning false",
            "gamerule doWeatherCycle false", "weather clear", "forceload add -48 -48 47 47"]:
    send(cmd)
time.sleep(5)
count = 0
for x in range(-24, 26, 2):
    for z in range(-20, 20, 2):
        send(f"summon minecraft:zombie {x} -60 {z} {{NoAI:1b,PersistenceRequired:1b,Silent:1b}}")
        count += 1
for x, z in [(-28, -11), (-28, 9), (27, -11), (27, 9), (-11, -24), (9, -24), (-11, 22), (9, 22)]:
    send(f'summon minecraft:villager {x}.5 -60 {z}.5 {{NoAI:1b,PersistenceRequired:1b,'
         f'VillagerData:{{profession:"monster_hunter_villager:monster_hunter",level:2,type:"minecraft:plains"}}}}')
time.sleep(2)
send("execute if entity @e[type=minecraft:villager]")
send("data get entity @e[type=minecraft:villager,limit=1] Pos")
time.sleep(WARMUP_S)

samples = []
pattern = re.compile(r"overworld.*?([0-9]+(?:\.[0-9]+)?)\s*ms", re.I)
for _ in range(SAMPLES):
    mark = len(lines)
    send("execute if entity @e[type=minecraft:villager]")
    send(tps_cmd)
    time.sleep(SAMPLE_EVERY_S)
    for line in lines[mark:]:
        m = pattern.search(line)
        if m:
            samples.append(float(m.group(1)))
            break

mark = len(lines)
send("execute if entity @e[type=minecraft:zombie]")
send("execute if entity @e[type=minecraft:villager]")
time.sleep(2)
counts = [l for l in lines[mark:] if "Test passed" in l or "count" in l.lower()]
send("stop")
try:
    proc.wait(120)
except subprocess.TimeoutExpired:
    proc.kill()

errors = [l for l in lines if "Exception" in l or "/ERROR]" in l]
result = {"label": label, "server": server, "zombies_summoned": count, "samples_ms": samples,
          "mean_ms": round(sum(samples) / len(samples), 3) if samples else None,
          "entity_counts": counts, "error_lines": errors[:10]}
with open(os.path.join(HERE, f"result-{label}.json"), "w") as f:
    json.dump(result, f, indent=2)
with open(os.path.join(HERE, f"log-{label}.txt"), "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print(json.dumps(result, indent=2))
