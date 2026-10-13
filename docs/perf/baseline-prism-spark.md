# Baseline Spark Profile (Prism Launcher)

## Status
**Deferred**: Awaiting spark profiler run from user's live Prism Launcher instance.

## Instructions for User
1. Install `spark` mod (Fabric 1.20.1) in Prism instance `bbs`.
2. Start the instance and open world `PholonCity v12` (or world with film `OP-perang-1`).
3. Run command: `/spark profiler start`
4. Play film `OP-perang-1` in-world or in film editor for ~60 seconds.
5. Run command: `/spark profiler stop`
6. Share the generated spark link or `.sparkprofile` file in chat.

Once provided, top-5 hotspots will be cross-checked against the JFR headless baseline (`docs/perf/baseline-world.md`).
