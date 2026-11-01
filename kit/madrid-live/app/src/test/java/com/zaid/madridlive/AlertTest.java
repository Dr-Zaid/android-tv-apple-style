package com.zaid.madridlive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AlertTest {
    private static Alert goal() {
        Match m = Fx.match("1", Match.IN, "2026-10-25T20:00:00Z");
        m.homeScore = 1;
        m.awayScore = 0;
        m.clock = "67'";
        Alert a = Alert.of(Alert.GOAL, m);
        a.player = "Vinícius Jr.";
        a.team = "Real Madrid";
        return a;
    }

    @Test
    public void goalHeadlineMatchesTheSpecExample() {
        // "هدف!  Real Madrid 1–0 Barcelona · 67'" (an invisible right-to-left mark sits after the Arabic word)
        assertEquals("هدف!  \u2066Real Madrid 1–0 Barcelona · 67'\u2069", goal().headline());
        assertEquals("Vinícius Jr. · Real Madrid", goal().subline());
    }

    @Test
    public void theFourArabicWords() {
        assertEquals("هدف!", Alert.kindWord(Alert.GOAL));
        assertEquals("بطاقة حمراء", Alert.kindWord(Alert.RED));
        assertEquals("بدأت المباراة", Alert.kindWord(Alert.KICKOFF));
        assertEquals("نهاية المباراة", Alert.kindWord(Alert.FULLTIME));
    }

    @Test
    public void kickoffAndFullTimeLines() {
        Match m = Fx.match("1", Match.IN, "2026-10-25T20:00:00Z");
        m.venue = "Spotify Camp Nou";
        Alert ko = Alert.of(Alert.KICKOFF, m);
        assertTrue(ko.headline().endsWith("Real Madrid vs Barcelona\u2069"));
        assertEquals("LaLiga · Spotify Camp Nou", ko.subline());
        assertEquals("Kick-off", ko.lastEvent());
        m.homeScore = 2;
        m.awayScore = 1;
        m.clock = "FT";
        Alert ft = Alert.of(Alert.FULLTIME, m);
        assertTrue(ft.headline().endsWith("Real Madrid 2–1 Barcelona · FT\u2069"));
        assertEquals("Full time", ft.lastEvent());
    }

    @Test
    public void penaltyAndOwnGoalAreMarked() {
        Alert a = goal();
        a.penalty = true;
        assertEquals("Vinícius Jr. (pen.) · Real Madrid", a.subline());
        a.penalty = false;
        a.ownGoal = true;
        assertEquals("Vinícius Jr. (o.g.) · Real Madrid", a.subline());
    }

    @Test
    public void demoAlertsExistForTheFourKinds() {
        long now = 1_800_000_000_000L;
        assertEquals(Alert.GOAL, Demo.alert("goal", now).kind);
        assertEquals(Alert.RED, Demo.alert("red", now).kind);
        assertEquals(Alert.KICKOFF, Demo.alert("kickoff", now).kind);
        assertEquals(Alert.FULLTIME, Demo.alert("fulltime", now).kind);
        assertEquals(null, Demo.alert("live", now));
        assertEquals(null, Demo.alert("nonsense", now));
        assertEquals(null, Demo.alert(null, now));
    }

    @Test
    public void demoLiveRowIsOneNilLeadAtSixtySeven() {
        long now = 1_800_000_000_000L;
        Match m = Demo.liveMatch(now);
        assertEquals(Match.IN, m.state);
        assertEquals(1, m.homeScore);
        assertEquals(0, m.awayScore);
        assertEquals("67'", m.clock);
        assertEquals("Goal · Vinícius Jr. 67'", m.lastEvent);
        assertEquals(now, m.lastEventMs);
    }
}
