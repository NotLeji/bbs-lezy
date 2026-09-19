# BBS LOD — AGENT RULES

Add-on BBS FS: distance culling + tiered model simplification untuk scene film.
Mod Minecraft 1.20.1, Fabric, Java 17. Berdiri sendiri, dipasang bersama BBS.

1. **Selalu `git commit` setelah selesai mengerjakan task.** Tak ada pengecualian: setiap
   perubahan yang sampai ke `src/`, `build.gradle`, `*.json`, atau dokumen ini langsung
   di-commit di repo ini setelah task selesai. Jangan menumpuk perubahan tidak ter-commit.
2. **Jangan sentuh `bbsrc/`.** Repo BBS punya git repo sendiri dan kontraknya sendiri. Bila
   perlu publish ulang BBS, build dari sana tanpa mengubah file-nya (lihat COMMANDS).
3. **Add-on ini hanya boleh menyentuh kontrak `mchorse.bbs_mod.api` + `mchorse.bbs_mod.api.client`**,
   plus SATU client mixin (`CubicVAORendererMixin`) yang fail-safe by default.
4. **Jangan pernah mutate state `ModelGroup`/`Model`** — model di-cache dan dipakai bersama
   per asset key. Tentukan per-form, jangan ubah shared state.
5. **Restore override visible hanya di `FilmEvents.RENDER_AFTER` / `SHUTDOWN`**, bukan di
   `FormRenderEvents.AFTER` — shadow dan name tag digambar setelahnya.
6. **Sebelum yield, build harus jalan**: `sh ../bbsrc/gradlew build` dari sini.

## STRUCTURE

```
./
├── src/main/java/bbslod/
│   ├── BBSLod.java             # common entrypoint (bbs-addon)
│   ├── BBSLodClient.java       # client entrypoint (bbs-client-addon), wiring Fabric events
│   ├── LodSettings.java        # holder settings client
│   ├── LodEngine.java          # tier math + bookkeeping visible
│   ├── LodState.java           # stack tier per-render (dibaca mixin)
│   ├── LodBoneDepth.java       # cache depth ModelGroup
│   └── mixin/client/CubicVAORendererMixin.java  # tier-1 bone cull, fail-safe
├── src/main/resources/
│   ├── fabric.mod.json         # id bbslod, depends bbs >=2.6.1-1.20.1
│   └── bbslod.mixins.json      # required:false, defaultRequire:0
├── build.gradle                # dari docs/addon-template + repo Modrinth + sodium
├── settings.gradle, gradle.properties   # dari template, versi dikunci ke BBS
├── bbs-publish-init.gradle     # work-around Gradle 9 publish BBS (lihat COMMANDS)
└── .gitignore
```

## CONVENTIONS

- Package `bbslod`, lowercase. Class PascalCase, field/method camelCase.
- Add-on bus (`EventBus`) hanya menemukan `@Subscribe` di **class entrypoint**. Handler di
  class lain (mis. `LodEngine`) kagak ketemu — register Fabric events dari `BBSLodClient`.
- Value per-form pakai ID namespaced (`bbslod:cull_distance`) biar selamat saat add-on
  tidak terpasang. `-1F` = warisi setting global.
- `visible.setRuntimeValue(...)` adalah satu-satunya cara hide form tanpa clobber keyframe
  BBS sendiri (yang juga lewat runtimeValue).
- Mixin config `required: false` + `defaultRequire: 0`: BBS berubah → mixin no-op, add-on
  tetap jalan cull-only. Itu sengaja, jangan diubah jadi `required: true`.
- Build file `build.gradle` selain blok repositories/dependencies sodium adalah verbatim
  dari `docs/addon-template/build.gradle`. Jangan tambah konfigurasi tanpa alasan.

## ANTI-PATTERNS

- Jangan simpan state per-frame di static field tanpa stack — `FormUtilsClient.render`
  reentrant (body parts), field datar akan ditimpa nesting.
- Jangan restore di `FormRenderEvents.AFTER` (terlalu cepat, shadow/nametag belum gambar).
- Jangan cull form yang `form.anchor.get().hasTarget()` (camera-locked) — sama dengan
  precedent `BaseFilmController.isCulled`.
- Jangan ubah `gradle.properties` versi selain dari `bbsrc/gradle.properties`.
- Jangan commit `run/`, `build/`, `.gradle/`, atau `*.log` (sudah di .gitignore).

## COMMANDS

```bash
# Publish BBS ke maven local (dari bbsrc, TANPA ubah file bbsrc):
sh ./gradlew publishToMavenLocal --init-script ../bbs-lod/bbs-publish-init.gradle --no-daemon
# ^ work-around: Gradle 9 fail kalau duplicate jar entry; api/AGENTS.md ada di dua source
#   set BBS. Init script set duplicatesStrategy=EXCLUDE di semua task Jar.

# Build add-on (dari sini):
sh ../bbsrc/gradlew build --no-daemon

# Jalankan dev client (Sodium ikut; Iris tidak):
sh ../bbsrc/gradlew runClient --no-daemon

# Dependencies report:
sh ../bbsrc/gradlew dependencies --configuration runtimeClasspath --no-daemon
```

Versi (kunci dari `bbsrc/gradle.properties`): minecraft 1.20.1, yarn 1.20.1+build.10,
loader 0.16.14, fabric-api 0.92.1+1.20.1, sodium mc1.20.1-0.5.8.
BBS terpublish sebagai `mchorse:bbs:2.6.1-1.20.1` (versi = mod_version + "-" + mc_version).

## NOTES

- `bbsrc/` adalah symlink ke `App/bbs-fs-F6-Fix`; wrapper resolve ke path fisik tapi
  build project yang sama. Add-on ini sibling dari symlink tersebut.
- Dev client jalan TANPA Iris (shader kagak ketest); Sodium sudah include.
- Settings client ada di `run/config/bbs/settings/bbslod.json` — juga editable via settings
  screen BBS. Default: enabled, cull 128, simplify 64, bone_cull_depth 3, fov_bias true.
- Debug: set `debug: true` → log tiap 200 frame `frames/tier1/tier2/mixinHits`.
  `mixinHits = 0` padahal `tier1 > 0` → mixin tidak apply (target BBS pindah).
- Verifikasi behavioral (cull, simplify, shadow, picking, FOV bias, persistensi) hanya
  bisa manual di scene film asli — lihat plan `local://bbs-lod-addon-plan.md`.
