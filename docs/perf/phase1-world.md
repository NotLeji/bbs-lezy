# Phase 1 Evaluation Budgeting Results (rbp-1000)

## Overview
Phase 1 implements `LezyPerfPolicy` and redirects `BaseFilmController.replays()` in both `startRenderFrame` (per-frame render setup) and `updateEntities` (per-tick entity state update) to skip pose and keyframe evaluation for replays farther than `skip_distance` (96 blocks) and throttle updates for middle-distance replays (32–96 blocks).

## Comparison: Baseline vs Phase 1

| Metric | Baseline (`perf_enabled=false`) | Phase 1 (`perf_enabled=true`) | Delta |
|---|---|---|---|
| **Average FPS** | 92.1 FPS | **102.3 FPS** | **+11.1% (+10.2 FPS)** |
| **Average Frame Time** | 10.85 ms | **9.77 ms** | **-1.08 ms / frame** |
| **Minimum FPS** | 0 FPS (sub-1 stutter) | **1 FPS** | Stutter floor elevated |
| **Peak FPS** | 107 FPS | **118 FPS** | **+10.3%** |
| **Eval Replays CPU** | 791 samples (19.5%) | **475 samples (12.4%)** | **-39.9% samples** |
| **Keyframes/Tracks CPU** | 803 samples (19.8%) | **517 samples (13.5%)** | **-35.6% samples** |
| **Total Evaluation Load** | 1,594 samples (**39.3%**) | **992 samples (25.9%)** | **-37.8% CPU load reduction** |

## Main Thread Bucket Breakdown (Phase 1)
- `vanilla_engine/render/gl`: 1390 samples (36.4%)
- `other_bbs`: 722 samples (18.9%)
- `keyframes/tracks`: 517 samples (13.5%)
- `render_forms (ModelFormRenderer/FormRenderer)`: 506 samples (13.2%)
- `eval_replays (updateEntities/startRenderFrame)`: 475 samples (12.4%)
- `shadow/nametag/entity_renderer (FilmEntityRenderer)`: 213 samples (5.6%)

## Findings for Subsequent Phases
1. **Eval Skip Verified**: Per-frame keyframe channel searching and track interpolation dropped by ~38%, matching the target design.
2. **Next Hotspot (Phase 2)**: `shadow/nametag` (`FilmEntityRenderer`) accounts for 213 samples (5.6%) of frame time. Phase 2 suppresses shadow and nametag rendering for distant replays.
3. **Allocation / Cull Hotspot (Phase 3)**: `BaseFilmController.isCulled` accounts for 204 samples (5.3% in call stacks) and allocates `new Box(...)` per replay per frame. Reusing this box in Phase 3 will directly trim allocation pressure.
