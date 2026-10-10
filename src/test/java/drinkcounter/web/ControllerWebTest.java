package drinkcounter.web;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.GoogleIdentityLinkingService;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.authentication.relay.AuthRelayTokenService;
import drinkcounter.dao.PartyDAO;
import drinkcounter.util.PartyMarshaller;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Web-layer test of every controller behind the app's security filter chain, with the services
 * mocked. All controller tests share this one configuration so they share one Spring context;
 * a test that declares its own {@code @MockitoBean} or narrows {@code @WebMvcTest} starts another.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@Import({WebSecurityConfiguration.class, SoundManifest.class, PartyAccess.class})
@MockitoBean(types = {
        DrinkCounterService.class,
        PartyDAO.class,
        UserService.class,
        PartyMarshaller.class,
        GoogleIdentityLinkingService.class,
        AuthRelayTokenService.class
})
@MockitoBean(name = "oauth2LoginCustomizer", types = Customizer.class)
public @interface ControllerWebTest {
}
