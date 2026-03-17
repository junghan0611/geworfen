#!/usr/bin/env bash
# geworfen — thrown into the world
set -euo pipefail
cd "$(dirname "$0")"

CMD="${1:-help}"
shift 2>/dev/null || true

ARCH=$(uname -m)
BINARY="target/geworfen-${ARCH}"

case "$CMD" in
  serve)
    # Run native binary (or JVM fallback)
    if [ -f "$BINARY" ]; then
      echo "Starting native binary: $BINARY"
      exec "$BINARY" "$@"
    else
      echo "No native binary found, using JVM..."
      exec clj -M:run "$@"
    fi
    ;;

  dev)
    exec clj -M:dev
    ;;

  build)
    OUTPUT=""
    FORCE=false
    ARGS=("$@")
    i=0
    while [ $i -lt ${#ARGS[@]} ]; do
      case "${ARGS[$i]}" in
        --output) i=$((i+1)); OUTPUT="${ARGS[$i]:-}" ;;
        --force)  FORCE=true ;;
        *)        [ -z "$OUTPUT" ] && OUTPUT="${ARGS[$i]}" ;;
      esac
      i=$((i+1))
    done

    if [ "$FORCE" = false ] && [ -f "${BINARY}" ]; then
      echo "✅ Cache hit: ${BINARY}"
    else
      NI_ARGS="--initialize-at-build-time --no-fallback"
      NI_ARGS="$NI_ARGS -H:+ReportExceptionStackTraces"
      NI_ARGS="$NI_ARGS -H:Name=geworfen-${ARCH}"
      NI_ARGS="$NI_ARGS -jar target/geworfen.jar"
      NI_ARGS="$NI_ARGS -o ${BINARY}"

      if command -v native-image &>/dev/null; then
        echo "=== GraalVM native-image build (${ARCH}) ==="
        clj -T:build uber
        # shellcheck disable=SC2086
        native-image $NI_ARGS
      else
        FHS_BIN="$(nix build .#fhs --no-link --print-out-paths 2>/dev/null)/bin/geworfen-build"
        if [ -x "$FHS_BIN" ]; then
          echo "=== FHS env → native-image build (${ARCH}) ==="
          "$FHS_BIN" -c "
            cd $(pwd)
            clj -T:build uber
            native-image $NI_ARGS
          "
        else
          echo "ERROR: native-image not found. Run inside: nix develop"
          exit 1
        fi
      fi
    fi

    echo "✅ Built: ${BINARY} ($(du -h "$BINARY" | cut -f1))"

    # Copy to output if specified
    if [ -n "$OUTPUT" ]; then
      mkdir -p "$(dirname "$OUTPUT")"
      cp "$BINARY" "$OUTPUT"
      echo "✅ Copied to: $OUTPUT"
    fi
    ;;

  clean)
    rm -rf target/
    echo "Cleaned target/"
    ;;

  *)
    echo "geworfen — thrown into the world"
    echo ""
    echo "Usage: ./run.sh <command>"
    echo ""
    echo "  serve              Start server (native or JVM fallback)"
    echo "  dev                Start nREPL (CIDER)"
    echo "  build [--force]    Build native binary via GraalVM"
    echo "  clean              Remove target/"
    echo ""
    ;;
esac
