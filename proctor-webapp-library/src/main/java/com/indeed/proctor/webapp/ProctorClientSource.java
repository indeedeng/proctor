package com.indeed.proctor.webapp;

import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.ProctorClientApplication;

import java.util.List;

public interface ProctorClientSource {
    List<ProctorClientApplication> loadClients(Environment environment);

    void probe(Environment environment) throws Exception;
}
