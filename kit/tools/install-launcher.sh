#!/usr/bin/env bash
# Rebuild the launcher from launcher/ (after editing config.xml) and install it on the stick as Home.
# Usage: work/install-launcher.sh [tv ip]
# Network ADB must be on (Settings > Device Preferences > Developer options > USB debugging).
set -euo pipefail
cd "$(dirname "$0")/.."
export TV_HOST="${1:-${TV_HOST:?usage: install-launcher.sh <tv ip>}}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export HOME_KEY_SERVICE=1   # this stick's Home button sends ALL_APPS; Android drops the helper on every reinstall
launcher/build-and-install.sh
