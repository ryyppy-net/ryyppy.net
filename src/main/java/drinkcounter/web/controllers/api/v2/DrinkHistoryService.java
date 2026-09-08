package drinkcounter.web.controllers.api.v2;

import com.csvreader.CsvWriter;
import drinkcounter.model.Drink;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the "Time,Drinks" per-day CSV that ProfileApiController.getDrinkHistory()
 * serves and userhistorygraph.js parses, extracted so DefaultController can build
 * the same data to embed on first render.
 */
public class DrinkHistoryService {
    public static String buildCsv(List<Drink> drinks, Clock clock) throws IOException {
        Map<String, Integer> drinksPerDay = new LinkedHashMap<String, Integer>();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        double timezoneOffset = 0; // (Double)session.getAttribute(AuthenticationController.TIMEZONEOFFSET);
        ZoneOffset dtz = ZoneOffset.ofTotalSeconds((int) (-timezoneOffset * 60));

        for (Drink d : drinks) {
            String s = d.getTimeStamp().atZone(dtz).format(format);

            Integer i = 0;
            if (drinksPerDay.containsKey(s))
                i = drinksPerDay.get(s);
            i += 1;
            drinksPerDay.put(s, i);
        }

        String today = LocalDate.now(clock.withZone(dtz)).format(format);
        if (!drinksPerDay.containsKey(today))
            drinksPerDay.put(today, 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{"Time", "Drinks"});

        for (Map.Entry<String, Integer> p : drinksPerDay.entrySet()) {
            long millis = LocalDate.parse(p.getKey(), format).atStartOfDay(dtz).toInstant().toEpochMilli();
            csvWriter.writeRecord(new String[]{Long.toString(millis), p.getValue().toString()});
        }

        csvWriter.close();
        return baos.toString(StandardCharsets.UTF_8);
    }
}
