package drinkcounter;

import org.crac.Core;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
class CheckpointListener implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if ("true".equals(System.getProperty("checkpoint.on.ready"))) {
            try {
                Core.checkpointRestore();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
