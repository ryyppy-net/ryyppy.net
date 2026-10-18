package drinkcounter.web;

import jakarta.servlet.DispatcherType;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.transaction.support.TransactionOperations;

@Configuration(proxyBeanMethods = false)
public class SessionConfiguration {

    /**
     * A request reads the session row and writes its last access time back, one statement
     * each. Run them in autocommit: a transaction around them adds a COMMIT round trip apiece.
     */
    @Bean
    TransactionOperations springSessionTransactionOperations() {
        return TransactionOperations.withoutTransaction();
    }

    @Bean
    static BeanPostProcessor requestSessionCachingRepository() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (!(bean instanceof SessionRepository)) {
                    return bean;
                }
                ProxyFactory proxyFactory = new ProxyFactory(bean);
                proxyFactory.addAdvice(new RequestSessionCache());
                return proxyFactory.getProxy();
            }
        };
    }

    @Bean
    FilterRegistrationBean<RequestSessionCache.Scope> requestSessionCacheScope() {
        FilterRegistrationBean<RequestSessionCache.Scope> registration =
                new FilterRegistrationBean<>(new RequestSessionCache.Scope());
        registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER - 1);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        return registration;
    }
}
