# Server tick benchmark

Measures what the mod costs a server: the same scene is run on Forge 1.20.1 with and without the
original 1.2.1 jar, and on NeoForge 1.21.1 with and without this port.

## Scenes

Flat world, time frozen at night, natural spawning off, 500 zombies with `NoAI` in a grid.

- `bench.py` (idle): plus 4 `NoAI` Monster Hunters parked out of the zombies' reach.
- `bench_active.py` (active hunt): plus 8 `NoAI` Monster Hunters ringing the grid, close enough to
  target zombies every tick, too far to knife them, so the zombie count stays at 500.

`NoAI` keeps every run identical. The mod's per-tick code still runs for these entities: the
original's through `LivingTickEvent`, the port's through `EntityTickEvent`.

After a 40 s warm-up, each run samples the server's own mean tick time (`/forge tps` or
`/neoforge tps`, a 100-tick average) every 5 s, 12 times. Entity counts are checked after
summoning, at every sample, and at the end.

## Setup

Install the servers next to the scripts with the official installers:

- `forge1201/`: `java -jar forge-1.20.1-47.4.2-installer.jar --installServer forge1201`
- `neo1211/`: `java -jar neoforge-21.1.248-installer.jar --install-server neo1211`

Then point the `JAVA17` and `JAVA21` environment variables at a Java 17 and a Java 21 executable
(both default to `java` on the PATH).

## Running

```
python bench.py <forge1201|neo1211> <mod jar or none> <label>
python bench_active.py <forge1201|neo1211> <mod jar or none> <label>
```

Run each configuration twice in A-B-B-A order (mod, no mod, no mod, mod). A single run is not
enough: identical setups vary by up to about 0.4 ms/tick between runs on the same machine, and a
first-vs-second-run effect is easy to mistake for mod overhead. Set `BENCH_JFR=1` to record a Java
Flight Recorder profile of the measurement window. `jfr_agg.py` compares two recordings'
server-thread samples.

`server.properties` must keep `spawn-npcs=true`: with it off, the server deletes every villager on
its first tick, silently removing the hunters from the scene.

`results/summary.json` holds every run behind the numbers in the main README.
