package com.indeed.proctor.webapp.db;

import com.indeed.proctor.store.ProctorStore;

/** A factory creating 3 distinct stores for different lifecycle stages of proctor tests */
public interface TrunkQaProdStoresFactory extends StoreFactory {

    ProctorStore getTrunkStore();

    ProctorStore getQaStore();

    ProctorStore getProductionStore();
}
