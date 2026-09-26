#!/usr/bin/env bash
set -euo pipefail

: "${GH_TOKEN:?GH_TOKEN is required}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${RELEASE_SHA:?RELEASE_SHA is required}"

# Android is the release gate because it owns the signed APKs. Web and Apple
# must not create a partial public release while the Android matrix is still
# running or when its signing/build has failed.
for attempt in $(seq 1 90); do
  runs="$(gh run list --workflow android-release.yml --limit 20 --json headBranch,headSha,status,conclusion 2>/dev/null || echo '[]')"
  state="$(printf '%s' "$runs" | jq -r --arg tag "$RELEASE_TAG" --arg sha "$RELEASE_SHA" '
    [.[] | select(.headBranch == $tag and .headSha == $sha)][0]
    | if . == null then "missing" else (.status + ":" + (.conclusion // "")) end
  ')"
  if [[ "$state" == completed:failure || "$state" == completed:cancelled || "$state" == completed:timed_out ]]; then
    echo "Android release workflow ended with ${state}; refusing a partial release." >&2
    exit 1
  fi

  release_json="$(gh api "repos/${GITHUB_REPOSITORY}/releases/tags/${RELEASE_TAG}" 2>/dev/null || true)"
  if [[ "$state" == completed:success ]] && [ -n "$release_json" ] && printf '%s' "$release_json" | jq -e '
      [.assets[].name] as $names
      | ($names | index("Velora-mobile-release.apk")) != null
      and ($names | index("Velora-tv-release.apk")) != null
    ' >/dev/null; then
    echo "Android signed release assets are available for ${RELEASE_TAG}."
    exit 0
  fi

  echo "Waiting for signed Android assets (${attempt}/90; Android run: ${state}; commit: ${RELEASE_SHA})."
  sleep 30
done

echo "Timed out waiting for signed Android release assets; refusing a partial release." >&2
exit 1
