package com.indeed.proctor.store.async;

import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.db.TrunkQaProdStoresFactory;
import org.apache.commons.configuration.ConfigurationException;

import java.util.concurrent.ExecutorService;

public class AsyncProctorStoreFactory implements TrunkQaProdStoresFactory {

    private final TrunkQaProdStoresFactory wrappedFactory;
    private final ExecutorService executorService;

    public AsyncProctorStoreFactory(final TrunkQaProdStoresFactory wrappedFactory, final ExecutorService executorService) {
        this.wrappedFactory = wrappedFactory;
        this.executorService = executorService;
    }

    @Override
    public ProctorStore getTrunkStore() {
        return new AsyncProctorStore(wrappedFactory::getTrunkStore, executorService);
    }

    @Override
    public ProctorStore getQaStore() {
        return new AsyncProctorStore(wrappedFactory::getQaStore, executorService);
    }

    @Override
    public ProctorStore getProductionStore() {
        return new AsyncProctorStore(wrappedFactory::getProductionStore, executorService);
    }

    @Override
    public ProctorStore createStore(final String relativePath) throws ConfigurationException {
        return wrappedFactory.createStore(relativePath);
    }
}