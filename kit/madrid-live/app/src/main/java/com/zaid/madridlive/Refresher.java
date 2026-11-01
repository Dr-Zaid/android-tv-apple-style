package com.zaid.madridlive;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Downloads the schedules of every competition, merges them into the {@link Store}, re-arms the alarms. */
public final class Refresher {
    private Refresher() {}

    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static volatile long lastKickMs;
    /** Upcoming matches kept per competition, and results kept. */
    private static final int KEEP_UPCOMING = 4;
    private static final int KEEP_RESULTS = 3;
    private static final long WHOLE_REFRESH_S = 40;

    /**
     * Refresh now, on the calling thread (so not the main thread). Does nothing if one is already running.
     * Returns true if every competition answered.
     */
    public static boolean refreshNow(Context ctx) {
        if (!RUNNING.compareAndSet(false, true)) return false;
        Context c = ctx.getApplicationContext();
        boolean allOk = false;
        try {
            long now = System.currentTimeMillis();
            String before = rowFingerprint(c, now);
            allOk = fetchAll(c, now);
            Match shown = Store.current(c, System.currentTimeMillis());
            if (shown != null) {
                LogoCache.ensure(c, shown.homeId, shown.homeLogo);
                LogoCache.ensure(c, shown.awayId, shown.awayLogo);
            }
            Scheduler.rescheduleAll(c);
            if (!before.equals(rowFingerprint(c, System.currentTimeMillis()))) Updates.changed(c);
            Scheduler.ensureLive(c);
        } catch (Throwable t) {
            // never crash a receiver or a provider thread
        } finally {
            RUNNING.set(false);
        }
        return allOk;
    }

    /** From the provider: if the data is over six hours old, refresh in the background. Never blocks the caller. */
    public static void kickIfStale(final Context ctx) {
        try {
            long now = System.currentTimeMillis();
            if (!Timing.isStale(Store.lastRefreshMs(ctx), now)) return;
            if (now < Store.nextRetryMs(ctx)) return;           // backing off after a failure
            if (now - lastKickMs < Timing.MINUTE) return;       // one kick a minute at most
            if (RUNNING.get()) return;
            lastKickMs = now;
            Thread t = new Thread(new Runnable() {
                @Override public void run() { refreshNow(ctx); }
            }, "madrid-refresh");
            t.setDaemon(true);
            t.start();
        } catch (Throwable ignored) {
            // the query that called us must still answer
        }
    }

    private static String rowFingerprint(Context c, long now) {
        Match m = Store.current(c, now);
        if (m == null) return "";
        Match f = m.copy();
        f.fetchedMs = 0;
        return f.toJson().toString();
    }

    private static boolean fetchAll(final Context c, final long now) {
        if (!Timing.isClockSane(now)) {
            fail(c, now);
            return false;
        }
        ExecutorService pool = Executors.newFixedThreadPool(Espn.LEAGUES.length, new ThreadFactory() {
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "madrid-fetch");
                t.setDaemon(true);
                return t;
            }
        });
        boolean allOk = true;
        try {
            List<Future<List<Match>>> futures = new ArrayList<>();
            for (final String[] league : Espn.LEAGUES) {
                futures.add(pool.submit(new Callable<List<Match>>() {
                    @Override public List<Match> call() throws Exception {
                        return fetchLeague(league[0], now);
                    }
                }));
            }
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(WHOLE_REFRESH_S);
            for (int i = 0; i < futures.size(); i++) {
                try {
                    long left = Math.max(1, deadline - System.nanoTime());
                    List<Match> list = futures.get(i).get(left, TimeUnit.NANOSECONDS);
                    Store.putLeague(c, Espn.LEAGUES[i][0], list);
                } catch (Exception e) {
                    allOk = false;
                    futures.get(i).cancel(true);
                }
            }
        } finally {
            pool.shutdownNow();
        }
        if (allOk) {
            Store.refreshOk(c, now);
        } else {
            // Leagues that answered are stored; the others keep their old data and are retried on the backoff.
            fail(c, now);
        }
        return allOk;
    }

    private static void fail(Context c, long now) {
        Store.refreshFailed(c, now + Timing.refreshBackoffMs(Store.failures(c) + 1));
    }

    /** One competition: the upcoming fixtures and the results, trimmed to what the home screen could ever show. */
    static List<Match> fetchLeague(String league, long now) throws Exception {
        List<Match> upcoming = fetchFeed(league, true, now);
        List<Match> results = fetchFeed(league, false, now);
        return trim(upcoming, results);
    }

    private static List<Match> fetchFeed(String league, boolean fixtures, long now) throws Exception {
        String body;
        try {
            body = Http.getString(Espn.scheduleUrl(league, fixtures));
        } catch (Http.StatusException e) {
            if (e.code == 404) return new ArrayList<>();   // this competition has no schedule for the team right now
            throw e;
        }
        return EspnParser.parseSchedule(body, league, now);   // the body is garbage after this, nothing keeps it
    }

    /** The next few not-finished matches and the last few results, no duplicates. */
    static List<Match> trim(List<Match> upcoming, List<Match> results) {
        Map<String, Match> byId = new LinkedHashMap<>();
        List<Match> pending = new ArrayList<>();
        List<Match> done = new ArrayList<>();
        for (List<Match> l : java.util.Arrays.asList(upcoming, results)) {
            for (Match m : l) {
                if (byId.put(m.id, m) != null) continue;
                (Match.POST.equals(m.state) ? done : pending).add(m);
            }
        }
        Comparator<Match> byDate = new Comparator<Match>() {
            @Override public int compare(Match a, Match b) { return Long.compare(a.kickoffMs, b.kickoffMs); }
        };
        Collections.sort(pending, byDate);
        Collections.sort(done, Collections.reverseOrder(byDate));
        List<Match> out = new ArrayList<>();
        for (int i = 0; i < pending.size() && i < KEEP_UPCOMING; i++) out.add(pending.get(i));
        for (int i = 0; i < done.size() && i < KEEP_RESULTS; i++) out.add(done.get(i));
        return out;
    }
}
