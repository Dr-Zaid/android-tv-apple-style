package com.zaid.madridlive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatchSelectorTest {
    private static final long NOW = Fx.ms("2026-10-25T21:00:00Z");

    private static Match m(String id, String state, String iso) {
        return Fx.match(id, state, iso);
    }

    @Test
    public void liveBeatsEverything() {
        Match live = m("live", Match.IN, "2026-10-25T20:00:00Z");
        Match result = m("res", Match.POST, "2026-10-25T17:00:00Z");
        Match next = m("next", Match.PRE, "2026-10-28T20:00:00Z");
        assertSame(live, MatchSelector.select(Arrays.asList(next, result, live), NOW));
    }

    @Test
    public void resultUnderThreeHoursOldBeatsTheNextMatch() {
        // kicked off 19:00, assumed over 21:00 -> 0h old
        Match result = m("res", Match.POST, "2026-10-25T19:00:00Z");
        Match next = m("next", Match.PRE, "2026-10-28T20:00:00Z");
        assertSame(result, MatchSelector.select(Arrays.asList(next, result), NOW));
    }

    @Test
    public void resultStaysThreeHoursThenTheNextMatchTakesOver() {
        Match result = m("res", Match.POST, "2026-10-25T19:00:00Z");   // assumed end 21:00
        Match next = m("next", Match.PRE, "2026-10-28T20:00:00Z");
        List<Match> all = Arrays.asList(result, next);
        assertSame(result, MatchSelector.select(all, Fx.ms("2026-10-26T00:00:00Z") - 1));   // 2h59m59s after the end
        assertSame(next, MatchSelector.select(all, Fx.ms("2026-10-26T00:00:00Z")));          // exactly 3h
    }

    @Test
    public void observedEndTimeWinsOverTheEstimate() {
        Match result = m("res", Match.POST, "2026-10-25T10:00:00Z");   // estimate says over at 12:00
        result.endedMs = Fx.ms("2026-10-25T20:30:00Z");               // but we saw the whistle at 20:30
        Match next = m("next", Match.PRE, "2026-10-28T20:00:00Z");
        assertSame(result, MatchSelector.select(Arrays.asList(result, next), NOW));
    }

    @Test
    public void nextIsTheEarliestUpcomingAcrossCompetitions() {
        Match later = m("later", Match.PRE, "2026-11-04T17:45:00Z");
        Match sooner = m("sooner", Match.PRE, "2026-10-29T20:00:00Z");
        Match old = m("old", Match.POST, "2026-10-01T20:00:00Z");
        assertSame(sooner, MatchSelector.select(Arrays.asList(later, old, sooner), NOW));
    }

    @Test
    public void nothingToShowGivesNull() {
        assertNull(MatchSelector.select(new ArrayList<Match>(), NOW));
        assertNull(MatchSelector.select(null, NOW));
        assertNull(MatchSelector.select(Arrays.asList(m("old", Match.POST, "2026-10-01T20:00:00Z")), NOW));
    }

    @Test
    public void staleLiveAndOverduePreAreIgnored() {
        Match staleLive = m("stale", Match.IN, "2026-10-25T10:00:00Z");   // "live" for 11 hours: the feed died
        Match overdue = m("overdue", Match.PRE, "2026-10-25T10:00:00Z"); // never started: postponed
        Match next = m("next", Match.PRE, "2026-10-28T20:00:00Z");
        assertSame(next, MatchSelector.select(Arrays.asList(staleLive, overdue, next), NOW));
    }

    @Test
    public void aMatchThatShouldHaveStartedButIsStillPreIsStillShown() {
        Match justStarted = m("now", Match.PRE, "2026-10-25T20:50:00Z");
        assertSame(justStarted, MatchSelector.select(Arrays.asList(justStarted), NOW));
    }

    @Test
    public void mergeKeepsWhatOnlyWeKnowAndNeverGoesBackwards() {
        Match old = m("a", Match.IN, "2026-10-25T20:00:00Z");
        old.lastEvent = "Goal · X 10'";
        old.lastEventMs = 5;
        old.homeScore = 1;
        Match stale = m("a", Match.PRE, "2026-10-25T20:00:00Z");    // the schedule feed lags behind
        Match other = m("b", Match.PRE, "2026-11-01T20:00:00Z");
        List<Match> merged = MatchSelector.merge(Arrays.asList(old), Arrays.asList(stale, other));
        assertEquals(2, merged.size());
        assertEquals(Match.IN, merged.get(0).state);
        assertEquals(1, merged.get(0).homeScore);
        assertEquals(Match.PRE, merged.get(1).state);

        Match done = m("a", Match.POST, "2026-10-25T20:00:00Z");
        done.homeScore = 2;
        merged = MatchSelector.merge(Arrays.asList(old), Arrays.asList(done));
        assertEquals(Match.POST, merged.get(0).state);
        assertEquals(2, merged.get(0).homeScore);
        assertEquals("Goal · X 10'", merged.get(0).lastEvent);
        assertEquals(5, merged.get(0).lastEventMs);
    }

    @Test
    public void mergeKeepsTheObservedEndTime() {
        Match old = m("a", Match.POST, "2026-10-25T20:00:00Z");
        old.endedMs = 123;
        Match fresh = m("a", Match.POST, "2026-10-25T20:00:00Z");
        assertEquals(123, MatchSelector.merge(Arrays.asList(old), Arrays.asList(fresh)).get(0).endedMs);
    }

    @Test
    public void refresherTrimKeepsTheNextFourAndLastThree() {
        List<Match> up = new ArrayList<>();
        for (int i = 1; i <= 6; i++) up.add(m("u" + i, Match.PRE, "2026-11-0" + i + "T20:00:00Z"));
        List<Match> res = new ArrayList<>();
        for (int i = 1; i <= 5; i++) res.add(m("r" + i, Match.POST, "2026-10-0" + i + "T20:00:00Z"));
        res.add(up.get(0));   // the same match listed in both feeds counts once
        List<Match> kept = Refresher.trim(up, res);
        assertEquals(7, kept.size());
        assertTrue(kept.get(0).id.equals("u1") && kept.get(3).id.equals("u4"));
        assertEquals("r5", kept.get(4).id);
        assertEquals("r3", kept.get(6).id);
    }
}
