# BBS Lezy

<p align="center"><img src="bbs-lezy.webp" alt="BBS Lezy" width="500"></p>

<p align="center">
  <b>English</b>  |  <a href="README_ID.md">Bahasa Indonesia</a>
</p>

---

BBS Lezy is an addon for making Minecraft content aimed at video, the kind of thing Grox, Reff, Remanrhn and friends put out.

It is built for large scenes and for editing speed: a model render limit (LOD) for thousands of actors, bulk replay panel management, **video export with two separate audio tracks** (BBS clips + Minecraft sounds), **direct playback of many audio formats** (.mp3, .m4a, .opus, .flac, and so on) with no conversion step, plus a pile of workflow fixes. Everything is configurable straight from the BBS editor, no config file surgery required.

## Features

**1. Model render limit (Limit Replay)**

Not every replay model is drawn every frame — only the ones closest to the camera make the cut. Worth having when a scene runs to thousands of actors and the GPU starts crying. Hidden models are toggled through a BBS runtime value, so animation keyframes are left untouched. Turn it off for renders and video exports and every model gets drawn.

**2. Replay panel**

- **Select all replays** — select every replay in the film, including the ones inside collapsed folders (the stock BBS select-all only grabs what is on screen).
- **Select same model** — select every replay whose model matches the current selection. Good for grabbing an entire duplicate-farm result in one click.
- **Duplicate to total** — the number you type is the **total** copy count across the whole selection, not a per-replay count. Say you select 3 replays and enter 150, you get 150 copies (50 each), not 450. Each replay gets its own `Duplicates N` group so they stay easy to manage or delete.
- **Scroll up/down buttons** — jump straight to the top or bottom of the replay list with no animation. Handy once the list runs to thousands of rows.
- **Reset replay actors** — right-click menu in the replay list panel to respawn every replay actor back to its start and refresh the display, without leaving and re-entering the dashboard.

**3. Audio export & codecs**

- **Separate audio tracks** — when exporting with BBS's **audio** and **minecraft sounds** both enabled, the result carries **two separate audio tracks** (Track 1: BBS film clip audio, Track 2: Minecraft SFX) instead of one stereo mix. Each track is forced to standard 2-channel stereo (AAC 192k) so it drops straight into Premiere Pro, DaVinci Resolve, CapCut, Vegas Pro and friends. Temporary WAV files are cleaned up once the mux finishes.
- **Multi-codec audio, picked straight in the audio browser** — BBS can now read `.mp3`, `.m4a`, `.aac`, `.opus`, `.wma`, `.alac`, `.ape`, `.flac`, `.aif/.aiff` and `.ac3` directly. They show up in the "Pick audio..." menu, get a waveform preview, can be edited (offset, duration, volume), cut or split on the timeline, and render just like the built-in WAVs. Decoding is on-demand through ffmpeg, so the file on disk is never modified or converted.
- **Import audio without conversion** — dropped audio files are copied **verbatim** (byte-identical) instead of being force-re-encoded to mono WAV. Video files (`.mp4`) still get their audio extracted the usual way.

**4. Screen effect clips**

Ported from ElgatoPro300's BBS CML — thanks!

https://github.com/user-attachments/assets/169defa4-20fe-452a-8dac-c3a78e5c6c52

- **Cinematic Effect** — one clip bundling the whole vintage camera look: film flicker, random scratches, desaturation, framing/letterbox, film grain, and optics (fisheye, chromatic aberration, VHS glitch, radial blur). Every effect has its own parameters and they combine freely.
- **Color Grade** — pure color grading (saturation, hue, brightness, contrast, lift, gamma, gain) plus a flat overlay tint.
- **Vignette** — radial darkening toward the frame edges.
- **Letterbox** — cinematic bars with aspect-ratio presets (Scope 2.39:1, Cinema, Univisium, Flat, 16:9, 4:3, Square, 9:16 Shorts, Custom), pillarbox support for vertical video, inner-edge feathering, color, offset, rotation and zoom.
- **Camera Shake** — procedural handheld shake with Gentle / Action / Explosion presets and per-axis amplitude control.
- **Impact Frame** — freeze frame, screen flash, FOV punch-in and shake impulse in one clip, with Anime Impact / Hard Hit / Explosion / Dramatic presets.
- **Per-track clip hierarchy** — effects follow the camera timeline's track order, bottom track first, so an upper-track grade layers over the lower tracks instead of merging into one look. Same-track clips still stack, and a single-track film still costs a single pass.

