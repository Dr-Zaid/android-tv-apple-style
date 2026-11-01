#!/usr/bin/env bash
# Quick Home smoothness probe: 20 D-pad presses on Home, then the launcher's janky-frame share and frame p90.
cd "$(dirname "$0")/.."
: "${TV_HOST:?set TV_HOST to the TV IP}"; TV_HOST="$TV_HOST" tools/venv/bin/python tools/tv.py 'input keyevent HOME; sleep 2; dumpsys gfxinfo com.gathin.tvhome reset >/dev/null; for k in 22 22 22 22 22 21 21 21 21 21 20 19 22 22 21 21 20 19 22 21; do input keyevent $k; sleep 0.45; done; sleep 1; dumpsys gfxinfo com.gathin.tvhome | grep -E "Total frames|Janky|90th"' | tr '\n' ' '; echo
