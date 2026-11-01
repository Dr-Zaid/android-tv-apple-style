package com.zaid.madridlive;

import java.util.ArrayList;
import java.util.List;

/** Which match the home screen shows, and how a fresh schedule folds into the stored one. Pure. */
public final class MatchSelector {
    private MatchSelector() {}

    /** A finished match stays up for this long. */
    public static final long RECENT_MS = 3L * 60 * 60 * 1000;
    /** Kick-off to the final whistle when we never saw the whistle: 90 + half-time + stoppage, rounded up. */
    public static final long ASSUMED_LENGTH_MS = 2L * 60 * 60 * 1000;
    /** An "in" match this long after kick-off is stale data (the poll died), not a live game. */
    public static final long LIVE_MAX_MS = 5L * 60 * 60 * 1000;
    /** A "pre" match this long after kick-off was postponed or the feed never moved on. */
    public static final long PRE_OVERDUE_MS = 4L * 60 * 60 * 1000;

    /** When the match ended: the moment we saw it, else kick-off plus {@link #ASSUMED_LENGTH_MS}. */
    public static long endTime(Match m) {
        return m.endedMs > 0 ? m.endedMs : m.kickoffMs + ASSUMED_LENGTH_MS;
    }

    /** Live match, else a result less than 3 h old, else the next fixture; null when there is none. */
    public static Match select(List<Match> all, long now) {
        if (all == null) return null;
        Match live = null;
        Match recent = null;
        Match next = null;
        for (Match m : all) {
            if (m == null) continue;
            if (Match.IN.equals(m.state)) {
                if (now - m.kickoffMs < LIVE_MAX_MS && (live == null || m.kickoffMs > live.kickoffMs)) live = m;
            } else if (Match.POST.equals(m.state)) {
                long age = now - endTime(m);
                if (m.kickoffMs <= now && age < RECENT_MS && (recent == null || endTime(m) > endTime(recent))) recent = m;
            } else if (Match.PRE.equals(m.state)) {
                if (now - m.kickoffMs < PRE_OVERDUE_MS && (next == null || m.kickoffMs < next.kickoffMs)) next = m;
            }
        }
        if (live != null) return live;
        if (recent != null) return recent;
        return next;
    }

    /**
     * The stored matches of one competition after a refresh. The fresh list wins, except that what only we know
     * (the last event, when it ended) is carried over, and a match never goes backwards (a schedule feed that lags
     * behind the live poll must not turn a live or finished match back into "upcoming").
     */
    public static List<Match> merge(List<Match> old, List<Match> fresh) {
        List<Match> out = new ArrayList<>();
        for (Match f : fresh) {
            Match keep = f;
            Match o = find(old, f.id);
            if (o != null) {
                if (o.stateRank() > f.stateRank()) {
                    keep = o.copy();   // newer than the feed
                    keep.fetchedMs = Math.max(o.fetchedMs, f.fetchedMs);
                } else {
                    keep = f.copy();
                    keep.lastEvent = o.lastEvent;
                    keep.lastEventMs = o.lastEventMs;
                    if (keep.endedMs == 0 && Match.POST.equals(keep.state)) keep.endedMs = o.endedMs;
                }
            }
            out.add(keep);
        }
        return out;
    }

    private static Match find(List<Match> list, String id) {
        if (list == null) return null;
        for (Match m : list) if (m.id.equals(id)) return m;
        return null;
    }
}
