package drinkcounter.alcoholcalculator;

import drinkcounter.Promilles;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Replays the scenarios in previous-curves.txt, which hold what the previous calculation gave,
 * and expects the same curve bit for bit.
 */
public class PreviousCurvesTest {

    private static final Instant NOW = Instant.parse("2026-10-25T21:37:00Z");
    private static final Duration CHART = Duration.ofMinutes(300);
    private static final Duration WIDE_BEFORE = Duration.ofDays(10);
    private static final Duration WIDE_AFTER = Duration.ofDays(2);
    private static final Duration LOOKBACK = Duration.ofDays(7);

    // grams were read a few milliseconds after the drinks were timed; that drift is far below this.
    private static final float GRAMS_TOLERANCE = 0.001f;

    record Scenario(String name, User user, List<Drink> drinks, int chartStep, int wideStep,
            float promilles, float grams, int totalDrinks, float[] chart, float[] wide) {
        @Override
        public String toString() {
            return name;
        }
    }

    static List<Scenario> scenarios() throws IOException {
        String text;
        try (InputStream in = PreviousCurvesTest.class.getResourceAsStream("previous-curves.txt")) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<Scenario> scenarios = new ArrayList<>();
        int id = 1;
        for (String block : text.split("\n\n")) {
            Map<String, String> values = new HashMap<>();
            for (String line : block.split("\n")) {
                if (line.isBlank() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                values.put(line.substring(0, eq), line.substring(eq + 1));
            }
            if (values.isEmpty()) continue;

            User user = new User();
            user.setId(id++);
            user.setWeight(Float.parseFloat(values.get("weight")));
            user.setSex(User.Sex.valueOf(values.get("sex")));
            List<Drink> drinks = new ArrayList<>();
            if (!values.get("drinks").isEmpty()) {
                for (String drinkText : values.get("drinks").split(",")) {
                    String[] parts = drinkText.split(":");
                    Drink drink = new Drink();
                    drink.setTimeStamp(NOW.plusMillis(Long.parseLong(parts[0])));
                    drink.setAlcohol(Float.parseFloat(parts[1]));
                    user.drink(drink);
                    drinks.add(drink);
                }
            }
            scenarios.add(new Scenario(values.get("scenario"), user, drinks,
                    Integer.parseInt(values.get("chartStep")), Integer.parseInt(values.get("wideStep")),
                    Float.parseFloat(values.get("expected.now")), Float.parseFloat(values.get("expected.grams")),
                    Integer.parseInt(values.get("expected.totalDrinks")),
                    floats(values.get("expected.chart")), floats(values.get("expected.wide"))));
        }
        return scenarios;
    }

    static Stream<Scenario> soberDuringTheWeek() throws IOException {
        return scenarios().stream().filter(s -> !drunkAllWeek(s));
    }

    static Stream<Scenario> drunkAllWeek() throws IOException {
        return scenarios().stream().filter(PreviousCurvesTest::drunkAllWeek);
    }

    /** Not sober at any minute from a week ago until the chart starts. */
    private static boolean drunkAllWeek(Scenario scenario) {
        return firstSoberMinuteInTheWeek(scenario).isAfter(NOW.minus(CHART));
    }

    /** Before then the curve still holds drinks from before the week; after it, it does not. */
    private static Instant firstSoberMinuteInTheWeek(Scenario scenario) {
        BloodAlcohol allDrinks = new BloodAlcohol(scenario.user(), scenario.drinks(), NOW);
        Instant cutoff = NOW.minus(LOOKBACK);
        List<Float> week = allDrinks.promillesAtInterval(Date.from(cutoff), Date.from(NOW), 60_000);
        for (int minute = 0; minute < week.size(); minute++) {
            if (week.get(minute) == 0f) return cutoff.plus(Duration.ofMinutes(minute));
        }
        return Instant.MAX;
    }

    @Test
    public void theScenariosCoverEveryKindOfHistory() throws IOException {
        List<Scenario> all = scenarios();
        assertTrue(all.size() > 200, "scenarios: " + all.size());
        assertTrue(all.stream().anyMatch(s -> s.drinks().isEmpty()));
        assertTrue(all.stream().anyMatch(s -> s.drinks().stream().anyMatch(d -> d.getTimeStamp().isAfter(NOW))));
        assertTrue(all.stream().anyMatch(s -> s.drinks().stream().anyMatch(d -> !d.getTimeStamp().isAfter(NOW.minus(LOOKBACK)))));
        assertTrue(all.stream().anyMatch(s -> s.totalDrinks() > 20));
        assertTrue(drunkAllWeek().findAny().isPresent());
        assertTrue(all.stream().anyMatch(s -> !drunkAllWeek(s)
                && firstSoberMinuteInTheWeek(s).isAfter(NOW.minus(LOOKBACK))), "a session across the week's start");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    public void allDrinksGiveThePreviousCurve(Scenario scenario) {
        BloodAlcohol bloodAlcohol = new BloodAlcohol(scenario.user(), scenario.drinks(), NOW);

        assertCurrentValues(scenario, bloodAlcohol);
        assertArrayEquals(scenario.chart(), chart(bloodAlcohol, scenario), "chart");
        assertArrayEquals(scenario.wide(), wide(bloodAlcohol, scenario), "wide");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("soberDuringTheWeek")
    public void theLastWeekOfDrinksGivesThePreviousCurve(Scenario scenario) {
        BloodAlcohol bloodAlcohol = promilles(scenario).of(scenario.user());

        assertCurrentValues(scenario, bloodAlcohol);
        assertArrayEquals(scenario.chart(), chart(bloodAlcohol, scenario), "chart");
        float[] wide = wide(bloodAlcohol, scenario);
        long fromSober = Duration.between(NOW.minus(WIDE_BEFORE), firstSoberMinuteInTheWeek(scenario)).toMillis();
        int first = (int) ((fromSober + scenario.wideStep() - 1) / scenario.wideStep());
        assertArrayEquals(Arrays.copyOfRange(scenario.wide(), first, wide.length),
                Arrays.copyOfRange(wide, first, wide.length), "wide, from the first sober minute in the week on");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    public void aPartyReadAtOnceGivesEachThePreviousValues(Scenario scenario) throws IOException {
        List<Scenario> party = new ArrayList<>(soberDuringTheWeek().limit(20).toList());
        party.removeIf(s -> s.name().equals(scenario.name()));
        if (!drunkAllWeek(scenario)) party.add(scenario);
        Promilles promilles = promilles(party.toArray(Scenario[]::new));

        Map<Integer, BloodAlcohol> bloodAlcohol = promilles.of(party.stream().map(Scenario::user).toList());

        for (Scenario member : party) {
            assertCurrentValues(member, bloodAlcohol.get(member.user().getId()));
        }
    }

    /** Drinking for a week without sobering up is cut short: drinks before the week no longer count. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("drunkAllWeek")
    public void aSessionLongerThanAWeekIsCutShort(Scenario scenario) {
        BloodAlcohol lastWeek = promilles(scenario).of(scenario.user());

        assertTrue(lastWeek.promilles() < scenario.promilles(), lastWeek.promilles() + " >= " + scenario.promilles());
        float[] chart = chart(lastWeek, scenario);
        for (int minute = 0; minute < chart.length; minute++) {
            assertTrue(chart[minute] < scenario.chart()[minute], "minute " + minute);
        }
    }

    @Test
    public void aSessionLongerThanAWeekIsTheOnlyOneCutShort() throws IOException {
        assertEquals(List.of("eight-day session crossing the week"),
                drunkAllWeek().map(Scenario::name).toList());
    }

    private static void assertCurrentValues(Scenario scenario, BloodAlcohol bloodAlcohol) {
        assertEquals(scenario.promilles(), bloodAlcohol.promilles(), "promilles now");
        assertEquals(scenario.grams(), bloodAlcohol.grams(), GRAMS_TOLERANCE, "grams now");
        assertEquals(scenario.totalDrinks(), bloodAlcohol.drinksSinceSober(), "drinks since sober");
    }

    private static float[] chart(BloodAlcohol bloodAlcohol, Scenario scenario) {
        return floats(bloodAlcohol.promillesAtInterval(Date.from(NOW.minus(CHART)), Date.from(NOW), scenario.chartStep()));
    }

    private static float[] wide(BloodAlcohol bloodAlcohol, Scenario scenario) {
        return floats(bloodAlcohol.promillesAtInterval(Date.from(NOW.minus(WIDE_BEFORE)), Date.from(NOW.plus(WIDE_AFTER)),
                scenario.wideStep()));
    }

    /** Promilles over a repository that returns what the database query would. */
    @SuppressWarnings("unchecked")
    private static Promilles promilles(Scenario... scenarios) {
        DrinkRepository drinkRepository = mock(DrinkRepository.class);
        when(drinkRepository.findByDrinkersSince(any(), any())).thenAnswer(invocation -> {
            Collection<Integer> ids = invocation.getArgument(0);
            Instant since = invocation.getArgument(1);
            return Arrays.stream(scenarios)
                    .filter(s -> ids.contains(s.user().getId()))
                    .flatMap(s -> s.drinks().stream())
                    .filter(drink -> drink.getTimeStamp().isAfter(since))
                    .sorted(Comparator.comparing(Drink::getTimeStamp))
                    .toList();
        });
        Promilles promilles = new Promilles(drinkRepository);
        ReflectionTestUtils.setField(promilles, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
        return promilles;
    }

    private static float[] floats(String text) {
        String[] parts = text.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) result[i] = Float.parseFloat(parts[i]);
        return result;
    }

    private static float[] floats(List<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i);
        return result;
    }
}
