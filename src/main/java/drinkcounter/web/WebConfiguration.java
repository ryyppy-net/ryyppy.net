package drinkcounter.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    /**
     * WebJar assets and hand-vendored third-party builds under /static/vendor/**
     * are versioned in their URL path (e.g. /webjars/jquery/1.8.3/...,
     * /static/vendor/jquery-ui/1.8.12.custom/...), so a given URL's content
     * never changes - safe to cache for a year as immutable.
     *
     * Sound effects under /static/sounds/** have no version in their URL, but
     * rarely change and the app isn't under active development, so they're
     * also cached for a year (without .immutable(), so a hard refresh still
     * revalidates if the file is ever replaced). Other first-party static
     * resources (everything else under /static/**) are untouched and keep the
     * default, unversioned caching behavior.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        CacheControl oneYearImmutable = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable();
        CacheControl oneYear = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic();

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .setCacheControl(oneYearImmutable);

        registry.addResourceHandler("/static/vendor/**")
                .addResourceLocations("classpath:/public/static/vendor/")
                .setCacheControl(oneYearImmutable);

        registry.addResourceHandler("/static/sounds/**")
                .addResourceLocations("classpath:/public/static/sounds/")
                .setCacheControl(oneYear);
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
}
