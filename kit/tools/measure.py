#!/usr/bin/env python3
"""Whole-stick speed test: every launchable app, the Settings screens and the network, not only Home.

For each app the TV lists (found at run time, so apps installed later are measured too):
  cold_ms   open after a force-stop        (am start -W TotalTime: until the app's first frame)
  warm_ms   open again while it is in memory
  home_ms   leave the app: Home's launcher back on screen (am start -W of the HOME intent)
  jank_pct, frame p50/p90/p99   smoothness while D-pad keys are pressed inside the app
For Settings screens (main, Wi-Fi, network, about, display, sound, apps, all-apps list, date) the same open time and
smoothness while scrolling. Network: ping to the router, to 1.1.1.1 and to youtube.com (includes DNS).
Also records free RAM and the system animation scales (Android's timers stop at the first frame, so the
open/close animation is not inside cold/warm/home; its length is window/transition scale x about 300 ms).

Only keys that move focus are pressed (Up/Down/Left/Right, never OK), so nothing is played, bought or changed.

Usage:
    TV_HOST=<tv ip> tools/venv/bin/python work/measure.py --out work/measure-before.json [--repeats 3]
    tools/venv/bin/python work/measure.py --compare work/measure-before.json work/measure-after.json
"""
import argparse
import json
import os
import re
import statistics
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LAUNCHER = "com.gathin.tvhome"
SKIP = {LAUNCHER, "com.google.android.tv"}            # the launcher itself; Live Channels (no tuner)
UNSAFE = re.compile(r"factory|fac\b|\.fac|\.at\.|test", re.I)   # never start factory/test packages
SETTINGS_PKG = "com.android.tv.settings"
SETTINGS_SCREENS = [
    ("Settings", "android.settings.SETTINGS"),
    ("Wi-Fi", "android.settings.WIFI_SETTINGS"),
    ("Network", "-n com.android.tv.settings/.connectivity.NetworkActivity"),
    ("About", "-n com.android.tv.settings/.about.AboutActivity"),
    ("All apps list", "-n com.android.tv.settings/.device.apps.AllAppsActivity"),
    ("Display", "android.settings.DISPLAY_SETTINGS"),
    ("Sound", "android.settings.SOUND_SETTINGS"),
    ("Apps", "android.settings.APPLICATION_SETTINGS"),
    ("Date & time", "android.settings.DATE_SETTINGS"),
]
APP_KEYS = "20 22 22 20 21 21 19 19"                    # down, right x2, down, left x2, up x2
SETTINGS_KEYS = "20 20 20 20 20 19 19 19 19 19"         # down x5, up x5


def connect():
    sys.path.insert(0, os.path.join(ROOT, "tools"))
    import tv  # noqa: E402  (reads TV_HOST at import)
    return tv.connect()


class Stick:
    def __init__(self):
        self.dev = connect()

    def reconnect(self):
        try:
            self.dev.close()
        except Exception:
            pass
        time.sleep(2.0)
        self.dev = connect()

    def sh(self, cmd, timeout=120.0):
        return self.dev.shell(cmd, read_timeout_s=timeout) or ""


def total_time(out):
    """TotalTime from `am start -W`, or None when the start failed."""
    if "Error" in out or "Status: ok" not in out and "Complete" not in out:
        return None
    m = re.search(r"TotalTime:\s*(\d+)", out)
    return int(m.group(1)) if m else None


def gfx(out):
    """Frames, jank share and frame-time percentiles from `dumpsys gfxinfo <pkg>`."""
    def num(pattern):
        m = re.search(pattern, out)
        return float(m.group(1)) if m else None
    return {
        "frames": num(r"Total frames rendered:\s*(\d+)"),
        "jank_pct": num(r"Janky frames:\s*\d+\s*\(([\d.]+)%\)"),
        "p50": num(r"50th percentile:\s*(\d+)ms"),
        "p90": num(r"90th percentile:\s*(\d+)ms"),
        "p99": num(r"99th percentile:\s*(\d+)ms"),
    }


def go_home(s):
    return total_time(s.sh("am start -W -a android.intent.action.MAIN -c android.intent.category.HOME"))


