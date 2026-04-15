#!/bin/bash
# Download hiddify-core (sing-box) AAR for VLessGram build.
# This avoids storing the 100+ MB binary in git.
#
# Usage: ./scripts/download-singbox.sh [version]

set -e

CORE_VERSION="${1:-4.1.0}"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$( cd "${SCRIPT_DIR}/.." && pwd )"
LIBS_DIR="${PROJECT_ROOT}/TMessagesProj/libs/singbox"
AAR_PATH="${LIBS_DIR}/hiddify-core.aar"
TARBALL_URL="https://github.com/hiddify/hiddify-next-core/releases/download/v${CORE_VERSION}/hiddify-lib-android.tar.gz"

if [ -f "${AAR_PATH}" ]; then
    echo "[singbox] AAR already present: ${AAR_PATH}"
    echo "[singbox] Delete it and re-run to force re-download."
    exit 0
fi

mkdir -p "${LIBS_DIR}"

TMP_DIR="$(mktemp -d)"
trap "rm -rf ${TMP_DIR}" EXIT

echo "[singbox] Downloading hiddify-core v${CORE_VERSION}..."
curl -L -o "${TMP_DIR}/hiddify-lib-android.tar.gz" "${TARBALL_URL}"

echo "[singbox] Extracting AAR..."
tar -xzf "${TMP_DIR}/hiddify-lib-android.tar.gz" -C "${TMP_DIR}"

# The tarball produces hiddify-core.aar at the root of the extraction
if [ ! -f "${TMP_DIR}/hiddify-core.aar" ]; then
    echo "[singbox] ERROR: hiddify-core.aar not found in tarball"
    exit 1
fi

mv "${TMP_DIR}/hiddify-core.aar" "${AAR_PATH}"
echo "[singbox] OK: ${AAR_PATH} ($(du -h "${AAR_PATH}" | cut -f1))"
