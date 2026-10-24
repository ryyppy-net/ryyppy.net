package drinkcounter.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
}
