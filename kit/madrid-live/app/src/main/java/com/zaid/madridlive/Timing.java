package com.zaid.madridlive;

import java.util.List;

/** All the clock arithmetic: when to wake up, when to poll, how long to wait after a failure. Pure. */
public final class Timing {
    private Timing() {}

    public static final long MINUTE = 60_000L;
    public static final long HOUR = 60 * MINUTE;

    /** The live service starts this long before kick-off. */
    public static final long LEAD_MS = 10 * MINUTE;
    /** ...and stops this long after the match is over. */
    public static final long TAIL_MS = 10 * MINUTE;
    /** Schedule refresh period, and the age at which a provider query kicks a refresh. */
    public static final long REFRESH_MS = 6 * HOUR;
    /** Live poll period. */
    public static final long POLL_MS = 30_000L;

    /** 2026-01-01 UTC: a TV with no network time yet reports 1970; an alarm computed against that would be wrong. */
    private static final long MIN_SANE_MS = 1_767_225_600_000L;

    public static boolean isClockSane(long now) {
        return now >= MIN_SANE_MS;
    }

    /** When the exact alarm for this match goes off. */
    public static long kickoffAlarmAt(Match m) {
        return m.kickoffMs - LEAD_MS;
    }

    /**
     * The next kick-off alarm to arm: the earliest not-yet-started match whose alarm time is still ahead. 0 if none
     * (a match already inside its window is handled by {@link #shouldPoll}, not by an alarm).
     */
    public static long nextKickoffAlarm(List<Match> all, long now) {
        long best = 0;
        if (all == null) return 0;
        for (Match m : all) {
            if (m == null || !Match.PRE.equals(m.state)) continue;
            long at = kickoffAlarmAt(m);
            if (at > now && (best == 0 || at < best)) best = at;
        }
        return best;
    }

    /** Should the live service be polling for this match right now? */
    public static boolean shouldPoll(Match m, long now) {
        if (m == null || now < kickoffAlarmAt(m)) return false;
        if (Match.POST.equals(m.state)) return now < MatchSelector.endTime(m) + TAIL_MS;
        if (Match.IN.equals(m.state)) return now - m.kickoffMs < MatchSelector.LIVE_MAX_MS;
        return now - m.kickoffMs < MatchSelector.PRE_OVERDUE_MS;
    }

    /** True when the stored schedule is older than six hours (or never fetched, or from the future). */
    public static boolean isStale(long lastRefreshMs, long now) {
        return lastRefreshMs <= 0 || lastRefreshMs > now || now - lastRefreshMs >= REFRESH_MS;
    }

    /** Wait before retrying a failed schedule refresh: 1, 2, 4 ... minutes, capped at 30. */
    public static long refreshBackoffMs(int failures) {
        int f = Math.max(1, Math.min(failures, 10));
        return Math.min(30 * MINUTE, MINUTE << (f - 1));
    }

    /** Wait before the next live poll: 30 s normally, doubling per consecutive failure up to 5 minutes. */
    public static long pollDelayMs(int consecutiveFailures) {
        if (consecutiveFailures <= 0) return POLL_MS;
        int f = Math.min(consecutiveFailures, 4);
        return Math.min(5 * MINUTE, POLL_MS << f);
    }
}
