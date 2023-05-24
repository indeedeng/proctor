package com.indeed.proctor.webapp.db;

import com.indeed.proctor.store.ProctorStore;

/** */
public interface StoreFactory {
    public ProctorStore createStore(final String relativePath);
}
