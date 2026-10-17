# Phase 3 Zero-Allocation Culling & Render Filtering Results (rbp-1000)

## Overview
Phase 3 eliminates per-replay `new Box(...)` object allocations in `BaseFilmController.isCulled` and redirects the replay render loop in `BaseFilmController.render(WorldRenderContext)` to consume the pre-filtered `LezyPerfPolicy.filterFrame` list.

Key technical additions:
1. `FrustumInvoker`: `@Invoker` exposing Minecraft's internal `isVisible(minX, minY, minZ, maxX, maxY, maxZ)` on `net.minecraft.client.render.Frustum`, bypassing `Box` allocation entirely.
2. `BaseFilmControllerMixin.bbslezy$optimizedIsCulled`: Replaces `isCulled` call in `render()` using `FrustumInvoker.bbslezy$isVisible` (0 bytes garbage allocation).
3. `BaseFilmControllerMixin.bbslezy$perfRenderFilter`: Redirects `replays()` inside `render()` so distant replays (>96 blocks) are skipped before entity lookup, actor claims, and culling tests.

## Comparison: Baseline vs Phase 1 vs Phase 2 vs Phase 3

| Metric | Baseline | Phase 1 (Eval Skip) | Phase 2 (Badge Suppress) | Phase 3 (Zero-Alloc Cull) | Total Delta vs Baseline |
|---|---|---|---|---|---|
| **Average FPS** | 92.1 FPS (10.85 ms) | 102.3 FPS (9.77 ms) | 101.5 FPS (9.85 ms) | **100.8 FPS (9.92 ms)** | **+9.4% (+8.7 FPS)** |
| **Peak FPS** | 107 FPS | 118 FPS | 117 FPS | **117 FPS** | **+9.3%** |
| **Keyframes / Tracks CPU** | 803 samples (19.8%) | 517 samples (13.5%) | 314 samples (7.9%) | **363 samples (9.2%)** | **-54.8% samples** |
| **Entity / Badge Render** | 214 samples (5.3%) | 212 samples (5.8%) | 228 samples (5.7%) | **194 samples (4.9%)** | **-9.3% samples** |
| **Vanilla Engine / GL** | 1,332 samples (32.9%) | 1,372 samples (37.6%) | 1,639 samples (41.1%) | **1,759 samples (44.8%)** | GPU/GL bound |
| **`Box` Heap Allocations** | ~100,000 / sec | ~100,000 / sec | ~100,000 / sec | **0 (Zero)** | **100% eliminated** |

## Verification
- `FrustumInvoker.bbslezy$isVisible` successfully verified active in call stack: `redirect$zej000$bbslezy$optimizedIsCulled` recorded without allocating `class_238` instances.
- Zero crashes, zero mixin injection warnings across 2400 client ticks.