def discover(s):
    out = s.sh("cmd package query-activities --brief -a android.intent.action.MAIN "
               "-c android.intent.category.LEANBACK_LAUNCHER")
    apps, seen = [], set()
    for line in out.splitlines():
        m = re.match(r"^\s*([A-Za-z_][\w.]*)/([\w.$]+)\s*$", line)
        if not m:
            continue
        pkg = m.group(1)
        if pkg in SKIP or pkg in seen or UNSAFE.search(pkg):
            continue
        seen.add(pkg)
        apps.append(f"{pkg}/{m.group(2)}")
    return apps


def interact(s, pkg, keys):
    s.sh(f"dumpsys gfxinfo {pkg} reset")
    s.sh(f"for k in {keys}; do input keyevent $k; sleep 0.5; done")
    time.sleep(0.6)
    return gfx(s.sh(f"dumpsys gfxinfo {pkg}"))


def measure_app(s, comp, repeats):
    pkg = comp.split("/")[0]
    r = {"component": comp, "cold_ms": [], "warm_ms": [], "home_ms": [], "frames": []}
    for _ in range(repeats):
        go_home(s)
        s.sh(f"am force-stop {pkg}")
        time.sleep(1.0)
        r["cold_ms"].append(total_time(s.sh(f"am start -W -n {comp}")))
        time.sleep(4.0)                                    # let the app finish loading before the keys
        r["frames"].append(interact(s, pkg, APP_KEYS))
        r["home_ms"].append(go_home(s))
        time.sleep(1.0)
        r["warm_ms"].append(total_time(s.sh(f"am start -W -n {comp}")))
        time.sleep(1.5)
        go_home(s)
        time.sleep(0.5)
    s.sh(f"am force-stop {pkg}")
    return r


def measure_settings(s, repeats):
    res = {}
    for name, action in SETTINGS_SCREENS:
        r = {"action": action, "open_ms": [], "frames": [], "errors": 0}
        for i in range(repeats):
            try:
                go_home(s)
                if i == 0:
                    s.sh(f"am force-stop {SETTINGS_PKG}")
                    time.sleep(1.0)
                target = action if action.startswith("-n ") else f"-a {action}"
                r["open_ms"].append(total_time(s.sh(f"am start -W {target}")))
                time.sleep(1.5)
                r["frames"].append(interact(s, SETTINGS_PKG, SETTINGS_KEYS))
            except Exception as e:                     # an ADB timeout: note it, reconnect, carry on
                r["errors"] += 1
                print(f"  {name}: {type(e).__name__}, reconnecting", flush=True)
                s.reconnect()
        go_home(s)
        res[name] = r
    return res


def ping(s, host):
    out = s.sh(f"ping -c 5 -W 2 {host}", timeout=60)
    m = re.search(r"= [\d.]+/([\d.]+)/", out)
    loss = re.search(r"(\d+)% packet loss", out)
    return {"avg_ms": float(m.group(1)) if m else None, "loss_pct": int(loss.group(1)) if loss else None}


def med(values):
    v = [x for x in values if isinstance(x, (int, float))]
    return round(statistics.median(v), 1) if v else None


def summarize(raw):
    out = {"apps": {}, "settings": {}}
    for pkg, r in raw["apps"].items():
        out["apps"][pkg] = {
            "cold_ms": med(r["cold_ms"]), "warm_ms": med(r["warm_ms"]), "home_ms": med(r["home_ms"]),
            "jank_pct": med([f["jank_pct"] for f in r["frames"]]),
            "frame_p90_ms": med([f["p90"] for f in r["frames"]]),
        }
    for name, r in raw["settings"].items():
        out["settings"][name] = {
            "open_ms": med(r["open_ms"]),
            "jank_pct": med([f["jank_pct"] for f in r["frames"]]),
            "frame_p90_ms": med([f["p90"] for f in r["frames"]]),
        }
    return out


