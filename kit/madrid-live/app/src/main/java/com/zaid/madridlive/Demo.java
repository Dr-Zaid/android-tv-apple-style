package com.zaid.madridlive;

import android.content.Context;
import android.graphics.Bitmap;

/** The test hook: fake banners and a fake live row, so the whole chain can be tried without a match on. */
public final class Demo {
    private Demo() {}

    public static final long LIVE_MS = 2 * Timing.MINUTE;

    /** A made-up live match: Real Madrid 1-0 Barcelona, 67th minute, at the Bernabéu. */
    public static Match liveMatch(long now) {
        Match m = new Match();
        m.id = "demo";
        m.league = "esp.1";
        m.competition = Espn.competitionName("esp.1");
        m.state = Match.IN;
        m.kickoffMs = now - 67 * Timing.MINUTE;
        m.home = "Real Madrid";
        m.away = "Barcelona";
        m.homeAbbr = "RMA";
        m.awayAbbr = "BAR";
        m.homeId = Match.MADRID_ID;
        m.awayId = "83";
        m.homeLogo = Espn.crestUrl(m.homeId);
        m.awayLogo = Espn.crestUrl(m.awayId);
        m.homeScore = 1;
        m.awayScore = 0;
        m.clock = "67'";
        m.madridHome = true;
        m.venue = "Santiago Bernabéu";
        m.fetchedMs = now;
        m.lastEvent = "Goal · Vinícius Jr. 67'";
        m.lastEventMs = now;
        return m;
    }

    /** The banner for a DEMO kind, or null for an unknown kind (and for "live", which has no banner). */
    public static Alert alert(String kind, long now) {
        if (kind == null) return null;
        Match m = liveMatch(now);
        Alert a;
        switch (kind) {
            case "goal":
                a = Alert.of(Alert.GOAL, m);
                a.player = "Vinícius Jr.";
                a.team = m.home;
                return a;
            case "red":
                m.homeScore = 1;
                m.awayScore = 1;
                m.clock = "80'";
                a = Alert.of(Alert.RED, m);
                a.player = "Ronald Araújo";
                a.team = m.away;
                return a;
            case "kickoff":
                m.homeScore = 0;
                m.awayScore = 0;
                m.clock = "0'";
                return Alert.of(Alert.KICKOFF, m);
            case "fulltime":
                m.homeScore = 2;
                m.awayScore = 1;
                m.clock = "FT";
                return Alert.of(Alert.FULLTIME, m);
            default:
                return null;
        }
    }

    /** Entry point for the broadcast: "live" fakes the row for two minutes; the rest show a banner. */
    public static void run(Context ctx, String kind) {
        if ("live".equals(kind)) {
            long now = System.currentTimeMillis();
            Store.setDemo(ctx, liveMatch(now), now + LIVE_MS);
            Scheduler.scheduleDemoEnd(ctx, now + LIVE_MS + 1_000);
            Updates.changed(ctx);
        } else if (alert(kind, System.currentTimeMillis()) != null) {
            LiveService.startDemo(ctx, kind);
        }
    }

    /** Show the banner right here (downloads the crests first, so call it off the main thread). */
    static void showDirect(Context ctx, String kind) {
        try {
            Alert a = alert(kind, System.currentTimeMillis());
            if (a == null) return;
            LogoCache.ensure(ctx, a.homeId, a.homeLogo);
            LogoCache.ensure(ctx, a.awayId, a.awayLogo);
            Bitmap h = LogoCache.bitmap(ctx, a.homeId);
            Bitmap w = LogoCache.bitmap(ctx, a.awayId);
            OverlayHost.get(ctx).show(a, h, w);
        } catch (Throwable ignored) {
            // a demo that cannot show is just a demo that does nothing
        }
    }
}
