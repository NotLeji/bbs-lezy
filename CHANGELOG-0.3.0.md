## Changelog

### Version format changed: 1.x to 0.x

The version is now **0.3.0**, down from 1.2.0. This is intentional.

The screen effect clips and illusion features were ported from BBS CML, which means a
fair amount of the code reaches into BBS internals through mixins. Those internals move
between BBS releases, and some of the ports (per-track compositing in particular) are
adapted designs rather than finished ones. Calling that 1.x would promise a stability
guarantee this build does not have, and once you ship a 1.0 the version number becomes a
promise you have to keep across every BBS update. 0.x says the same thing without the
commitment. Bump to 1.0.0 once the mixin targets settle.

### New

- **Screen effect clips** — Cinematic Effect (vintage film, film grain, fisheye, chromatic
  aberration, VHS glitch, radial blur), Color Grade, Vignette, and Letterbox, all
  configurable per clip and animatable on the camera timeline. Ported from BBS CML.
- **Illusion** — visual duplication around a form or model block without adding entities.
  6 aim directions, spread, opacity fade, plus Uniform Distance, Real, Distort, and
  Gradual Transform. Keyframable via the Dope Sheet as the `illusion` and
  `illusion_transform` tracks. Ported from BBS CML.
- **Per-track clip hierarchy** — clip effects follow the camera timeline track order.
  Each populated track gets its own pass, bottom track first, so a Color Grade on an
  upper track reads as a layer on top of the lower track's grade instead of being summed
  into one look. Clips on the same track still stack. A film with everything on one track
  still costs a single pass, so there is no extra cost.
- **Subtitle and image overlays follow the same hierarchy** — overlays on an upper track
  now draw over the effects on lower tracks instead of always being buried under them.
- **Video CQP setting** — 0 to 51, default 18.
- **Video codec setting** — h264 (default), h265 (HEVC), or vp9 (WebM).
- **GPU codec warning** — VP9 has no hardware encoder on any consumer GPU. Selecting it
  with hardware acceleration on pops a dialog offering a CPU fallback for that one
  export, instead of failing silently mid-render.
- **Reset replay actors** — right-click menu in the replay list panel to respawn every
  replay actor back to its start and refresh the display.
- Bilingual README: English plus an Indonesian version, with a switcher at the top.

### Fixed

- **Action clips after a restart** — the damage clip stopped working after a rewind or
  restart because killed actors were dropped from the cast on the server and never
  rebuilt. The reset action now syncs the film so the cast respawns properly.
- **Overlay interleave** — BBS registers its image and subtitle renderers before the addon
  event fires, so the take-over now happens through reflection and actually takes effect.
  Previously the accessor was silently rejected and subtitles stayed buried under the
  color grade.

### Credits

Screen effect clips and illusion are ports of features from
[BBS CML](https://www.youtube.com/@ElGatoPro300) by
[ElgatoPro300](https://www.youtube.com/@ElGatoPro300). All credit for those two features
goes back to him.
