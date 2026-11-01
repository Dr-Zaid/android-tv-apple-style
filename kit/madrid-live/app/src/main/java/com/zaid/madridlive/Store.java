package com.zaid.madridlive;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The app's small memory: the stored matches (per competition, so one failed league keeps its old data), when the
 * schedule was last refreshed, the failure count for the backoff, the live tracker and the demo row. One
 * SharedPreferences file; every access is synchronized because the provider, the receivers and the service all run in
 * one process on different threads.
 */
public final class Store {
    private Store() {}

    private static final String PREFS = "madrid_live";
    private static final String K_LEAGUES = "leagues";
    private static final String K_REFRESHED = "refreshed_ms";
    private static final String K_FAILURES = "failures";
    private static final String K_NEXT_RETRY = "next_retry_ms";
    private static final String K_TRACKER = "tracker";
    private static final String K_DEMO = "demo";
    private static final String K_DEMO_UNTIL = "demo_until_ms";

    /** Parsed copy of the stored matches, so a provider query does not parse JSON every time. Null = not loaded. */
    private static Map<String, List<Match>> cache;

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Map<String, List<Match>> load(Context c) {
        if (cache != null) return cache;
        Map<String, List<Match>> map = new LinkedHashMap<>();
        try {
            String raw = prefs(c).getString(K_LEAGUES, "");
            if (!raw.isEmpty()) {
                JSONObject root = new JSONObject(raw);
                JSONArray names = root.names();
                for (int i = 0; names != null && i < names.length(); i++) {
                    String league = names.getString(i);
                    JSONArray arr = root.optJSONArray(league);
                    List<Match> list = new ArrayList<>();
                    for (int j = 0; arr != null && j < arr.length(); j++) {
                        Match m = Match.fromJson(arr.optJSONObject(j));
                        if (m != null) list.add(m);
                    }
                    map.put(league, list);
                }
            }
        } catch (Exception ignored) {
            map.clear();   // corrupt file: start empty, the next refresh refills it
        }
        cache = map;
        return map;
    }

    private static void save(Context c, Map<String, List<Match>> map) {
        try {
            JSONObject root = new JSONObject();
            for (Map.Entry<String, List<Match>> e : map.entrySet()) {
                JSONArray arr = new JSONArray();
                for (Match m : e.getValue()) arr.put(m.toJson());
                root.put(e.getKey(), arr);
            }
            prefs(c).edit().putString(K_LEAGUES, root.toString()).apply();
        } catch (Exception ignored) {
            // keep the in-memory copy; try again on the next write
        }
    }

    /** All stored matches of every competition, soonest first. */
    public static synchronized List<Match> all(Context c) {
        List<Match> out = new ArrayList<>();
        for (List<Match> l : load(c).values()) out.addAll(l);
        Collections.sort(out, new Comparator<Match>() {
            @Override public int compare(Match a, Match b) { return Long.compare(a.kickoffMs, b.kickoffMs); }
        });
        return out;
    }

    /** Replace one competition's matches with a fresh read (merged with what we already know). */
    public static synchronized void putLeague(Context c, String league, List<Match> fresh) {
        Map<String, List<Match>> map = load(c);
        map.put(league, MatchSelector.merge(map.get(league), fresh));
        save(c, map);
    }

    /** A live read of one match: write it over the stored one (same id), keeping what only we know. */
    public static synchronized void updateMatch(Context c, Match m) {
        Map<String, List<Match>> map = load(c);
        List<Match> list = map.get(m.league);
        if (list == null) {
            list = new ArrayList<>();
            map.put(m.league, list);
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(m.id)) {
                list.set(i, m);
                save(c, map);
                return;
            }
        }
        list.add(m);
        save(c, map);
    }

    /** The match the home screen should show now (demo row first while a demo runs). */
    public static synchronized Match current(Context c, long now) {
        Match demo = demo(c, now);
        if (demo != null) return demo;
        return MatchSelector.select(all(c), now);
    }

    /** The match the live service follows: never the demo. */
    public static synchronized Match real(Context c, long now) {
        return MatchSelector.select(all(c), now);
    }

    // ----------------------------------------------------------------------------------------- refresh state

    public static synchronized long lastRefreshMs(Context c) {
        return prefs(c).getLong(K_REFRESHED, 0);
    }

    public static synchronized void refreshOk(Context c, long now) {
        prefs(c).edit().putLong(K_REFRESHED, now).putInt(K_FAILURES, 0).putLong(K_NEXT_RETRY, 0).apply();
    }

    /** Records a failed refresh and returns the failure count so far. */
    public static synchronized int refreshFailed(Context c, long nextRetryMs) {
        SharedPreferences p = prefs(c);
        int n = p.getInt(K_FAILURES, 0) + 1;
        p.edit().putInt(K_FAILURES, n).putLong(K_NEXT_RETRY, nextRetryMs).apply();
        return n;
    }

    public static synchronized int failures(Context c) {
        return prefs(c).getInt(K_FAILURES, 0);
    }

    public static synchronized long nextRetryMs(Context c) {
        return prefs(c).getLong(K_NEXT_RETRY, 0);
    }

    // ----------------------------------------------------------------------------------------- tracker

    public static synchronized EventTracker tracker(Context c) {
        return EventTracker.fromJson(prefs(c).getString(K_TRACKER, ""));
    }

    public static synchronized void saveTracker(Context c, EventTracker t) {
        prefs(c).edit().putString(K_TRACKER, t.toJson()).apply();
    }

    // ----------------------------------------------------------------------------------------- demo

    public static synchronized void setDemo(Context c, Match m, long untilMs) {
        prefs(c).edit().putString(K_DEMO, m.toJson().toString()).putLong(K_DEMO_UNTIL, untilMs).apply();
    }

    public static synchronized void clearDemo(Context c) {
        prefs(c).edit().remove(K_DEMO).remove(K_DEMO_UNTIL).apply();
    }

    /** The fake live row while a demo is running, else null. */
    public static synchronized Match demo(Context c, long now) {
        SharedPreferences p = prefs(c);
        long until = p.getLong(K_DEMO_UNTIL, 0);
        if (until <= now) return null;
        try {
            return Match.fromJson(new JSONObject(p.getString(K_DEMO, "")));
        } catch (Exception e) {
            return null;
        }
    }
}
