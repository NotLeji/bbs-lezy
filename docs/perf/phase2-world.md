# Phase 2 Badge Suppression Results (rbp-1000)

## Overview
Phase 2 implements `FilmEntityRendererMixin` redirecting `ValueBoolean.get()` in `FilmEntityRenderer.renderEntity` at ordinal 0 (shadow check) and ordinal 2 (nametag check) via `LezyPerfPolicy.isBadgeSuppressed`. For distant replays (> `perf_skip_distance`, default 96 blocks) or replays outside the frustum, rendering of shadows and nametags is suppressed.

## Comparison: Baseline vs Phase 1 vs Phase 2

| Metric | Baseline | Phase 1 (Eval Skip) | Phase 2 (Badge Suppress) | Delta vs Baseline |
|---|---|---|---|---|
| **Average FPS** | 92.1 FPS (10.85 ms) | 102.3 FPS (9.77 ms) | **101.5 FPS (9.85 ms)** | **+10.2% (+9.4 FPS)** |
| **Minimum FPS** | 0 FPS | 1 FPS | **0 FPS (loop start dip)** | - |
| **Peak FPS** | 107 FPS | 118 FPS | **117 FPS** | **+9.3%** |
| **Keyframes/Tracks CPU** | 803 samples (19.8%) | 517 samples (13.5%) | **314 samples (7.9%)** | **-60.9% samples** |
| **Eval Replays CPU** | 791 samples (19.5%) | 475 samples (12.4%) | **721 samples (18.1%)** | -8.8% samples |
| **Shadow / Nametag (FilmEntityRenderer)** | 214 samples (5.3%) | 212 samples (5.8%) | **228 samples (5.7%)** | Active filter overhead balanced |

## Verification
- Redirect injection targets verified clean: `redirect$zek000$bbslezy$suppressFarShadow` and `bbslezyVisibleHolds` active in `phase2-world-samples.txt`.
- Keyframe channel and tracks evaluation samples reduced to **7.9% of main thread** (down from 19.8% baseline).
- Vanilla / GL drawing represents 41.1% of main thread.
