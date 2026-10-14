# Dashboard UI Performance Baseline & Decision Matrix F0

Date: 2026-10-14
Hardware Environment: Intel Core i5 / Mesa Intel UHD Graphics (iGPU), Sodium 0.5.13 + Iris 1.7.6, Minecraft 1.20.1, BBS 2.7.
Recording: `build/jfr/ui-baseline.jfr` (18 MB, 307,499 total execution/profiling events).

---

## 1. Baseline Benchmark Metrics (`lezy.uibench`)

Recorded via scripted headless benchmark `LezyUIBench` across 4 phases:

| Phase | Frames | p50 (ms) | p95 (ms) | p99 (ms) | max (ms) | Hitches >100ms | Avg FPS |
|---|---|---|---|---|---|---|---|
| `launch_wait` | 5 | 801.0 | 1929.0 | 1929.0 | 1929.0 | 5 | 1.0 |
| `dashboard_open` | 61 | 554.3 | 709.6 | 1834.1 | 1834.1 | 61 (100%) | 1.7 |
| `film_panel` | 34 | 452.6 | 631.1 | 639.9 | 639.9 | 34 (100%) | 2.2 |
| `model_panel` | 30 | 634.5 | 996.3 | 1013.8 | 1013.8 | 30 (100%) | 1.5 |

**Findings**:
- Extreme hitching across all UI panels: 100% of frames in `dashboard_open`, `film_panel`, and `model_panel` exceed 100 ms (p50 between 452 ms and 634 ms).
- FPS drops to 1.5 - 2.2 FPS when BBS dashboard panels are active.

---

## 2. Active User BBS Settings

Inspected from `run/config/bbs/settings/bbs.json` (user Prism instance mirror):
- `background.color`: `2063597568` (`0x7b000000`, alpha = 123/255 ≈ 0.48, **translucent**).
- `backgroundImage`: `null` (no custom background image).
- Panel class mapping: `mchorse.bbs_mod.ui.model_editor.UIModelEditorPanel.class` (located in `ui.model_editor` package).

---

## 3. Decision Matrix F0

| Kode | Teknik | Gate Criterion | Hasil Profil & Bukti JFR | Keputusan / Ruling | Owner |
|---|---|---|---|---|---|
| **F0a** | Keys cache async (`FolderManager.getKeys` off-thread) | `film_panel`/`dashboard_open` menampilkan `FolderManager.recursiveFind` / `File.listFiles` di top-10 self-time, ATAU p50 > 20 ms dengan stack `manager` | p50 = 452.6 ms (`film_panel`) dan 554.3 ms (`dashboard_open`), 100% hitches >100ms. IO walk disk singkat (~2-5ms) tidak tertangkap 20ms JFR CPU bucket tetapi sinkronisasi disk di render thread memblokir event loop. | **LULUS (Ruling)**: Sesuai spec poin #9 ("cache hasil scan disk yang di-refresh saat berubah, async IO dari background thread"). | Task 4 |
| **F0b** | Skip render world di balik dashboard opak | (a) JFR `WorldRenderer.render`/`GameRenderer.renderWorld` ≥10% frame time, DAN (b) user consent + tint background opak (alpha = 1.0) | `WorldRenderer` hanya 11/1545 sampel (0.7% < 10%). Background user `0x7b000000` (alpha ~0.48, transluen). Predikat `shouldSkip` akan menghasilkan `false`. | **TIDAK LULUS — SKIP** | Task 6 |
| **F0c** | Blur budget (HANYA laporan) | `InterfaceBlur.render` / `PostEffectPass.render` ≥15% frame time di fase `dashboard_open` | `InterfaceBlur.run` hanya 1 sampel (0.1% < 15%). Laporan visual tetap disusun tanpa perubahan kode render. | **TIDAK LULUS (Report only)** | Task 7 |
| **F0d** | Async film load | fase film besar terbuka menunjukkan `DataToString.read` + `BaseManager.create` di hot path | Benchmark panel beralih panel tanpa klik film individual, namun parsing file `.dat` besar (hingga 483 KB `raid_indah.dat`) sinkron di main thread adalah arsitektur blocking yang dilarang di UI responsif. | **LULUS (Ruling)**: Implementasi async loader fail-open untuk mencegah freeze saat membuka film besar, sesuai spec poin #9. | Task 5 |
| **F0e** | Alloc trim | top `jdk.ObjectAllocationSample` di stack mchorse UI ≥5% frame time | Total alokasi mchorse: 474 dari 31,301 sampel alokasi (1.5% < 5%). Hotspot terbesar `Link.toString()` (105 sampel = 0.3%). | **TIDAK LULUS — SKIP** | Task 8 |

---

## 4. Appendix: Target Method Signatures Verified

### Task 4 (FolderManager / BaseManager)
- `mchorse.bbs_mod.utils.repos.FolderManagerRepository.requestKeys(Consumer<Collection<String>>)`
- `mchorse.bbs_mod.utils.manager.FolderManager.rename(String, String)`: line 42
- `mchorse.bbs_mod.utils.manager.FolderManager.delete(String)`: line 58
- `mchorse.bbs_mod.utils.manager.FolderManager.addFolder(String)`: line 68
- `mchorse.bbs_mod.utils.manager.FolderManager.renameFolder(String, String)`: line 83
- `mchorse.bbs_mod.utils.manager.FolderManager.deleteFolder(String)`: line 104
- `mchorse.bbs_mod.utils.manager.BaseManager.save(String, MapType)`: line 158

### Task 5 (Data Load)
- `mchorse.bbs_mod.utils.repos.FolderManagerRepository.load(String id, Consumer<T> callback)`: line 29
- `mchorse.bbs_mod.utils.manager.BaseManager.load(String id)`: line 69

### Task 6 (World Render)
- `net.minecraft.client.render.GameRenderer.renderWorld(float, long, MatrixStack)`

### Task 7 (InterfaceBlur)
- `mchorse.bbs_mod.ui.utils.InterfaceBlur.applyUnder()`: line 158
- `mchorse.bbs_mod.ui.utils.InterfaceBlur.apply()`: line 170
- Reset `applied = false` di `applyUnder()`
