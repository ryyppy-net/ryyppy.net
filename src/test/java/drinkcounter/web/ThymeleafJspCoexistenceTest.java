package drinkcounter.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression test for the incident where ThymeleafViewResolver, once added
 * alongside the JSP InternalResourceViewResolver, claimed EVERY view name
 * (not just converted ones) and only failed at render time when the
 * template didn't exist - breaking every unconverted page with a 500
 * instead of falling through to JSP. Fixed by constraining it to the
 * spring.thymeleaf.view-names allow-list in application.yml. Every page has
 * since been converted (see issue #75), but the guard still matters for any
 * future new view name, so this uses a name that is deliberately not in the
 * allow-list rather than a real page.
 */
class ThymeleafJspCoexistenceTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(ThymeleafAutoConfiguration.class))
            .withPropertyValues("spring.thymeleaf.view-names=privacy");

    @Test
    void resolverClaimsOnlyAllowListedViewNames() {
        contextRunner.run(context -> {
            ThymeleafViewResolver resolver = context.getBean(ThymeleafViewResolver.class);

            assertNotNull(resolver.resolveViewName("privacy", Locale.of("fi", "FI")),
                    "the converted view name must still resolve to a Thymeleaf view");
            assertNull(resolver.resolveViewName("not-a-real-view", Locale.of("fi", "FI")),
                    "an unlisted view name must be left for the JSP resolver, not claimed and failed on by Thymeleaf");
        });
    }
}
