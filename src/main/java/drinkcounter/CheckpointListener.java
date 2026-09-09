package drinkcounter;

import org.crac.Core;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Optionally triggers a CRaC checkpoint once the app is fully started, when
 * run with {@code -Dcheckpoint.on.ready=true}. Used for local testing; the
 * Railway build pipeline instead warms up the app with real traffic first
 * and triggers the checkpoint externally via jcmd, so the checkpoint
 * captures JIT-compiled, already-warmed code paths rather than a checkpoint
 * taken the instant the app becomes ready.
 *
 * <p>Rebinding the DataSource (and other beans listed under
 * {@code spring.cloud.refresh.extra-refreshable} in application.yml)
 * against the live environment after restore - so a checkpoint trained
 * against a throwaway build-time database can restore safely into a
 * container with real production credentials - is handled automatically
 * by Spring Cloud Context's own {@code RefreshScopeLifecycle} once
 * {@code org.crac:crac} and {@code spring-cloud-context} are on the
 * classpath; no application code is needed for that part.
 */
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
