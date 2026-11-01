package com.zaid.madridlive;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Test helpers: the saved ESPN responses and quick match builders. */
final class Fx {
    private Fx() {}

    static String read(String name) throws Exception {
        try (InputStream in = Fx.class.getResourceAsStream("/fixtures/" + name)) {
            if (in == null) throw new IllegalStateException("missing fixture " + name);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    static long ms(String iso) {
        return Instant.parse(iso).toEpochMilli();
    }

    static Match match(String id, String state, String kickoffIso) {
        Match m = new Match();
        m.id = id;
        m.league = "esp.1";
        m.competition = "LaLiga";
        m.state = state;
        m.kickoffMs = ms(kickoffIso);
        m.home = "Real Madrid";
        m.away = "Barcelona";
        m.homeId = "86";
        m.awayId = "83";
        m.madridHome = true;
        m.homeScore = Match.PRE.equals(state) ? -1 : 0;
        m.awayScore = m.homeScore;
        return m;
    }

    static Detail goal(String teamId, String player, String clock) {
        Detail d = new Detail();
        d.kind = Detail.GOAL;
        d.teamId = teamId;
        d.player = player;
        d.clock = clock;
        return d;
    }

    static Detail red(String teamId, String player, String clock) {
        Detail d = new Detail();
        d.kind = Detail.RED;
        d.teamId = teamId;
        d.player = player;
        d.clock = clock;
        return d;
    }

    static Snapshot snap(String state, int hs, int as, String clock, Detail... details) {
        Match m = match("1", state, "2026-10-25T20:00:00Z");
        m.homeScore = hs;
        m.awayScore = as;
        m.clock = clock;
        Snapshot s = new Snapshot(m);
        for (Detail d : details) s.details.add(d);
        return s;
    }
}
