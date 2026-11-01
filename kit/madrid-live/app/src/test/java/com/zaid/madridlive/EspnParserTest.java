package com.zaid.madridlive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/** The parser against real saved ESPN responses (src/test/resources/fixtures). */
public class EspnParserTest {
    private static final long NOW = 1_000L;

    @Test
    public void upcomingFixtureIsPreWithNoScore() throws Exception {
        List<Match> list = EspnParser.parseSchedule(Fx.read("schedule_laliga_fixtures.json"), "esp.1", NOW);
        assertEquals(3, list.size());
        Match m = list.get(0);
        assertEquals("401882832", m.id);
        assertEquals(Match.PRE, m.state);
        assertEquals(Fx.ms("2026-10-25T20:00:00Z"), m.kickoffMs);
        assertEquals("Barcelona", m.home);
        assertEquals("Real Madrid", m.away);
        assertEquals("BAR", m.homeAbbr);
        assertEquals("RMA", m.awayAbbr);
        assertFalse(m.madridHome);   // away at Barcelona
        assertEquals(-1, m.homeScore);
        assertEquals(-1, m.awayScore);
        assertEquals("", m.clock);
        assertEquals("Spotify Camp Nou", m.venue);
        assertEquals("LaLiga", m.competition);
        assertEquals("esp.1", m.league);
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/83.png", m.homeLogo);
        assertEquals("86", m.awayId);
        assertEquals(NOW, m.fetchedMs);
    }

    @Test
    public void championsLeagueFixtureGetsItsOwnName() throws Exception {
        List<Match> list = EspnParser.parseSchedule(Fx.read("schedule_ucl_fixtures.json"), "uefa.champions", NOW);
        assertEquals(2, list.size());
        assertEquals("Champions League", list.get(0).competition);
        assertEquals("AEK", list.get(0).homeAbbr);
    }

    @Test
    public void resultHasScoreFromTheScoreObjectAndFullTimeClock() throws Exception {
        List<Match> list = EspnParser.parseSchedule(Fx.read("schedule_laliga_results.json"), "esp.1", NOW);
        Match m = list.get(0);
        assertEquals("401882839", m.id);
        assertEquals(Match.POST, m.state);
        assertEquals(4, m.homeScore);
        assertEquals(1, m.awayScore);
        assertEquals("FT", m.clock);
        assertTrue(m.madridHome);
        assertEquals("Real Madrid", m.home);
    }

    @Test
    public void emptyOrBrokenFeedsGiveNothing() throws Exception {
        assertTrue(EspnParser.parseSchedule(Fx.read("schedule_copa_empty.json"), "esp.copa_del_rey", NOW).isEmpty());
        assertTrue(EspnParser.parseSchedule("", "esp.1", NOW).isEmpty());
        assertTrue(EspnParser.parseSchedule("<html>502</html>", "esp.1", NOW).isEmpty());
        assertTrue(EspnParser.parseSchedule("{\"events\":[{\"id\":\"1\"},{\"competitions\":[]},null,5]}", "esp.1", NOW).isEmpty());
        assertNull(EspnParser.parseScoreboard("not json", "1", "esp.1", NOW));
        assertNull(EspnParser.parseSummary("{}", "esp.1", NOW));
    }

    @Test
    public void aBadFieldDropsOnlyThatMatch() throws Exception {
        String good = Fx.read("schedule_laliga_fixtures.json");
        // break the second event's date (both places it appears)
        String broken = good.replace("2026-11-01T15:15Z", "yesterday");
        List<Match> list = EspnParser.parseSchedule(broken, "esp.1", NOW);
        assertEquals(2, list.size());
        assertEquals("401882832", list.get(0).id);
    }

    @Test
    public void dateWithOrWithoutSeconds() {
        assertEquals(Fx.ms("2026-10-25T20:00:00Z"), EspnParser.parseDate("2026-10-25T20:00Z"));
        assertEquals(Fx.ms("2026-10-25T20:00:30Z"), EspnParser.parseDate("2026-10-25T20:00:30Z"));
        assertEquals(0, EspnParser.parseDate("soon"));
        assertEquals(0, EspnParser.parseDate(null));
    }

    @Test
    public void realLiveScoreboardEntry() throws Exception {
        Snapshot s = EspnParser.parseScoreboard(Fx.read("scoreboard_live.json"), "401882827", "esp.1", NOW);
        assertNotNull(s);
        Match m = s.match;
        assertEquals(Match.IN, m.state);
        assertEquals("Valencia", m.home);
        assertEquals("Villarreal", m.away);
        assertEquals("62'", m.clock);
        assertEquals(1, m.homeScore);
        assertEquals(0, m.awayScore);
        assertEquals("Mestalla Stadium", m.venue);
        assertEquals("https://a.espncdn.com/i/teamlogos/soccer/500/94.png", m.homeLogo);   // team.logo, not logos[]
        assertFalse(m.madridHome);
        // yellow cards are not events we announce; the penalty is
        assertEquals(1, s.details.size());
        Detail d = s.details.get(0);
        assertEquals(Detail.GOAL, d.kind);
        assertEquals("Pepelu", d.player);
        assertEquals("38'", d.clock);
        assertEquals("94", d.teamId);
        assertTrue(d.penalty);
        assertNull(EspnParser.parseScoreboard(Fx.read("scoreboard_live.json"), "999", "esp.1", NOW));
    }

    @Test
    public void summaryOfAFinishedMatchHasAllGoals() throws Exception {
        Snapshot s = EspnParser.parseSummary(Fx.read("summary_post.json"), "esp.1", NOW);
        assertNotNull(s);
        Match m = s.match;
        assertEquals("401882839", m.id);
        assertEquals(Match.POST, m.state);
        assertEquals(4, m.homeScore);
        assertEquals(1, m.awayScore);
        assertEquals("FT", m.clock);
        assertEquals("Santiago Bernabéu", m.venue);
        assertTrue(m.madridHome);
        assertEquals(5, s.details.size());          // keyEvents repeats the same goals; they are not doubled
        assertEquals("Arda Güler", s.details.get(0).player);
        assertEquals("30'", s.details.get(0).clock);
        assertTrue(s.details.get(2).penalty);
        assertEquals("Manu Bueno", s.details.get(3).player);
        assertEquals("243", s.details.get(3).teamId);
    }

    @Test
    public void syntheticLiveSummary() throws Exception {
        Snapshot s = EspnParser.parseSummary(Fx.read("summary_live_synthetic.json"), "esp.1", NOW);
        assertNotNull(s);
        assertEquals(Match.IN, s.match.state);
        assertEquals("67'", s.match.clock);
        assertEquals(3, s.match.homeScore);
        assertEquals(0, s.match.awayScore);
        assertEquals(3, s.details.size());
    }
}
