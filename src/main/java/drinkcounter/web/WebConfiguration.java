package drinkcounter.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import java.time.Duration;
import java.util.Locale;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final String buildId;
    private final Duration fingerprintedMaxAge;

    public WebConfiguration(
            StaticAssets staticAssets,
            @Value("${application.cache.fingerprinted-max-age}") Duration fingerprintedMaxAge) {
        this.buildId = staticAssets.buildId();
        this.fingerprintedMaxAge = fingerprintedMaxAge;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // The fingerprinted copy of classpath:/public/. VersionResourceResolver
        // strips the /v/<build id>/ prefix before looking the file up, and
        // refuses prefixes from any other build - stale URLs must not resolve,
        // or a page cached from an older deploy would keep loading old assets
        // forever. Because the prefix is part of the path (not a ?v= query),
        // relative references inside the served files - the app shell's own
        // <script src="js/app.js">, the fonts font-awesome.css asks for, the
        // partials AngularJS fetches - land under the same prefix on their own.
        registry.addResourceHandler(StaticAssets.VERSIONED_PREFIX + "/**")
                .addResourceLocations("classpath:/public/")
                .setCacheControl(fingerprintedCacheControl())
                // Resolved resources are only worth caching in memory once
                // they're immutable; in development the point is to pick up
                // edits without a restart.
                .resourceChain(cachesAssets())
                .addResolver(new VersionResourceResolver().addFixedVersionStrategy(buildId, "/**"));

        // The app shell is the one file that must never be cached hard: it's
        // what points at the current build's assets, so a stale copy would keep
        // a browser on an old build (or, once those URLs stop resolving, on a
        // broken page). Revalidating it is cheap - Last-Modified makes it a 304.
        registry.addResourceHandler("/app/*.html")
                .addResourceLocations("classpath:/public/app/")
                .setCacheControl(CacheControl.noCache());
    }

    /**
     * Rewrites asset URLs emitted by JSTL's {@code <c:url>} into their
     * fingerprinted form, so the JSP views don't have to spell out the build id.
     */
    @Bean
    public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
        return new ResourceUrlEncodingFilter();
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        slr.setDefaultLocale(Locale.of("fi", "FI"));
        return slr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    private CacheControl fingerprintedCacheControl() {
        if (!cachesAssets()) {
            return CacheControl.noCache();
        }
        return CacheControl.maxAge(fingerprintedMaxAge).immutable();
    }

    private boolean cachesAssets() {
        return !fingerprintedMaxAge.isZero() && !fingerprintedMaxAge.isNegative();
    }
}
