package com.zaid.madridlive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.List;

/** The provider contract: exact columns, in order, with the right types. */
public class MatchRowTest {
    @Test
    public void columnsAreExactlyTheContract() {
        assertArrayEquals(new String[]{
                "state", "kickoff_ms", "competition", "home", "away", "home_abbr", "away_abbr",
                "home_score", "away_score", "clock", "madrid_home", "venue", "fetched_ms", "last_event", "last_event_ms",
        }, MatchRow.COLUMNS);
    }

    @Test
    public void preMatchRowHasMinusOneScoresAndEmptyClock() throws Exception {
        List<Match> list = EspnParser.parseSchedule(Fx.read("schedule_laliga_fixtures.json"), "esp.1", 777);
        Object[] v = MatchRow.values(list.get(0));
        assertEquals(MatchRow.COLUMNS.length, v.length);
        assertEquals("pre", v[0]);
        assertEquals(Fx.ms("2026-10-25T20:00:00Z"), v[1]);
        assertEquals("LaLiga", v[2]);
        assertEquals("Barcelona", v[3]);
        assertEquals("Real Madrid", v[4]);
        assertEquals("BAR", v[5]);
        assertEquals("RMA", v[6]);
        assertEquals(-1, v[7]);
        assertEquals(-1, v[8]);
        assertEquals("", v[9]);
        assertEquals(0, v[10]);
        assertEquals("Spotify Camp Nou", v[11]);
        assertEquals(777L, v[12]);
        assertEquals("", v[13]);
        assertEquals(0L, v[14]);
    }

    @Test
    public void madridHomeIsOneOrZero() throws Exception {
        List<Match> list = EspnParser.parseSchedule(Fx.read("schedule_laliga_results.json"), "esp.1", 1);
        assertEquals(1, MatchRow.values(list.get(0))[10]);
    }

    @Test
    public void projectionKeepsKnownColumnsOnly() {
        assertArrayEquals(new String[]{"home", "clock"}, MatchRow.pick(new String[]{"home", "bogus", "clock"}));
        assertArrayEquals(MatchRow.COLUMNS, MatchRow.pick(null));
        assertArrayEquals(MatchRow.COLUMNS, MatchRow.pick(new String[]{"bogus"}));
    }

    @Test
    public void matchSurvivesItsJson() throws Exception {
        Match m = EspnParser.parseSchedule(Fx.read("schedule_laliga_results.json"), "esp.1", 5).get(0);
        m.lastEvent = "Goal · X 1'";
        m.lastEventMs = 9;
        m.endedMs = 11;
        Match back = Match.fromJson(m.toJson());
        assertNotNull(back);
        assertEquals(m.toJson().toString(), back.toJson().toString());
        assertNull(Match.fromJson(new org.json.JSONObject()));
        assertNull(Match.fromJson(null));
    }

    @Test
    public void crestScalingNeverExceedsTheLimitButKeepsDetail() {
        assertEquals(2, LogoCache.sampleSize(500, 500, 160));    // 500/2=250 >= 160, 500/4=125 < 160
        assertEquals(1, LogoCache.sampleSize(200, 200, 160));
        assertEquals(1, LogoCache.sampleSize(100, 100, 160));
        assertEquals(4, LogoCache.sampleSize(1000, 600, 160));
    }
}
