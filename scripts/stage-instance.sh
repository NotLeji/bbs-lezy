#!/usr/bin/env bash
# Stage Prism 'bbs' instance mods + config into run/ so a local headless
# (mc-runtime-test / HeadlessMC) run sees the same mod set and settings.
# run/ is gitignored; nothing here is committed.
# Usage: ./scripts/stage-instance.sh [--mods-only | --config-only]
set -euo pipefail

SRC="$HOME/.local/share/PrismLauncher/instances/bbs/minecraft"
DST="run"
MODE="${1:-all}"

if [ ! -d "$SRC/mods" ]; then
    echo "instance not found: $SRC" >&2
    exit 1
fi

if [ "$MODE" = "all" ] || [ "$MODE" = "--mods-only" ]; then
    mkdir -p "$DST/mods"
    # Verbatim copy incl. .disabled jars and .index, except what the build stages itself.
    for f in "$SRC"/mods/*; do
        base="$(basename "$f")"
        case "$base" in
            bbs-*.jar|bbs-lezy-*.jar|bbs_lezy-*.jar|fabric-api-*.jar) continue ;;
            *) cp -f "$f" "$DST/mods/" ;;
        esac
    done
    echo "mods staged: $(ls "$DST/mods" | wc -l) files"
fi

if [ "$MODE" = "all" ] || [ "$MODE" = "--config-only" ]; then
    mkdir -p "$DST/config"
    # Same configs, minus the heavy user payload the test never reads.
    rsync -a --delete \
        --exclude 'bbs/assets/' \
        --exclude 'bbs/movies/' \
        --exclude 'bbs/screenshots/' \
        --exclude 'bbs/thumbnails/' \
        "$SRC/config/" "$DST/config/"
    # Top-level client files the run also mirrors.
    for f in options.txt servers.dat; do
        [ -f "$SRC/$f" ] && cp -f "$SRC/$f" "$DST/$f"
    done
    echo "config staged: $(du -sh "$DST/config" | cut -f1)"
fi
