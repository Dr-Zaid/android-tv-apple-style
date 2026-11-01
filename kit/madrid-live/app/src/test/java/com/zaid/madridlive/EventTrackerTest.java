package com.zaid.madridlive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class EventTrackerTest {
    private static final String RM = "86";
    private static final String FCB = "83";

    @Test
    public void firstSnapshotIsOnlyABaseline() {
        EventTracker t = new EventTracker();
        List<Alert> a = t.update(Fx.snap(Match.IN, 2, 1, "70'", Fx.goal(RM, "A", "10'"), Fx.goal(RM, "B", "20'"), Fx.goal(FCB, "C", "30'")));
        assertTrue(a.isEmpty());
    }

    @Test
    public void aNewGoalIsAnnouncedOnceNotTwice() {
        EventTracker t = new EventTracker();
        t.update(Fx.snap(Match.IN, 0, 0, "60'"));
        Detail g = Fx.goal(RM, "Vinícius Jr.", "67'");

        List<Alert> first = t.update(Fx.snap(Match.IN, 1, 0, "67'", g));
        assertEquals(1, first.size());
        Alert a = first.get(0);
        assertEquals(Alert.GOAL, a.kind);
        assertEquals("Vinícius Jr.", a.player);
        assertEquals("Real Madrid", a.team);
        assertEquals(1, a.homeScore);
        assertEquals("67'", a.clock);
        assertEquals("Goal · Vinícius Jr. 67'", a.lastEvent());

        // the same state read again 30 s later, and again with a later clock
        assertTrue(t.update(Fx.snap(Match.IN, 1, 0, "67'", g)).isEmpty());
        assertTrue(t.update(Fx.snap(Match.IN, 1, 0, "68'", g)).isEmpty());
    }

    @Test
    public void notTwiceEvenAcrossAServiceRestart() {
        EventTracker t = new EventTracker();
        t.update(Fx.snap(Match.IN, 0, 0, "60'"));
        Detail g = Fx.goal(RM, "X", "61'");
        assertEquals(1, t.update(Fx.snap(Match.IN, 1, 0, "61'", g)).size());

        EventTracker restored = EventTracker.fromJson(t.toJson());
        assertTrue(restored.update(Fx.snap(Match.IN, 1, 0, "62'", g)).isEmpty());
        // and a restart in the middle of a match with no saved state raises nothing for the goals already scored
        assertTrue(new EventTracker().update(Fx.snap(Match.IN, 1, 0, "62'", g)).isEmpty());
        assertEquals(1, restored.update(Fx.snap(Match.IN, 1, 1, "70'", g, Fx.goal(FCB, "Y", "70'"))).size());
    }

    @Test
    public void bothTeamsGoalsAreAnnouncedAndNamed() {
        EventTracker t = new EventTracker();
        t.update(Fx.snap(Match.IN, 0, 0, "10'"));
        List<Alert> a = t.update(Fx.snap(Match.IN, 0, 1, "12'", Fx.goal(FCB, "Lamine", "12'")));
        assertEquals(1, a.size());
        assertEquals("Lamine", a.get(0).player);
        assertEquals("Barcelona", a.get(0).team);
        assertEquals(1, a.get(0).awayScore);
    }

    @Test
    public void aGoalBeforeItsDetailsArriveStillAlertsAndTheDetailIsNotRepeated() {
        EventTracker t = new EventTracker();
        t.update(Fx.snap(Match.IN, 0, 0, "10'"));
        List<Alert> a = t.update(Fx.snap(Match.IN, 1, 0, "11'"));          // score moved, no details yet
        assertEquals(1, a.size());
        assertEquals("", a.get(0).player);
        assertEquals("Real Madrid", a.get(0).subline());
        // the details show up later: the score did not move, so nothing new is announced
        assertTrue(t.update(Fx.snap(Match.IN, 1, 0, "12'", Fx.goal(RM, "Late", "11'"))).isEmpty());
    }

    @Test
    public void aGoalTakenBackByVarIsNotAnnouncedAndTheNextOneIs() {
        EventTracker t = new EventTracker();
        t.update(Fx.snap(Match.IN, 1, 0, "30'", Fx.goal(RM, "A", "30'")));
        assertTrue(t.update(Fx.snap(Match.IN, 0, 0, "33'")).isEmpty());          // disallowed
        assertEquals(1, t.update(Fx.snap(Match.IN, 1, 0, "50'", Fx.goal(RM, "B", "50'"))).size());
    }

    @Test
    public void redCardOnceByPlayer() {
        EventTracker t = new EventTracker();
        t.update(Fx.snap(Match.IN, 0, 0, "70'"));
        Detail r = Fx.red(FCB, "Araujo", "75'");
        List<Alert> a = t.update(Fx.snap(Match.IN, 0, 0, "75'", r));
        assertEquals(1, a.size());
        assertEquals(Alert.RED, a.get(0).kind);
        assertEquals("Araujo", a.get(0).player);
        assertEquals("Barcelona", a.get(0).team);
        assertEquals("Red card · Araujo 75'", a.get(0).lastEvent());
        assertTrue(t.update(Fx.snap(Match.IN, 0, 0, "76'", r)).isEmpty());
        // the other feed spells the clock differently: still the same card
        assertTrue(t.update(Fx.snap(Match.IN, 0, 0, "77'", Fx.red(FCB, "Araujo", "75:12"))).isEmpty());
    }

    @Test
    public void kickoffThenFullTimeEachOnce() {
        EventTracker t = new EventTracker();
        assertTrue(t.update(Fx.snap(Match.PRE, -1, -1, "")).isEmpty());
        assertTrue(t.update(Fx.snap(Match.PRE, -1, -1, "")).isEmpty());
        List<Alert> ko = t.update(Fx.snap(Match.IN, 0, 0, "1'"));
        assertEquals(1, ko.size());
        assertEquals(Alert.KICKOFF, ko.get(0).kind);
        assertTrue(t.update(Fx.snap(Match.IN, 0, 0, "2'")).isEmpty());

        List<Alert> ft = t.update(Fx.snap(Match.POST, 2, 1, "FT", Fx.goal(RM, "A", "1'"), Fx.goal(RM, "B", "2'"), Fx.goal(FCB, "C", "3'")));
        // two home goals and one away goal were scored since the last poll, then the whistle
        assertEquals(4, ft.size());
        assertEquals(Alert.FULLTIME, ft.get(3).kind);
        assertTrue(t.update(Fx.snap(Match.POST, 2, 1, "FT")).isEmpty());
    }

    @Test
    public void joiningAfterTheWhistleSaysNothing() {
        EventTracker t = new EventTracker();
        assertTrue(t.update(Fx.snap(Match.POST, 3, 0, "FT")).isEmpty());
        assertTrue(t.update(Fx.snap(Match.POST, 3, 0, "FT")).isEmpty());
    }

    @Test
    public void aDifferentMatchStartsFromANewBaseline() {
        EventTracker t = new EventTracker();
        t.update(Fx.snap(Match.IN, 3, 0, "80'"));
        Snapshot other = Fx.snap(Match.IN, 5, 5, "10'");
        other.match.id = "2";
        assertTrue(t.update(other).isEmpty());
    }

    @Test
    public void realFixturesEndToEnd() throws Exception {
        Snapshot live = EspnParser.parseSummary(Fx.read("summary_live_synthetic.json"), "esp.1", 1);
        Snapshot post = EspnParser.parseSummary(Fx.read("summary_post.json"), "esp.1", 2);
        assertNotNull(live);
        assertNotNull(post);

        EventTracker t = new EventTracker();
        assertTrue(t.update(live).isEmpty());        // 3-0 at 67', baseline

        List<Alert> a = t.update(post);              // 4-1 and the whistle
        assertEquals(3, a.size());
        assertEquals(Alert.GOAL, a.get(0).kind);
        assertEquals("Yan Diomande", a.get(0).player);
        assertEquals("82'", a.get(0).clock);
        assertEquals(Alert.GOAL, a.get(1).kind);
        assertEquals("Manu Bueno", a.get(1).player);
        assertEquals("Sevilla", a.get(1).team);
        assertEquals(Alert.FULLTIME, a.get(2).kind);

        assertTrue(t.update(post).isEmpty());
        assertFalse(EventTracker.fromJson(t.toJson()).update(post).iterator().hasNext());
    }

    @Test
    public void garbageStateTextGivesAFreshTracker() {
        assertTrue(EventTracker.fromJson("{{{").update(Fx.snap(Match.IN, 4, 4, "5'")).isEmpty());
        assertTrue(EventTracker.fromJson(null).update(Fx.snap(Match.IN, 4, 4, "5'")).isEmpty());
    }
}
