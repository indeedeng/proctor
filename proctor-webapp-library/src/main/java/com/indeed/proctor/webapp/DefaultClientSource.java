package com.indeed.proctor.webapp;

import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.ProctorClientApplication;

import java.util.Collections;
import java.util.List;

/**
 */
public class DefaultClientSource implements ProctorClientSource {
    @Override
    public List<ProctorClientApplication> loadClients(final Environment environment) {
        return Collections.emptyList();
    }

    @Override
    public void probe(final Environment environment) throws Exception {}
}
