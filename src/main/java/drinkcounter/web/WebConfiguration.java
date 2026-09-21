package drinkcounter.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.GitProperties;
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
     * /static/vendor/jquery-ui/1.8.24/...), so a given URL's content
     * never changes - safe to cache for a year.
     *
     * First-party JSP-served resources under /static/css/**, /static/js/**,
     * /static/images/**, /app/css/** and /app/js/** are not versioned by
     * path, so they get a content-hash VersionResourceResolver instead: the
     * resource chain rewrites the actual URL (e.g. /static/js/party.js ->
     * /static/js/party-<hash>.js) whenever the file changes. JSPs must
     * reference these paths through <c:url> so that ResourceUrlEncodingFilter
     * (registered below) can rewrite them to the hashed URL via
     * response.encodeURL(). This is why the AngularJS app's index.html is a
     * JSP (appIndex.jsp) rather than a static file - only JSP's <c:url>
     * triggers the rewrite.
     *
     * Sound effects under /static/sounds/** are content-hashed the same way.
     * Nothing references them from markup, so SoundManifest resolves them
     * through this chain server-side instead.
     *
     * Font files under /static/fonts/** are referenced by plain url() in
     * fonts.css rather than through a template, so they can't go through the
     * <c:url>-driven rewrite either - they get the vendor treatment instead:
     * a fixed path with a long cache lifetime.
     *
     * The favicon and Apple touch icons are requested by browsers/OS via
     * fixed, well-known root paths (not referenced through any <c:url> or
     * <link> tag), so they can't be versioned either. They change rarely, so
     * they get a week-long cache - short enough to roll out a replacement
     * without a URL change, unlike the year-long caches above.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        CacheControl oneYearImmutable = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable();
        CacheControl oneWeek = CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic();

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

        registry.addResourceHandler("/app/css/**", "/app/js/**")
                .addResourceLocations("classpath:/public/app/css/", "classpath:/public/app/js/")
                .setCacheControl(oneYearImmutable)
                .resourceChain(true)
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));

        registry.addResourceHandler("/static/sounds/**")
                .addResourceLocations("classpath:/public/static/sounds/")
                .setCacheControl(oneYearImmutable)
                .resourceChain(true)
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));

        registry.addResourceHandler("/static/fonts/**")
                .addResourceLocations("classpath:/public/static/fonts/")
                .setCacheControl(oneYearImmutable);

        registry.addResourceHandler("/favicon.ico", "/apple-touch-icon.png", "/apple-touch-icon-precomposed.png")
                .addResourceLocations("classpath:/public/")
                .setCacheControl(oneWeek);
    }

    @Bean
    public FilterRegistrationBean<ResourceUrlEncodingFilter> resourceUrlEncodingFilter() {
        FilterRegistrationBean<ResourceUrlEncodingFilter> registration =
                new FilterRegistrationBean<>(new ResourceUrlEncodingFilter());
        registration.setDispatcherTypes(jakarta.servlet.DispatcherType.REQUEST);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AppVersionFilter> appVersionFilter(
            @Autowired(required = false) GitProperties gitProperties) {
        FilterRegistrationBean<AppVersionFilter> registration =
                new FilterRegistrationBean<>(new AppVersionFilter(gitProperties));
        registration.addUrlPatterns("/*");
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
