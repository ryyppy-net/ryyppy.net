package drinkcounter.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
     * never changes - safe to cache for a year.
     *
     * First-party JSP-served resources under /static/css/**, /static/js/** and
     * /static/images/** are not versioned by path, so they get a content-hash
     * VersionResourceResolver instead: the resource chain rewrites the actual
     * URL (e.g. /static/js/party.js -> /static/js/party-<hash>.js) whenever the
     * file changes. JSPs must reference these paths through <c:url> so that
     * ResourceUrlEncodingFilter (registered below) can rewrite them to the
     * hashed URL via response.encodeURL().
     *
     * Sound effects under /static/sounds/** are referenced by plain string
     * paths from AngularJS code rather than through <c:url>, so they can't go
     * through the same URL-rewriting versioning. They have no version in
     * their URL, but rarely change and the app isn't under active
     * development, so they're still cached for a year (without .immutable(),
     * so a hard refresh still revalidates if a file is ever replaced).
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

        registry.addResourceHandler("/static/css/**", "/static/js/**", "/static/images/**")
                .addResourceLocations("classpath:/public/static/css/", "classpath:/public/static/js/", "classpath:/public/static/images/")
                .setCacheControl(oneYearImmutable)
                .resourceChain(true)
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));

        registry.addResourceHandler("/static/sounds/**")
                .addResourceLocations("classpath:/public/static/sounds/")
                .setCacheControl(oneYear);
    }

    @Bean
    public FilterRegistrationBean<ResourceUrlEncodingFilter> resourceUrlEncodingFilter() {
        FilterRegistrationBean<ResourceUrlEncodingFilter> registration =
                new FilterRegistrationBean<>(new ResourceUrlEncodingFilter());
        registration.setDispatcherTypes(jakarta.servlet.DispatcherType.REQUEST);
        return registration;
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