**5. Illusion (visual duplication)**

- Visual duplicates around a form or model block **without adding entities** — one actor reads as ten with none of the entity load. 6 directions, spread, opacity fade, an **Enabled** toggle, etc. (Uniform Distance, Real, Distort, Gradual Transform).
- Keyframable from the Dope Sheet (`illusion` and `illusion_transform` tracks), all tuned from the Form Editor's **Illusion** section. Ported from ElgatoPro300's BBS CML.

**6. Video export (CQP, codec & GPU)**

- **Video CQP** — controls quality/compression (0–51, default 18).
- **Video Codec** — pick `h264` (default, maximum compatibility), `h265` (HEVC, better compression), or `vp9` (WebM).
- **Hardware Acceleration (GPU)** — encode on the GPU (NVIDIA NVENC, AMD AMF on Windows / VA-API on Linux, Intel QSV with VA-API fallback on Linux) for much faster exports and a lighter CPU load. Default: **on**. There is an **Auto-Detect** option or you can pick a specific vendor.
- **GPU codec warning** — if the selected codec (such as VP9) or the active GPU / ffmpeg build lacks a working hardware encoder, a dialog offers to fall back to CPU encoding for that one export.
- **Linux quality-of-life fixes** — bounded sliders wrap the cursor at the window edge on Linux/XWayland just like unbounded trackpads, Linux video export probes and uses NVENC, Intel QSV (with VA-API fallback), or AMD VA-API per render node, and loading/reloading an Iris shaderpack inside the BBS editor schedules a clean reload once the editor closes.

**7. Framing guides**

- Toggle button in the film preview toolbar: left-click turns all guides on/off (your last setup is remembered), right-click picks which ones show.
- Safe areas (Action 90%, Title 80%), 9:16 vertical and 2.39:1 scope overlays, plus BBS's native center lines and crosshair.
- Color, opacity and labels live in the BBS Lezy settings.

**8. Etc.**

Smaller stuff that didn't earn its own section: halftone screentone clip, motion blur and pixelation channels, skin search with 3D preview, Look At batch baking, damage action clip, snap-player-to-preview, reverse audio with volume keyframes, open-folder-on-import, Linux cursor wrap and Iris depth fixes, dashboard shader toggle (K).

## Documentation

### Requirements

| Item      | Version                                     |
| --------- | ------------------------------------------- |
| Minecraft | 1.20.1                                      |
| Java      | 17+                                         |
| Fabric    | Loader 0.16.14, Fabric API 0.92.1+1.20.1    |
| BBS FS    | 2.7-1.20.1                                  |
| Sodium    | 0.5.8                                       |
| Iris      | Optional (for shader support)               |
| ffmpeg    | You install it yourself (not bundled)       |

### Install

1. Download the `.jar` from [Releases](../../releases) (no git clone needed).
2. Drop it in your `mods` folder like any other Fabric addon.

### Lezy settings

Every setting is reachable from two places:

**Through the BBS settings screen** (gear icon → BBS Lezy category):

