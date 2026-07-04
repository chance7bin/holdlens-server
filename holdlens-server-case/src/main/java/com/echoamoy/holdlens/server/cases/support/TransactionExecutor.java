package com.echoamoy.holdlens.server.cases.support;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.Resource;
import java.util.function.Supplier;

/**
 * Case-layer transaction boundary helper; domain code must stay free of Spring transaction APIs.
 */
@Component
public class TransactionExecutor {

    @Resource
    private PlatformTransactionManager transactionManager;

    public <T> T required(Supplier<T> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate.execute(status -> action.get());
    }

    public <T> T requiresNew(Supplier<T> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> action.get());
    }
}
