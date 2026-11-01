package com.zaid.madridlive;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * One fixture as the provider row needs it. A plain value holder (public fields, {@link #copy()} to change one) so the
 * pure logic around it (parser, selector, tracker) stays easy to test on the JVM.
 */
public final class Match {
    public static final String PRE = "pre";
    public static final String IN = "in";
    public static final String POST = "post";

    /** ESPN's team id for Real Madrid. */
    public static final String MADRID_ID = "86";

    public String id = "";
    /** ESPN league slug, e.g. {@code esp.1}; the live poll needs it to build its URL. */
    public String league = "";
    /** Display name: "LaLiga", "Champions League", ... */
    public String competition = "";
    public String state = PRE;
    public long kickoffMs;
    public String home = "";
    public String away = "";
    public String homeAbbr = "";
    public String awayAbbr = "";
    public String homeId = "";
    public String awayId = "";
    public String homeLogo = "";
    public String awayLogo = "";
    /** -1 before kick-off. */
    public int homeScore = -1;
    public int awayScore = -1;
    /** "67'", "HT", "FT"; empty before kick-off. */
    public String clock = "";
    public boolean madridHome;
    public String venue = "";
    public long fetchedMs;
    public String lastEvent = "";
    public long lastEventMs;
    /** When we saw the match turn {@code post}; 0 if never (the selector then estimates it). */
    public long endedMs;

    public Match copy() {
        Match m = new Match();
        m.id = id;
        m.league = league;
        m.competition = competition;
        m.state = state;
        m.kickoffMs = kickoffMs;
        m.home = home;
        m.away = away;
        m.homeAbbr = homeAbbr;
        m.awayAbbr = awayAbbr;
        m.homeId = homeId;
        m.awayId = awayId;
        m.homeLogo = homeLogo;
        m.awayLogo = awayLogo;
        m.homeScore = homeScore;
        m.awayScore = awayScore;
        m.clock = clock;
        m.madridHome = madridHome;
        m.venue = venue;
        m.fetchedMs = fetchedMs;
        m.lastEvent = lastEvent;
        m.lastEventMs = lastEventMs;
        m.endedMs = endedMs;
        return m;
    }

    /** 0 = pre, 1 = in, 2 = post: a match only moves forward. */
    public int stateRank() {
        if (IN.equals(state)) return 1;
        if (POST.equals(state)) return 2;
        return 0;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("league", league);
            o.put("competition", competition);
            o.put("state", state);
            o.put("kickoffMs", kickoffMs);
            o.put("home", home);
            o.put("away", away);
            o.put("homeAbbr", homeAbbr);
            o.put("awayAbbr", awayAbbr);
            o.put("homeId", homeId);
            o.put("awayId", awayId);
            o.put("homeLogo", homeLogo);
            o.put("awayLogo", awayLogo);
            o.put("homeScore", homeScore);
            o.put("awayScore", awayScore);
            o.put("clock", clock);
            o.put("madridHome", madridHome);
            o.put("venue", venue);
            o.put("fetchedMs", fetchedMs);
            o.put("lastEvent", lastEvent);
            o.put("lastEventMs", lastEventMs);
            o.put("endedMs", endedMs);
        } catch (JSONException ignored) {
            // keys are constants and values are plain: cannot happen
        }
        return o;
    }

    /** Null when the object is unusable (no id or no kick-off): a bad stored row degrades to "no row". */
    public static Match fromJson(JSONObject o) {
        if (o == null) return null;
        Match m = new Match();
        m.id = o.optString("id", "");
        m.league = o.optString("league", "");
        m.competition = o.optString("competition", "");
        m.state = o.optString("state", PRE);
        m.kickoffMs = o.optLong("kickoffMs", 0);
        m.home = o.optString("home", "");
        m.away = o.optString("away", "");
        m.homeAbbr = o.optString("homeAbbr", "");
        m.awayAbbr = o.optString("awayAbbr", "");
        m.homeId = o.optString("homeId", "");
        m.awayId = o.optString("awayId", "");
        m.homeLogo = o.optString("homeLogo", "");
        m.awayLogo = o.optString("awayLogo", "");
        m.homeScore = o.optInt("homeScore", -1);
        m.awayScore = o.optInt("awayScore", -1);
        m.clock = o.optString("clock", "");
        m.madridHome = o.optBoolean("madridHome", false);
        m.venue = o.optString("venue", "");
        m.fetchedMs = o.optLong("fetchedMs", 0);
        m.lastEvent = o.optString("lastEvent", "");
        m.lastEventMs = o.optLong("lastEventMs", 0);
        m.endedMs = o.optLong("endedMs", 0);
        if (m.id.isEmpty() || m.kickoffMs <= 0) return null;
        return m;
    }
}
