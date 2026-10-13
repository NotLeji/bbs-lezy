# Baseline Headless World-Play CPU Profile (Phase 0 Task 2)

## Benchmark Configuration
- Spec: `OP-perang-1,600,1800,true` (film: OP-perang-1, 1001 replays, 600 warmup ticks, 1800 sample ticks = 2400 ticks total, camera enabled).
- Duration: 193 seconds total run (world generation + 2400 client ticks across multiple 223-tick film loops).
- JFR Recording: `build/jfr/baseline-world.jfr` (9.6 MB).
- Summary: `docs/perf/baseline-world-summary.txt`
- Samples: `docs/perf/baseline-world-samples.txt` (11,636 ExecutionSample events).

## FPS Summary (from BBSLezy[bench] harness log)
- Samples count: 90 sample checkpoints (every 20 ticks across 1800 sample ticks).
- Average FPS: **92.1 FPS** (10.85 ms/frame).
- Minimum FPS: **0 FPS** (sub-1 FPS stutters during loop restart when all 1001 replays spawn/evaluate concurrently, e.g. tick 680-700 = 1 FPS, tick 1340-1360 = 0-1 FPS, tick 2020 = 1 FPS).
- Maximum FPS: **107 FPS** (when replays finish motion towards end of camera path).

## Frame Time CPU Breakdown (Main Thread: 4,050 samples)

| Bucket | Samples | Percentage | Hotspot Description |
|---|---|---|---|
| Vanilla engine / render / GL | 1332 | 32.9% | LWJGL draw calls, Minecraft LevelRenderer, GL buffer upload |
| **Keyframes / tracks interpolation** | **803** | **19.8%** | `KeyframeChannel.findSegment`, `KeyframeChannel.interpolate`, `ReplayKeyframes.apply` |
| **Eval replays (`updateEntities` + `startRenderFrame`)** | **791** | **19.5%** | `BaseFilmController.startRenderFrame`, `applyTracks`, `ReplayItemUse.compute` |
| Other BBS operations | 523 | 12.9% | `TrackBehaviours.clearOverrides`, `FormUtils.splitPath`, entity management |
| Render forms (`ModelFormRenderer` / `FormRenderer`) | 387 | 9.6% | Model part transforms, cube rendering |
| Shadow / nametag (`FilmEntityRenderer`) | 214 | 5.3% | `FilmEntityRenderer.renderEntity`, nametag rendering |

## Top BBS Hotspots (Leaf methods)
1. `mchorse.bbs_mod.utils.keyframes.KeyframeChannel.findSegment(float)` (line 240): 199 samples (4.9%)
2. `mchorse.bbs_mod.utils.keyframes.KeyframeChannel.findRawSegment(float)` (line 394): 169 samples (4.2%)
3. `mchorse.bbs_mod.utils.keyframes.KeyframeChannel.interpolate(float, Object)` (line 179): 164 samples (4.0%)
4. `mchorse.bbs_mod.film.replays.tracks.TrackBehaviours.clearOverrides(Form)` (line 81): 88 samples (2.2%)
5. `mchorse.bbs_mod.utils.keyframes.KeyframeChannel.find(float)` (line 154): 67 samples (1.7%)
6. `mchorse.bbs_mod.forms.FormUtils.splitPath(String)` (line 220): 58 samples (1.4%)
7. `leji.bbslezy.LodEngine.rank` (line 227): 45 samples (1.1%)
8. `mchorse.bbs_mod.film.BaseFilmController.applyTracks` (line 447): 45 samples (1.1%)

## Analysis & Phase 1 Priority
Evaluating 1001 replays every frame spends **~39.3% of the main thread** on keyframe channel searching and track interpolation (`startRenderFrame` + `updateEntities`). The severe periodic drops to 0-1 FPS occur when all 1001 actors must re-evaluate their tracks simultaneously.

Phase 1 (`LezyPerfPolicy` skipping distant replays beyond `skip_distance` in `startRenderFrame` and `updateEntities`) directly eliminates this 39.3% evaluation load for the majority of non-visible/distant actors.