| Setting                 | Range   | Description                                                                                                                                                                                                                          |
| ----------------------- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Enable render limit     | on/off  | Turns the whole render limit feature on or off.                                                                                                                                                                                      |
| Max rendered models     | 0–2000  | How many models stay rendered per frame. The rest are hidden. **0 disables it** (everything renders).                                                                                                                                 |
| Focus distance          | 0–256m  | Prioritizes models around this distance from the camera. **0 means nearest model wins.** Useful for watching distant replays without raising the render limit.                                                                   |
| Separate audio tracks   | on/off  | Export results carry two separate audio tracks (BBS clips + Minecraft sounds). Only applies when both the BBS **audio** and **minecraft sounds** export options are on. Off = one mixed track as usual.                             |
| Open folder on import   | on/off  | Automatically open File Explorer at the destination folder when files are dropped into BBS. Default: **off** (folder does not open by itself).                                                                                              |
| Baking batch percentage | 1–100%  | How aggressive the per-frame batch is, as a percentage of total replays processed during Look At baking. Higher finishes faster, lower gives a smoother and steadier progress bar (default: **5%**).                            |
| Video CQP (Quality)     | 0–51    | Constant Quantization Parameter. Lower means better quality, higher means a smaller file. **0 = lossless**, 18 = default (high quality), 23 = balanced, 28 = small file.                                                          |
| Video Codec             | choice  | `h264` (default, maximum compatibility), `h265` (HEVC, better compression), or `vp9` (WebM).                                                                                                                                        |
| Hardware Acceleration   | on/off  | Use the GPU encoder (NVIDIA NVENC / AMD AMF / Intel QSV) for much faster renders. Default: **on**.                                                                                                                                    |
| GPU Encoder             | choice  | Which vendor encoder to use: Auto-Detect (from the active OpenGL GPU), NVIDIA (NVENC), AMD (AMF), or Intel (QSV).                                                                                                                     |

**Through the film editor preview toolbar** — click the eye icon (next to the motion path button) to open a popup: on/off toggle, Render Limit slider, Focus Distance slider. Changes save themselves to `bbslezy.json`.

The settings file lives at `<BBS config folder>/bbs/settings/bbslezy.json`.

### Build from source

```bash
sh ./gradlew build
```

The result ends up in `build/libs/bbs-lezy-<version>.jar`.

> Note: BBS itself has to be published to maven local first (`sh ./gradlew publishToMavenLocal` from the BBS folder). This addon is locked to BBS 2.7-1.20.1 — a different BBS version makes the dev environment behave strangely.

### Technical notes

- **UI mixins are fail-safe**: `bbslezy.mixins.json` uses `required: false` + `defaultRequire: 0`. If BBS internals shift and a mixin fails, the addon still loads — you only lose the replay panel and audio features, while the Limit Replay feature stays safe because it goes through the official API. Mixin targets: `VideoExportSession` (two-track export), `AudioReader` (codecs), `ToWAVImporter`/`WAVImporter` (conversion-free import), `UISoundOverlayPanel` (audio browser), `UIScreen` (open folder on import toggle), `UIProcessReplaysPanel` (per-tick Look At and async baking), `UIFormUndoHandler` (single compound undo).
- **Override restore only happens in `RENDER_AFTER` and `SHUTDOWN`**, not in the normal render pass, because shadows and name tags are drawn after it.
- **The budget is computed as squared distance** — no `sqrt`, no per-form `Vec3d` allocation, so it stays cheap with thousands of actors.
- Forms locked to the camera (an `anchor` with a target) are never culled, matching stock BBS behavior.
- Editor clicks can still select an actor that is currently capped — the picking pass is skipped from culling.
- **ffmpeg stays external**: audio/video features shell out to an `ffmpeg` binary you provide (via `PATH` or BBS settings) as a separate process — no ffmpeg code or binary is linked or shipped with this mod.

## Credits

Standing on other people's work, so a big thank you to:

- [McHorse](https://www.youtube.com/@McHorsesCreations) — creator of BBS (Blockbuster), the original mod none of this exists without.
- [Wemmpy](https://www.youtube.com/@Wemppy4) — creator of BBS FS, the fork this addon is built for.
- [ElgatoPro300](https://www.youtube.com/@ElGatoPro300) — screen effect clips and illusion, ported from his BBS CML.

and the AI that was willing to be chopped up for it 😈

## License

MIT — free to use and modify. Bored of a bug or want to add a feature? Fork it yourself.
