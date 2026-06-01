#!/bin/sh
set -e

apk add --no-cache curl jq >/dev/null 2>&1 || true

git config --global --add safe.directory "$WORKSPACE"

VERSION="v$(date +%Y.%m.%d)-$(git rev-parse --short HEAD)"

PREV_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

if [ -z "$PREV_TAG" ]; then
    LOG=$(git log --pretty=format:"%s|%an")
else
    LOG=$(git log ${PREV_TAG}..HEAD --pretty=format:"%s|%an")
fi

FEATURES=$(echo "$LOG" | grep -i "^feat" | sed 's/|/ — /' | sed 's/^/- /' || true)
FIXES=$(echo "$LOG"    | grep -i "^fix"  | sed 's/|/ — /' | sed 's/^/- /' || true)
OTHERS=$(echo "$LOG"   | grep -iv "^feat\\|^fix" | sed 's/|/ — /' | sed 's/^/- /' || true)

BODY="## CircleGuard Release $VERSION\n\n"

if [ -n "$FEATURES" ]; then
    BODY="${BODY}### New Features\n${FEATURES}\n\n"
fi
if [ -n "$FIXES" ]; then
    BODY="${BODY}### Bug Fixes\n${FIXES}\n\n"
fi
if [ -n "$OTHERS" ]; then
    BODY="${BODY}### Other Changes\n${OTHERS}\n\n"
fi

BODY="${BODY}---\n_Deployed by Jenkins build #${BUILD_NUMBER} on $(date '+%Y-%m-%d %H:%M:%S')_"

curl -s -X POST "https://api.github.com/repos/${REPO}/releases" \
    -H "Authorization: token $GITHUB_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$(jq -n \
        --arg tag "$VERSION" \
        --arg name "Release $VERSION" \
        --arg body "$BODY" \
        '{tag_name: $tag, name: $name, body: $body, draft: false, prerelease: false}')"

echo "Release $VERSION created successfully"
