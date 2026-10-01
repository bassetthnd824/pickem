package com.curleesoft.pickem.backend.seed;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Reference data for {@code tools/seed}. Team names and {@code cfbdTeamId}
 * values are the CFBD school strings and ids so schedule import can resolve
 * the conference. Venue ids are CFBD venue ids. Document ids stay stable so a
 * re-run updates the same documents.
 */
public final class SeedCatalog {

    public static final String ACTOR = "seed";

    public static final String SEASON_YEAR = "2026";

    public static final LocalDate SEASON_BEGIN = LocalDate.of(2026, 8, 27);

    public static final int WEEK_COUNT = 15;

    public static final String DEFAULT_THEME = "light";

    public static final String UNKNOWN_VENUE_ID = "unknown";

    private static final List<VenueSeed> VENUES = List.of(
            new VenueSeed("bryant-denny", "Bryant-Denny Stadium", "Tuscaloosa, AL", 3657L),
            new VenueSeed("razorback", "Razorback Stadium", "Fayetteville, AR", 3887L),
            new VenueSeed("jordan-hare", "Jordan-Hare Stadium", "Auburn, AL", 3785L),
            new VenueSeed("ben-hill-griffin", "Ben Hill Griffin Stadium", "Gainesville, FL", 3634L),
            new VenueSeed("sanford", "Sanford Stadium", "Athens, GA", 3917L),
            new VenueSeed("kroger-field", "Kroger Field", "Lexington, KY", 3683L),
            new VenueSeed("tiger-stadium", "Tiger Stadium", "Baton Rouge, LA", 3958L),
            new VenueSeed("davis-wade", "Davis Wade Stadium", "Starkville, MS", 3693L),
            new VenueSeed("faurot-field", "Faurot Field", "Columbia, MO", 3838L),
            new VenueSeed("gaylord", "Gaylord Family Oklahoma Memorial Stadium", "Norman, OK", 3835L),
            new VenueSeed("vaught-hemingway", "Vaught-Hemingway Stadium", "Oxford, MS", 3974L),
            new VenueSeed("williams-brice", "Williams-Brice Stadium", "Columbia, SC", 3994L),
            new VenueSeed("neyland", "Neyland Stadium", "Knoxville, TN", 3853L),
            new VenueSeed("dkr-texas", "DKR-Texas Memorial Stadium", "Austin, TX", 3910L),
            new VenueSeed("kyle-field", "Kyle Field", "College Station, TX", 3795L),
            new VenueSeed("firstbank", "FirstBank Stadium", "Nashville, TN", 3973L),
            new VenueSeed(UNKNOWN_VENUE_ID, "Unknown", "Unknown", null));

    private static final List<TeamSeed> TEAMS = List.of(
            new TeamSeed("alabama", "Alabama", "Crimson Tide", 333L, "bryant-denny"),
            new TeamSeed("arkansas", "Arkansas", "Razorbacks", 8L, "razorback"),
            new TeamSeed("auburn", "Auburn", "Tigers", 2L, "jordan-hare"),
            new TeamSeed("florida", "Florida", "Gators", 57L, "ben-hill-griffin"),
            new TeamSeed("georgia", "Georgia", "Bulldogs", 61L, "sanford"),
            new TeamSeed("kentucky", "Kentucky", "Wildcats", 96L, "kroger-field"),
            new TeamSeed("lsu", "LSU", "Tigers", 99L, "tiger-stadium"),
            new TeamSeed("mississippi-state", "Mississippi State", "Bulldogs", 344L, "davis-wade"),
            new TeamSeed("missouri", "Missouri", "Tigers", 142L, "faurot-field"),
            new TeamSeed("oklahoma", "Oklahoma", "Sooners", 201L, "gaylord"),
            new TeamSeed("ole-miss", "Ole Miss", "Rebels", 145L, "vaught-hemingway"),
            new TeamSeed("south-carolina", "South Carolina", "Gamecocks", 2579L, "williams-brice"),
            new TeamSeed("tennessee", "Tennessee", "Volunteers", 2633L, "neyland"),
            new TeamSeed("texas", "Texas", "Longhorns", 251L, "dkr-texas"),
            new TeamSeed("texas-am", "Texas A&M", "Aggies", 245L, "kyle-field"),
            new TeamSeed("vanderbilt", "Vanderbilt", "Commodores", 238L, "firstbank"));

