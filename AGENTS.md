# BBS Lezy — AGENT RULES

always use caveman and ponytail skill

Add-on BBS FS: cap jumlah model yg dirender per frame (yg terdekat ke kamera menang),
biar scene ribuan actor gak membebani GPU selama pengerjaan.

1. **Selalu `git commit` setelah selesai mengerjakan task.** Tak ada pengecualian: setiap
   perubahan yang sampai ke `src/`, `build.gradle`, `*.json`, atau dokumen ini langsung
   di-commit di repo ini setelah task selesai. Jangan menumpuk perubahan tidak ter-commit.
2. **Jangan sentuh `bbsrc/`.** Repo BBS punya git repo sendiri dan kontraknya sendiri. Bila
   perlu publish ulang BBS, build dari sana tanpa mengubah file-nya (lihat COMMANDS).
3. **Add-on ini hanya boleh menyentuh kontrak `mchorse.bbs_mod.api` + `mchorse.bbs_mod.api.client`.**
   Mixin, raw GL, dan per-bone cull sudah dibuang — add-on ini murni event BBS.
4. **Restore override visible hanya di `FilmEvents.RENDER_AFTER` / `SHUTDOWN`**, bukan di
   `FormRenderEvents.AFTER` — shadow dan name tag digambar setelahnya.
5. **Sebelum yield, build harus jalan**: `sh ./gradlew build` dari sini.

## STRUCTURE

```
./
├── src/main/java/bbslod/
│   ├── BBSLod.java             # common entrypoint (bbs-addon), MOD_ID = "bbslezy"
│   ├── BBSLodClient.java       # client entrypoint (bbs-client-addon), wiring Fabric events
│   ├── LodSettings.java        # settings: enabled + render_limit
│   └── LodEngine.java          # ranking jarak + budget + override visible
├── src/main/resources/
│   └── fabric.mod.json         # id bbslezy, depends bbs >=2.6.0-1.20.1
├── build.gradle                # dari docs/addon-template + repo Modrinth + sodium
├── settings.gradle, gradle.properties   # dari template, versi dikunci ke BBS
├── bbs-publish-init.gradle     # work-around Gradle 9 publish BBS (lihat COMMANDS)
└── .gitignore
```

## CONVENTIONS

- Package `bbslod`, lowercase. Class PascalCase, field/method camelCase.
- Add-on bus (`EventBus`) hanya menemukan `@Subscribe` di **class entrypoint**. Handler di
  class lain (mis. `LodEngine`) kagak ketemu — register Fabric events dari `BBSLodClient`.
- `visible.setRuntimeValue(...)` adalah satu-satunya cara hide form tanpa clobber keyframe
  BBS sendiri (yang juga lewat runtimeValue).
- Build file `build.gradle` selain blok repositories/dependencies sodium adalah verbatim
  dari `docs/addon-template/build.gradle`. Jangan tambah konfigurasi tanpa alasan.
- Ranking dihitung di `FilmEvents.RENDER_AFTER` (sudah tau semua jarak form) lalu dipakai
  frame depan — jadi budget selalu telat 1 frame. Itu sengaja: full sort lebih murah daripada
  partial-select manual, dan 1 frame lag gak kelihatan.
- Budget disimpan sebagai **squared distance** (`double`) — gak ada sqrt, gak ada alloc
  `Vec3d` per form. `before()` bandingin squared, `onRenderAfter` hitung squared manual.
- `touched` (IdentityHashMap) melacak form yg di-override engine; restore via
  `clearOverrides` tiap RENDER_AFTER + SHUTDOWN.

## ANTI-PATTERNS

- Jangan simpan state per-form di static field tanpa reset per controller —
  `clearOverrides` wajib jalan tiap RENDER_AFTER, kagak boleh bocor ke film lain.
- Jangan restore di `FormRenderEvents.AFTER` (terlalu cepat, shadow/nametag belum gambar).
- Jangan cull form yang `form.anchor.get().hasTarget()` (camera-locked) — sama dengan
  precedent `BaseFilmController.isCulled`.
- Jangan skip pass picking: editor click harus tetap bisa select actor yg lagi di-cap.
  Guard `context.isPicking()` di `before()` buat itu.
- Jangan ubah `gradle.properties` versi selain dari `bbsrc/gradle.properties`.
- Jangan commit `run/`, `build/`, `.gradle/`, atau `*.log` (sudah di .gitignore).

## COMMANDS

```bash
# Publish BBS ke maven local (jalankan DARI bbsrc/, TANPA ubah file bbsrc):
sh ./gradlew publishToMavenLocal --init-script ../bbs-publish-init.gradle --no-daemon
# ^ work-around: Gradle 9 fail kalau duplicate jar entry; api/AGENTS.md ada di dua source
#   set BBS. Init script set duplicatesStrategy=EXCLUDE di semua task Jar.

# Build add-on (dari root project ini):
sh ./gradlew build --no-daemon

# Jalankan dev client (Sodium ikut; Iris tidak):
sh ./gradlew runClient --no-daemon

# Dependencies report:
sh ./gradlew dependencies --configuration runtimeClasspath --no-daemon
```

Versi (kunci dari `bbsrc/gradle.properties`): minecraft 1.20.1, yarn 1.20.1+build.10,
loader 0.16.14, fabric-api 0.92.1+1.20.1, sodium mc1.20.1-0.5.8.
BBS terpublish sebagai `mchorse:bbs:2.6.1-1.20.1` (versi = mod_version + "-" + mc_version).
Dependensi mod `bbs >=2.6.0-1.20.1` — add-on jalan di 2.6+ sampe BBS ada breaking change
API. `BBSApi.requireVersion(MOD_ID, 1)` di `onSourcePacks` jadi guard: mismatch baca
"add-on gak cocok build ini", bukan crash saat dipakai.

## NOTES

- `bbsrc/` adalah symlink ke `App/bbs-fs-F6-Fix`; wrapper resolve ke path fisik tapi
  build project yang sama. Project ini ada di root `bbs-lod-1.20.1`, dan `bbsrc` adalah symlink di dalamnya — jangan di-commit (sudah di-ignore).
- Dev client jalan TANPA Iris (shader kagak ketest); Sodium sudah include.
- Settings client ada di `run/config/bbs/settings/bbslezy.json` — juga editable via settings
  screen BBS. Default: enabled true, render_limit 100. `render_limit` 0 = mati (semua render).
- Matiin `enabled` pas mau render/record — fitur ini buat pengerjaan, bukan hasil akhir.
- Yang di-cull: root form milik controller (lewat `controller.getEntities()`). Body parts
  ikut parent karena kagak punya entri sendiri di map itu.
- Verifikasi behavioral hanya bisa manual di scene film asli: spawn ribuan actor, atur
  `render_limit`, liat yg jauh menghilang + yg dekat tetap.
