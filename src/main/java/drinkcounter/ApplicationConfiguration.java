package drinkcounter;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({
    "classpath:drinkcounter/applicationContext.xml"
})
public class ApplicationConfiguration {

}