    private static final List<RivalrySeed> RIVALRIES = List.of(
            new RivalrySeed("iron-bowl", "Iron Bowl", "alabama", "auburn"),
            new RivalrySeed("alabama-ole-miss-rivalry", "Alabama - Ole Miss Rivalry", "alabama", "ole-miss"),
            new RivalrySeed("third-saturday-in-october", "Third Saturday in October", "alabama", "tennessee"),
            new RivalrySeed("saban-bowl", "Saban Bowl", "alabama", "lsu"),
            new RivalrySeed("90-mile-drive", "90 Mile Drive", "alabama", "mississippi-state"),
            new RivalrySeed("southwest-classic", "Southwest Classic", "arkansas", "texas-am"),
            new RivalrySeed("arkansas-ole-miss-rivalry", "Arkansas - Ole Miss Rivalry", "arkansas", "ole-miss"),
            new RivalrySeed("battle-for-the-golden-boot", "Battle for the Golden Boot", "arkansas", "lsu"),
            new RivalrySeed("tiger-bowl", "Tiger Bowl", "auburn", "lsu"),
            new RivalrySeed("deep-souths-oldest-rivalry", "Deep South's Oldest Rivalry", "auburn", "georgia"),
            new RivalrySeed("third-saturday-in-september", "Third Saturday in September", "florida", "tennessee"),
            new RivalrySeed("lsu-florida-rivalry", "LSU - Florida Rivalry", "lsu", "florida"),
            new RivalrySeed("worlds-largest-outdoor-cocktail-party", "World's Largest Outdoor Cocktail Party",
                    "florida", "georgia"),
            new RivalrySeed("battle-for-the-barrel", "Battle for the Barrel", "tennessee", "kentucky"),
            new RivalrySeed("magnolia-bowl", "Magnolia Bowl", "lsu", "ole-miss"),
            new RivalrySeed("egg-bowl", "Egg Bowl", "ole-miss", "mississippi-state"),
            new RivalrySeed("vanderbilt-ole-miss-rivalry", "Vanderbilt - Ole Miss Rivalry", "vanderbilt", "ole-miss"),
            new RivalrySeed("tennessee-vanderbilt-rivalry", "Tennessee - Vanderbilt Rivalry", "tennessee",
                    "vanderbilt"),
            new RivalrySeed("tennessee-georgia-rivalry", "Tennessee - Georgia Rivalry", "tennessee", "georgia"),
            new RivalrySeed("florida-auburn-rivalry", "Florida - Auburn Rivalry", "florida", "auburn"),
            new RivalrySeed("georgia-south-carolina-rivalry", "Georgia - South Carolina Rivalry", "georgia",
                    "south-carolina"),
            new RivalrySeed("halloween-game", "Halloween Game", "south-carolina", "tennessee"),
            new RivalrySeed("mississippi-state-kentucky-rivalry", "Mississippi State - Kentucky Rivalry",
                    "mississippi-state", "kentucky"),
            new RivalrySeed("red-river-rivalry", "Red River Rivalry", "texas", "oklahoma"),
            new RivalrySeed("lone-star-showdown", "Lone Star Showdown", "texas", "texas-am"));

