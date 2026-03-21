#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v gh >/dev/null 2>&1 && [ ! -x "$HOME/.local/bin/gh" ]; then
  echo "gh is not installed. Install it first." >&2
  exit 1
fi

GH_BIN="${GH_BIN:-}"
if [ -z "$GH_BIN" ]; then
  if command -v gh >/dev/null 2>&1; then
    GH_BIN="$(command -v gh)"
  else
    GH_BIN="$HOME/.local/bin/gh"
  fi
fi

VERSION="$(awk -F'"' '/^version = "/ { print $2; exit }' build.gradle.kts)"
if [ -z "$VERSION" ]; then
  echo "Could not parse version from build.gradle.kts" >&2
  exit 1
fi

TAG="v$VERSION"
JAR_PATH="plugin-bootstrap/build/libs/Diamond-SMP-$VERSION.jar"
echo "Preparing release for version $VERSION ($TAG)"

"$GH_BIN" auth status >/dev/null
./gradlew build

if [ ! -f "$JAR_PATH" ]; then
  echo "Expected release jar not found: $JAR_PATH" >&2
  exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet || [ -n "$(git ls-files --others --exclude-standard)" ]; then
  echo "Committing local changes for $TAG"
  git add -A
  if ! git diff --cached --quiet; then
    git commit -m "Release $TAG"
  fi
fi

if git rev-parse "$TAG" >/dev/null 2>&1; then
  TAG_COMMIT="$(git rev-list -n 1 "$TAG")"
  HEAD_COMMIT="$(git rev-parse HEAD)"
  if [ "$TAG_COMMIT" != "$HEAD_COMMIT" ]; then
    echo "Tag $TAG already exists on a different commit ($TAG_COMMIT)." >&2
    echo "Move or delete the tag manually before rerunning." >&2
    exit 1
  fi
else
  git tag "$TAG"
fi

git push origin main --follow-tags
"$GH_BIN" release view "$TAG" >/dev/null 2>&1 || "$GH_BIN" release create "$TAG" \
  --repo Papaya-Voldemort/Diamond-SMP \
  --title "$TAG" \
  --generate-notes
"$GH_BIN" release upload "$TAG" "$JAR_PATH" \
  --repo Papaya-Voldemort/Diamond-SMP \
  --clobber

echo "Released $TAG"
