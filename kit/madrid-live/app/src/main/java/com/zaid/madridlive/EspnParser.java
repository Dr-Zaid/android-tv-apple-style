package com.zaid.madridlive;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ESPN JSON to {@link Match} and {@link Detail}. Pure (no Android classes) and forgiving: a field that is missing or the
 * wrong type makes that one match null / that one detail skipped, never an exception.
 */
public final class EspnParser {
    private EspnParser() {}

    /** Every parsable event of a team schedule response, in the feed's order. Empty on garbage or an empty feed. */
    public static List<Match> parseSchedule(String json, String league, long fetchedMs) {
        List<Match> out = new ArrayList<>();
        try {
            JSONArray events = new JSONObject(json).optJSONArray("events");
            if (events == null) return out;
            for (int i = 0; i < events.length(); i++) {
                JSONObject ev = events.optJSONObject(i);
                Match m = parseEvent(ev, league, fetchedMs, null);
                if (m != null) out.add(m);
            }
        } catch (Exception ignored) {
            // not JSON at all: nothing to add
        }
        return out;
    }

    /** One event of a scoreboard response with its goals and cards, or null when the id is not listed. */
    public static Snapshot parseScoreboard(String json, String eventId, String league, long fetchedMs) {
        try {
            JSONArray events = new JSONObject(json).optJSONArray("events");
            if (events == null) return null;
            for (int i = 0; i < events.length(); i++) {
                JSONObject ev = events.optJSONObject(i);
                if (ev == null || !eventId.equals(ev.optString("id"))) continue;
                Match m = parseEvent(ev, league, fetchedMs, null);
                if (m == null) return null;
                Snapshot s = new Snapshot(m);
                addDetails(s, firstCompetition(ev), null);
                return s;
            }
        } catch (Exception ignored) {
            // fall through to null
        }
        return null;
    }

