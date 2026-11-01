package com.zaid.madridlive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class TimingTest {
    private static final long KICKOFF = Fx.ms("2026-10-25T20:00:00Z");

    private static Match pre(String id, String iso) {
        return Fx.match(id, Match.PRE, iso);
    }

    @Test
    public void alarmIsTenMinutesBeforeKickoff() {
        assertEquals(KICKOFF - 10 * 60_000L, Timing.kickoffAlarmAt(pre("a", "2026-10-25T20:00:00Z")));
        assertEquals(Fx.ms("2026-10-25T19:50:00Z"), Timing.kickoffAlarmAt(pre("a", "2026-10-25T20:00:00Z")));
    }

    @Test
    public void nextAlarmIsTheEarliestOneStillAhead() {
        Match tonight = pre("a", "2026-10-25T20:00:00Z");
        Match tuesday = pre("b", "2026-10-27T20:00:00Z");
        Match past = pre("c", "2026-10-20T20:00:00Z");
        long now = Fx.ms("2026-10-25T12:00:00Z");
        assertEquals(Fx.ms("2026-10-25T19:50:00Z"), Timing.nextKickoffAlarm(Arrays.asList(tuesday, past, tonight), now));
        // once tonight's alarm time has passed, the next alarm is Tuesday's
        assertEquals(Fx.ms("2026-10-27T19:50:00Z"),
                Timing.nextKickoffAlarm(Arrays.asList(tuesday, tonight), Fx.ms("2026-10-25T19:55:00Z")));
        assertEquals(0, Timing.nextKickoffAlarm(Arrays.asList(past), now));
        assertEquals(0, Timing.nextKickoffAlarm(null, now));
        // a live or finished match never gets an alarm
        assertEquals(0, Timing.nextKickoffAlarm(Arrays.asList(Fx.match("l", Match.IN, "2026-10-26T20:00:00Z")), now));
    }

    @Test
    public void pollingWindowStartsTenMinutesBeforeKickoff() {
        Match m = pre("a", "2026-10-25T20:00:00Z");
        assertFalse(Timing.shouldPoll(m, Fx.ms("2026-10-25T19:49:59Z")));
        assertTrue(Timing.shouldPoll(m, Fx.ms("2026-10-25T19:50:00Z")));
        assertTrue(Timing.shouldPoll(m, Fx.ms("2026-10-25T20:30:00Z")));
        assertFalse(Timing.shouldPoll(null, KICKOFF));
    }

    @Test
    public void pollingStopsTenMinutesAfterTheWhistle() {
        Match m = Fx.match("a", Match.POST, "2026-10-25T20:00:00Z");
        m.endedMs = Fx.ms("2026-10-25T21:51:00Z");
        assertTrue(Timing.shouldPoll(m, Fx.ms("2026-10-25T22:00:59Z")));
        assertFalse(Timing.shouldPoll(m, Fx.ms("2026-10-25T22:01:00Z")));
        // whistle never seen: estimated at kickoff + 2h, so the window closes at 22:10
        Match unseen = Fx.match("b", Match.POST, "2026-10-25T20:00:00Z");
        assertTrue(Timing.shouldPoll(unseen, Fx.ms("2026-10-25T22:09:00Z")));
        assertFalse(Timing.shouldPoll(unseen, Fx.ms("2026-10-25T22:10:00Z")));
    }

    @Test
    public void aMatchThatNeverStartsOrNeverEndsIsGivenUpOn() {
        assertFalse(Timing.shouldPoll(pre("a", "2026-10-25T20:00:00Z"), Fx.ms("2026-10-26T00:01:00Z")));   // pre, 4h overdue
        assertFalse(Timing.shouldPoll(Fx.match("b", Match.IN, "2026-10-25T20:00:00Z"), Fx.ms("2026-10-26T01:01:00Z")));   // in, 5h+
    }

    @Test
    public void staleMeansOlderThanSixHours() {
        long now = Fx.ms("2026-10-25T12:00:00Z");
        assertFalse(Timing.isStale(now - 5 * Timing.HOUR, now));
        assertFalse(Timing.isStale(now - 6 * Timing.HOUR + 1, now));
        assertTrue(Timing.isStale(now - 6 * Timing.HOUR, now));
        assertTrue(Timing.isStale(0, now));
        assertTrue(Timing.isStale(now + 1, now));   // clock went backwards
    }

    @Test
    public void refreshBackoffDoublesFromOneMinuteToThirty() {
        assertEquals(1 * Timing.MINUTE, Timing.refreshBackoffMs(1));
        assertEquals(2 * Timing.MINUTE, Timing.refreshBackoffMs(2));
        assertEquals(4 * Timing.MINUTE, Timing.refreshBackoffMs(3));
        assertEquals(16 * Timing.MINUTE, Timing.refreshBackoffMs(5));
        assertEquals(30 * Timing.MINUTE, Timing.refreshBackoffMs(6));
        assertEquals(30 * Timing.MINUTE, Timing.refreshBackoffMs(500));
        assertEquals(1 * Timing.MINUTE, Timing.refreshBackoffMs(0));
    }

    @Test
    public void pollDelayIsThirtySecondsUntilSomethingFails() {
        assertEquals(30_000, Timing.pollDelayMs(0));
        assertEquals(60_000, Timing.pollDelayMs(1));
        assertEquals(120_000, Timing.pollDelayMs(2));
        assertEquals(240_000, Timing.pollDelayMs(3));
        assertEquals(300_000, Timing.pollDelayMs(4));
        assertEquals(300_000, Timing.pollDelayMs(99));
    }

    @Test
    public void aTvWithoutNetworkTimeIsNotTrusted() {
        assertFalse(Timing.isClockSane(0));
        assertFalse(Timing.isClockSane(Fx.ms("2025-06-01T00:00:00Z")));
        assertTrue(Timing.isClockSane(Fx.ms("2026-10-24T20:00:00Z")));
    }
}