def run(args):
    s = Stick()
    m = re.search(r"via (\S+)", s.sh("ip route get 1.1.1.1"))
    router = m.group(1) if m else None
    raw = {
        "when": time.strftime("%Y-%m-%d %H:%M:%S"),
        "launcher_version": (re.search(r"versionName=(\S+)", s.sh(f"dumpsys package {LAUNCHER} | grep versionName")) or [None, None])[1],
        "animation_scales": {k: s.sh(f"settings get global {k}").strip()
                             for k in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale")},
        "apps": {}, "settings": {},
    }
    apps = discover(s) if args.only in (None, "apps") else []
    print(f"{len(apps)} apps: " + ", ".join(a.split('/')[0] for a in apps), flush=True)
    for comp in apps:
        pkg = comp.split("/")[0]
        try:
            raw["apps"][pkg] = measure_app(s, comp, args.repeats)
            print(f"  {pkg}: done", flush=True)
        except Exception as e:                              # one stubborn app must not end the run
            raw["apps"][pkg] = {"component": comp, "error": str(e), "cold_ms": [], "warm_ms": [], "home_ms": [], "frames": []}
            print(f"  {pkg}: {e}", flush=True)
            s.reconnect()
    raw["settings"] = measure_settings(s, args.repeats) if args.only in (None, "settings") else {}
    raw["network"] = {"router": ping(s, router) if router else None, "1.1.1.1": ping(s, "1.1.1.1"),
                      "youtube.com": ping(s, "www.youtube.com")}
    m = re.search(r"Free RAM:\s*([\d,]+)K", s.sh("dumpsys meminfo | grep 'Free RAM'"))
    raw["free_ram_mb"] = round(int(m.group(1).replace(",", "")) / 1024) if m else None
    go_home(s)
    raw["summary"] = summarize(raw)
    with open(args.out, "w") as f:
        json.dump(raw, f, indent=1)
    print_table(raw)
    print(f"\nsaved {args.out}")


def print_table(raw):
    sm = raw["summary"]
    print(f"\nAnimation scales: {raw['animation_scales']}   free RAM: {raw.get('free_ram_mb')} MB")
    print(f"\n{'App':38} {'cold':>7} {'warm':>7} {'home':>7} {'jank%':>6} {'p90':>5}")
    for pkg, v in sm["apps"].items():
        print(f"{pkg:38} {v['cold_ms']!s:>7} {v['warm_ms']!s:>7} {v['home_ms']!s:>7} {v['jank_pct']!s:>6} {v['frame_p90_ms']!s:>5}")
    print(f"\n{'Settings screen':38} {'open':>7} {'jank%':>6} {'p90':>5}")
    for name, v in sm["settings"].items():
        print(f"{name:38} {v['open_ms']!s:>7} {v['jank_pct']!s:>6} {v['frame_p90_ms']!s:>5}")
    print("\nNetwork:", json.dumps(raw.get("network")))


def compare(a_path, b_path):
    a, b = json.load(open(a_path)), json.load(open(b_path))

    def row(label, x, y):
        if x is None or y is None:
            return f"| {label} | {x} | {y} | |"
        d = (y - x) / x * 100 if x else 0
        return f"| {label} | {x} | {y} | {d:+.0f}% |"

    lines = [f"Before: {a['when']}  After: {b['when']}",
             f"Animation scales: {a['animation_scales']} -> {b['animation_scales']}",
             f"Free RAM: {a.get('free_ram_mb')} MB -> {b.get('free_ram_mb')} MB", "",
             "| App: metric | Before | After | Change |", "|---|---|---|---|"]
    for pkg in sorted(set(a["summary"]["apps"]) | set(b["summary"]["apps"])):
        x, y = a["summary"]["apps"].get(pkg, {}), b["summary"]["apps"].get(pkg, {})
        for k in ("cold_ms", "warm_ms", "home_ms", "jank_pct"):
            lines.append(row(f"{pkg}: {k}", x.get(k), y.get(k)))
    lines += ["", "| Settings: metric | Before | After | Change |", "|---|---|---|---|"]
    for name in a["summary"]["settings"]:
        x, y = a["summary"]["settings"][name], b["summary"]["settings"].get(name, {})
        for k in ("open_ms", "jank_pct"):
            lines.append(row(f"{name}: {k}", x.get(k), y.get(k)))
    print("\n".join(lines))


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--out", default="work/measure.json")
    p.add_argument("--repeats", type=int, default=3)
    p.add_argument("--only", choices=["apps", "settings"], help="measure only that part (network and RAM always)")
    p.add_argument("--compare", nargs=2, metavar=("BEFORE", "AFTER"))
    a = p.parse_args()
    compare(*a.compare) if a.compare else run(a)
