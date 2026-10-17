# UI InterfaceBlur Measurement & Feasibility Report

Date: 2026-10-14
Component: `mchorse.bbs_mod.ui.utils.InterfaceBlur` (BBS Dual Kawase Blur)
Target: `bbsrc/src/client/java/mchorse/bbs_mod/ui/utils/InterfaceBlur.java`

---

## 1. Code Structure Analysis

In `InterfaceBlur.java`:
- Line 67: `public static void apply()`
- Line 63: `applied = false;` (resets after `apply()`)
- Line 79: `public static void applyUnder()`
- Line 82: `applied = false;` (resets state so multiple elements or overlay panels in the same frame invoke blur again)
- Kawase chain: `MAX_LEVELS = 5` (up to 5 down-sampling passes + 5 up-sampling passes = 10 post-processing passes on screen framebuffer).

---

## 2. JFR Profiling & Measurement

From `ui-baseline.jfr`:
- Total samples in `InterfaceBlur.run`: 1 sample out of 1,545 main thread execution samples (0.06% ≈ 0.1% CPU self-time).
- Invocation rate: ~0.02 - 1.0 sample per frame in `dashboard_open`.
- Analysis: On CPU, setting up uniforms and binding textures consumes negligible CPU time (<0.5%). The bottleneck of 10 Dual Kawase passes on Intel iGPU is fillrate and memory bandwidth on integrated GPU execution units rather than CPU instruction count.

---

## 3. Visual Sacredness & User Decision

- **Constraint**: Spec requirement #8 mandates strict visual identity (zero pixel shift without explicit user consent).
- **Options evaluated**:
  1. *Deduplicate `applyUnder` per frame*: Prevents multiple blurs when overlay panels appear, but shifts visual result when overlapping panels rely on nested blurs.
  2. *Reduce Kawase levels (e.g. 5 -> 3)*: Drastically cuts fillrate cost on iGPU, but noticeably sharpens background bokeh (changes visual output).
  3. *Downsample input buffer (half-resolution)*: Saves 75% fillrate, but blurs text edges or changes softness curve.
- **Rekomendasi & Keputusan**:
  - **Pilihan 1 (Default - Diterapkan)**: **Tidak ada kerja (No code changes)**. Hasil visual tetap 100% identik dengan BBS vanilla. Segala optimisasi shader blur yang berpotensi mengubah delta visual ditolak dari scope plan ini dan hanya boleh dikerjakan bila ada permintaan/persetujuan eksplisit user di masa mendatang.
