package com.indeed.proctor.webapp.db;

import com.indeed.proctor.store.ProctorStore;
import org.apache.commons.configuration.ConfigurationException;

/**
 */
public interface StoreFactory {
    public ProctorStore createStore(final String relativePath) throws ConfigurationException;
}