    private static final List<ThemeSeed> THEMES = List.of(
            new ThemeSeed("light", "Light", "#1F2937", "#2563EB"),
            new ThemeSeed("dark", "Dark", "#0F172A", "#38BDF8"),
            new ThemeSeed("alabama", "Alabama Crimson Tide", "#9E1B32", "#828A8F"),
            new ThemeSeed("arkansas", "Arkansas Razorbacks", "#9D2235", "#FFFFFF"),
            new ThemeSeed("auburn", "Auburn Tigers", "#0C2340", "#E87722"),
            new ThemeSeed("florida", "Florida Gators", "#0021A5", "#FA4616"),
            new ThemeSeed("georgia", "Georgia Bulldogs", "#BA0C2F", "#000000"),
            new ThemeSeed("kentucky", "Kentucky Wildcats", "#0033A0", "#000000"),
            new ThemeSeed("lsu", "LSU Tigers", "#461D7C", "#FDD023"),
            new ThemeSeed("mississippi-state", "Mississippi State Bulldogs", "#660000", "#75787B"),
            new ThemeSeed("missouri", "Missouri Tigers", "#000000", "#F1B82D"),
            new ThemeSeed("oklahoma", "Oklahoma Sooners", "#841617", "#FDF9D8"),
            new ThemeSeed("ole-miss", "Ole Miss Rebels", "#CE1126", "#14213D"),
            new ThemeSeed("south-carolina", "South Carolina Gamecocks", "#73000A", "#000000"),
            new ThemeSeed("tennessee", "Tennessee Volunteers", "#FF8200", "#58595B"),
            new ThemeSeed("texas", "Texas Longhorns", "#BF5700", "#333F48"),
            new ThemeSeed("texas-am", "Texas A&M Aggies", "#500000", "#FFFFFF"),
            new ThemeSeed("vanderbilt", "Vanderbilt Commodores", "#000000", "#866D4B"));

    private static final List<MatchupSeed> MATCHUPS = List.of(
            new MatchupSeed("2026-w1-vanderbilt-at-alabama", 1, "2026-08-29", "alabama", "vanderbilt", "bryant-denny",
                    42, 10),
            new MatchupSeed("2026-w1-ole-miss-at-georgia", 1, "2026-08-29", "georgia", "ole-miss", "sanford", 31, 24),
            new MatchupSeed("2026-w1-kentucky-at-florida", 1, "2026-08-29", "florida", "kentucky", "ben-hill-griffin",
                    35, 14),
            new MatchupSeed("2026-w4-auburn-at-georgia", 4, "2026-09-19", "georgia", "auburn", "sanford", 28, 17),
            new MatchupSeed("2026-w4-lsu-at-ole-miss", 4, "2026-09-19", "ole-miss", "lsu", "vaught-hemingway", 24, 21),
            new MatchupSeed("2026-w5-tennessee-at-alabama", 5, "2026-09-26", "alabama", "tennessee", "bryant-denny", 24,
                    17),
            new MatchupSeed("2026-w5-texas-at-oklahoma", 5, "2026-09-26", "oklahoma", "texas", "gaylord", 28, 21),
            new MatchupSeed("2026-w5-missouri-at-south-carolina", 5, "2026-09-26", "south-carolina", "missouri",
                    "williams-brice", null, null),
            new MatchupSeed("2026-w6-alabama-at-auburn", 6, "2026-10-03", "auburn", "alabama", "jordan-hare", null,
                    null),
            new MatchupSeed("2026-w6-florida-at-georgia", 6, "2026-10-03", "georgia", "florida", "sanford", null, null),
            new MatchupSeed("2026-w6-arkansas-at-lsu", 6, "2026-10-03", "lsu", "arkansas", "tiger-stadium", null,
                    null));

    private SeedCatalog() {
    }

    public static List<VenueSeed> venues() {
        return VENUES;
    }

    public static List<TeamSeed> teams() {
        return TEAMS;
    }

    public static List<RivalrySeed> rivalries() {
        return RIVALRIES;
    }

    public static List<ThemeSeed> themes() {
        return THEMES;
    }

    public static List<MatchupSeed> matchups() {
        return MATCHUPS;
    }

    public static String seasonId() {
        return SEASON_YEAR;
    }

    public static LocalDate seasonEnd() {
        return weekBegin(WEEK_COUNT).plusDays(6);
    }

    public static LocalDate weekBegin(int weekNumber) {
        return SEASON_BEGIN.plusDays((weekNumber - 1L) * 7L);
    }

    public static String weekId(int weekNumber) {
        return String.format(Locale.ROOT, "%s-w%02d", SEASON_YEAR, weekNumber);
    }

    public record VenueSeed(String id, String name, String cityState, Long cfbdVenueId) {
    }

    public record TeamSeed(String id, String name, String squad, long cfbdTeamId, String venueId) {
    }

    public record RivalrySeed(String id, String name, String team1Id, String team2Id) {
    }

    public record ThemeSeed(String key, String name, String primary, String secondary) {
    }

    public record MatchupSeed(String id, int weekNumber, String date, String homeId, String awayId, String venueId,
            Integer homeScore, Integer awayScore) {
    }
}
