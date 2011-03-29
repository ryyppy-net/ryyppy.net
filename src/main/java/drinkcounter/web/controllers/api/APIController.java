package drinkcounter.web.controllers.api;

import com.csvreader.CsvWriter;
import com.google.common.base.Charsets;
import drinkcounter.DrinkCounterService;
import drinkcounter.model.User;
import drinkcounter.util.PartyMarshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Toni
 */
@Controller
public class APIController {

    private static final Logger log = LoggerFactory.getLogger(APIController.class);

    @Autowired
    private PartyMarshaller partyMarshaller;

    @Autowired
    private DrinkCounterService service;

    @RequestMapping("/parties/{partyId}")
    public @ResponseBody byte[] printXml(@PathVariable String partyId) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        partyMarshaller.marshall(partyId, baos);
        byte[] bytesXml = baos.toByteArray();
        return bytesXml;
    }

    @RequestMapping("/users/{userId}/add-drink")
    public @ResponseBody String addDrink(@PathVariable String userId){
        service.addDrink(userId);
        User user = service.getUser(userId);
        return Integer.toString(user.getTotalDrinks());
    }
    
    @RequestMapping("/users/{userId}")
    public @ResponseBody byte[] userXml(@PathVariable String userId) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        partyMarshaller.marshallUser(userId, baos);
        byte[] bytesXml = baos.toByteArray();
        return bytesXml;
    }
    
    @RequestMapping("/users/{userId}/show-history")
    public ResponseEntity<byte[]> showHistory(@PathVariable String userId) throws IOException{
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/plain;charset=utf-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvWriter csvWriter = new CsvWriter(new OutputStreamWriter(baos, Charsets.UTF_8), ',');
        csvWriter.writeRecord(new String[]{"Time", "Alcohol"});

        User user = service.getUser(userId);
        
        int intervalMs = 2 * 60 * 1000;
        
        DateTime now = new DateTime();
        DateTime start = now.minusMinutes(300);
        List<Float> history = user.getPromillesAtInterval(start.toDate(), now.toDate(), intervalMs);

        DateTime time = start;
        for (Float f : history) {
            csvWriter.writeRecord(
                    new String[]{time.toString(), Float.toString(f)}
            );
            time = time.plusMillis(intervalMs);
        }
        csvWriter.close();
        byte[] bytes = baos.toByteArray();
        return new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
    }
}
