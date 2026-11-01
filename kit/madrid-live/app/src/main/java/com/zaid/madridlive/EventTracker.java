package com.zaid.madridlive;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns successive live snapshots of one match into alerts, each event exactly once.
 *
 * <p>Goals are driven by the score going up (the number every feed agrees on); the goal's details, when they are in
 * the feed, only name the scorer. Red cards, kick-off and full time come from the details and the state. The first
 * snapshot is a baseline and raises nothing, so restarting the service mid-match never replays old events. The state
 * survives process death ({@link #toJson()}).
 */
public final class EventTracker {
    private String matchId = "";
    private String state = "";
    private int home = -1;
    private int away = -1;
    private boolean ready;
    /** Keys of the details already used or known at the baseline. */
    private final Set<String> seen = new LinkedHashSet<>();

    public List<Alert> update(Snapshot snap) {
        List<Alert> out = new ArrayList<>();
        Match m = snap.match;
        if (!m.id.equals(matchId)) {
            matchId = m.id;
            ready = false;
            seen.clear();
        }
        if (!ready) {
            baseline(snap);
            return out;
        }
        String prev = state;
        if (Match.PRE.equals(prev) && Match.IN.equals(m.state)) {
            out.add(Alert.of(Alert.KICKOFF, m));
        }
        if (!Match.PRE.equals(m.state)) {
            goals(snap, out, true, home, m.homeScore);
            goals(snap, out, false, away, m.awayScore);
            reds(snap, out);
        }
        if (Match.IN.equals(prev) && Match.POST.equals(m.state)) {
            out.add(Alert.of(Alert.FULLTIME, m));
        }
        state = m.state;
        home = Math.max(0, m.homeScore);
        away = Math.max(0, m.awayScore);
        return out;
    }

    private void baseline(Snapshot snap) {
        Match m = snap.match;
        state = m.state;
        home = Math.max(0, m.homeScore);
        away = Math.max(0, m.awayScore);
        for (Detail d : snap.details) seen.add(d.key());
        ready = true;
    }

    private void goals(Snapshot snap, List<Alert> out, boolean homeSide, int before, int nowScore) {
        int diff = Math.max(0, nowScore) - Math.max(0, before);
        if (diff <= 0) return;   // no change, or a goal taken away by VAR: nothing to announce
        Match m = snap.match;
        String sideId = homeSide ? m.homeId : m.awayId;
        // Unannounced goals, the scoring side's first; the newest ones are the ones that just happened.
        List<Detail> mine = new ArrayList<>();
        List<Detail> other = new ArrayList<>();
        for (Detail d : snap.details) {
            if (d.kind != Detail.GOAL || seen.contains(d.key())) continue;
            (d.teamId.equals(sideId) ? mine : other).add(d);
        }
        List<Detail> pool = !mine.isEmpty() ? mine : other;
        int first = Math.max(0, pool.size() - diff);
        for (int i = 0; i < Math.min(diff, 4); i++) {
            Alert a = Alert.of(Alert.GOAL, m);
            a.team = homeSide ? m.home : m.away;
            int idx = first + i;
            if (idx < pool.size() && i < pool.size()) {
                Detail d = pool.get(idx);
                seen.add(d.key());
                a.player = d.player;
                a.penalty = d.penalty;
                a.ownGoal = d.ownGoal;
                if (!d.clock.isEmpty()) a.clock = d.clock;
            }
            out.add(a);
        }
    }

    private void reds(Snapshot snap, List<Alert> out) {
        Match m = snap.match;
        for (Detail d : snap.details) {
            if (d.kind != Detail.RED || !seen.add(d.key())) continue;
            Alert a = Alert.of(Alert.RED, m);
            a.player = d.player;
            a.team = d.teamId.equals(m.homeId) ? m.home : d.teamId.equals(m.awayId) ? m.away : "";
            if (!d.clock.isEmpty()) a.clock = d.clock;
            out.add(a);
        }
    }

    // ----------------------------------------------------------------------------------------- persistence

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", matchId);
            o.put("state", state);
            o.put("home", home);
            o.put("away", away);
            o.put("ready", ready);
            o.put("seen", new JSONArray(seen));
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /** A tracker from {@link #toJson()}; a fresh one when the text is empty or unreadable. */
    public static EventTracker fromJson(String json) {
        EventTracker t = new EventTracker();
        if (json == null || json.isEmpty()) return t;
        try {
            JSONObject o = new JSONObject(json);
            t.matchId = o.optString("id", "");
            t.state = o.optString("state", "");
            t.home = o.optInt("home", -1);
            t.away = o.optInt("away", -1);
            t.ready = o.optBoolean("ready", false);
            JSONArray seen = o.optJSONArray("seen");
            if (seen != null) for (int i = 0; i < seen.length(); i++) t.seen.add(seen.optString(i));
        } catch (Exception e) {
            return new EventTracker();
        }
        return t;
    }
}