    /** A summary response ({@code header} + {@code keyEvents}), or null when it cannot be read. */
    public static Snapshot parseSummary(String json, String league, long fetchedMs) {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject header = root.optJSONObject("header");
            if (header == null) return null;
            JSONObject venue = null;
            JSONObject info = root.optJSONObject("gameInfo");
            if (info != null) venue = info.optJSONObject("venue");
            // The header is an event-shaped object (id + competitions), like a scoreboard entry.
            Match m = parseEvent(header, league, fetchedMs, venue);
            if (m == null) return null;
            Snapshot s = new Snapshot(m);
            addDetails(s, firstCompetition(header), root.optJSONArray("keyEvents"));
            return s;
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    private static JSONObject firstCompetition(JSONObject event) {
        JSONArray comps = event == null ? null : event.optJSONArray("competitions");
        return comps == null ? null : comps.optJSONObject(0);
    }

    /** An event (schedule, scoreboard or summary header) to a Match; null if anything essential is missing. */
    static Match parseEvent(JSONObject event, String league, long fetchedMs, JSONObject venueOverride) {
        try {
            JSONObject comp = firstCompetition(event);
            if (event == null || comp == null) return null;
            Match m = new Match();
            m.id = event.optString("id", comp.optString("id", ""));
            m.league = league == null ? "" : league;
            m.competition = Espn.competitionName(league);
            m.fetchedMs = fetchedMs;

            String date = comp.optString("date", event.optString("date", ""));
            m.kickoffMs = parseDate(date);

            JSONObject status = comp.optJSONObject("status");
            if (status == null) status = event.optJSONObject("status");
            if (status == null) return null;
            JSONObject type = status.optJSONObject("type");
            if (type == null) return null;
            String state = type.optString("state", "");
            if (!Match.PRE.equals(state) && !Match.IN.equals(state) && !Match.POST.equals(state)) return null;
            m.state = state;

            JSONArray competitors = comp.optJSONArray("competitors");
            if (competitors == null) return null;
            JSONObject home = null;
            JSONObject away = null;
            for (int i = 0; i < competitors.length(); i++) {
                JSONObject c = competitors.optJSONObject(i);
                if (c == null) continue;
                String side = c.optString("homeAway", "");
                if ("home".equals(side)) home = c;
                else if ("away".equals(side)) away = c;
            }
            if (home == null || away == null) return null;
            JSONObject homeTeam = home.optJSONObject("team");
            JSONObject awayTeam = away.optJSONObject("team");
            if (homeTeam == null || awayTeam == null) return null;

            m.home = homeTeam.optString("displayName", "");
            m.away = awayTeam.optString("displayName", "");
            m.homeAbbr = homeTeam.optString("abbreviation", "");
            m.awayAbbr = awayTeam.optString("abbreviation", "");
            m.homeId = homeTeam.optString("id", home.optString("id", ""));
            m.awayId = awayTeam.optString("id", away.optString("id", ""));
            m.homeLogo = logo(homeTeam);
            m.awayLogo = logo(awayTeam);
            m.madridHome = Match.MADRID_ID.equals(m.homeId);

            if (Match.PRE.equals(state)) {
                m.homeScore = -1;
                m.awayScore = -1;
            } else {
                m.homeScore = Math.max(0, score(home));
                m.awayScore = Math.max(0, score(away));
            }

            m.clock = clock(status, type, state);

            JSONObject venue = venueOverride != null ? venueOverride : comp.optJSONObject("venue");
            m.venue = venue == null ? "" : venue.optString("fullName", "");

            if (m.id.isEmpty() || m.kickoffMs <= 0 || m.home.isEmpty() || m.away.isEmpty()) return null;
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    /** "2026-10-25T20:00Z" (ESPN drops the seconds) or with seconds; epoch ms, or 0 if unreadable. */
    public static long parseDate(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        try {
            String s = iso.endsWith("Z") ? iso.substring(0, iso.length() - 1) : iso;
            return LocalDateTime.parse(s).toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }

    private static String logo(JSONObject team) {
        JSONArray logos = team.optJSONArray("logos");
        if (logos != null) {
            JSONObject first = logos.optJSONObject(0);
            if (first != null && !first.optString("href", "").isEmpty()) return first.optString("href");
        }
        return team.optString("logo", "");
    }

    /** The score is a string ("4") on the scoreboard and an object ({@code displayValue}) in a schedule. */
    private static int score(JSONObject competitor) {
        Object s = competitor.opt("score");
        try {
            if (s instanceof JSONObject) {
                JSONObject o = (JSONObject) s;
                String d = o.optString("displayValue", "");
                if (!d.isEmpty()) return Integer.parseInt(d.trim());
                return (int) o.optDouble("value", 0);
            }
            if (s instanceof Number) return ((Number) s).intValue();
            if (s instanceof String) return Integer.parseInt(((String) s).trim());
        } catch (Exception ignored) {
            // unreadable score: treated as 0 by the caller
        }
        return 0;
    }

    private static String clock(JSONObject status, JSONObject type, String state) {
        if (Match.PRE.equals(state)) return "";
        String c = type.optString("shortDetail", "");
        if (c.isEmpty()) c = status.optString("displayClock", "");
        return c;
    }

    private static void addDetails(Snapshot s, JSONObject comp, JSONArray keyEvents) {
        if (comp == null) return;
        Set<String> seen = new HashSet<>();
        JSONArray details = comp.optJSONArray("details");
        boolean haveGoals = false;
        if (details != null) {
            for (int i = 0; i < details.length(); i++) {
                Detail d = fromDetail(details.optJSONObject(i));
                if (d != null && seen.add(d.key())) {
                    s.details.add(d);
                    if (d.kind == Detail.GOAL) haveGoals = true;
                }
            }
        }
        if (keyEvents != null) {
            for (int i = 0; i < keyEvents.length(); i++) {
                Detail d = fromKeyEvent(keyEvents.optJSONObject(i));
                if (d == null) continue;
                if (d.kind == Detail.GOAL && haveGoals) continue;
                if (seen.add(d.key())) s.details.add(d);
            }
        }
    }

    private static Detail fromDetail(JSONObject o) {
        if (o == null) return null;
        try {
            if (o.optBoolean("shootout", false)) return null;
            int kind;
            if (o.optBoolean("scoringPlay", false)) kind = Detail.GOAL;
            else if (o.optBoolean("redCard", false)) kind = Detail.RED;
            else return null;
            Detail d = new Detail();
            d.kind = kind;
            JSONObject team = o.optJSONObject("team");
            d.teamId = team == null ? "" : team.optString("id", "");
            JSONObject clock = o.optJSONObject("clock");
            d.clock = clock == null ? "" : clock.optString("displayValue", "");
            d.ownGoal = o.optBoolean("ownGoal", false);
            d.penalty = o.optBoolean("penaltyKick", false);
            d.player = firstPlayer(o);
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    private static Detail fromKeyEvent(JSONObject o) {
        if (o == null) return null;
        try {
            if (o.optBoolean("shootout", false)) return null;
            JSONObject type = o.optJSONObject("type");
            String t = type == null ? "" : type.optString("type", "");
            int kind;
            if (o.optBoolean("scoringPlay", false)) kind = Detail.GOAL;
            else if (t.startsWith("red-card")) kind = Detail.RED;
            else return null;
            Detail d = new Detail();
            d.kind = kind;
            JSONObject team = o.optJSONObject("team");
            d.teamId = team == null ? "" : team.optString("id", "");
            JSONObject clock = o.optJSONObject("clock");
            d.clock = clock == null ? "" : clock.optString("displayValue", "");
            d.penalty = t.startsWith("penalty");
            d.ownGoal = t.startsWith("own-goal");
            d.player = firstPlayer(o);
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    /** The scoreboard lists {@code athletesInvolved}; the summary {@code participants[].athlete}. First = scorer. */
    private static String firstPlayer(JSONObject o) {
        JSONArray involved = o.optJSONArray("athletesInvolved");
        if (involved != null) {
            JSONObject a = involved.optJSONObject(0);
            if (a != null) return a.optString("displayName", a.optString("fullName", ""));
        }
        JSONArray parts = o.optJSONArray("participants");
        if (parts != null) {
            JSONObject p = parts.optJSONObject(0);
            JSONObject a = p == null ? null : p.optJSONObject("athlete");
            if (a != null) return a.optString("displayName", "");
        }
        return "";
    }
}
