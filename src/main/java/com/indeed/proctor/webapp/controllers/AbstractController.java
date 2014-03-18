package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.ProctorClientSource;
import com.indeed.proctor.webapp.model.WebappConfiguration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * @author parker
 */
public class AbstractController {
    private final WebappConfiguration configuration;
    private final Map<Environment, ProctorStore> stores;

    public AbstractController(final WebappConfiguration configuration,
                              final ProctorStore trunkStore,
                              final ProctorStore qaStore,
                              final ProctorStore productionStore) {
        this.configuration = configuration;
        stores = ImmutableMap.of(
            Environment.WORKING, trunkStore,
            Environment.QA, qaStore,
            Environment.PRODUCTION, productionStore
        );
    }

    protected static String printThrowable(final Throwable t) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    protected static boolean isAJAXRequest(final HttpServletRequest request) {
        final String xhrHeader = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equals(xhrHeader)) {
            return true;
        }
        // For redirects in an AJAX request, it's common to append parameter
        final String xhrParameter = request.getParameter("X-Requested-With");
        return "XMLHttpRequest".equals(xhrHeader);
    }

    protected Environment determineEnvironmentFromParameter(String branch) {
        final Environment b = Environment.fromName(branch);
        return b != null ? b : Environment.WORKING;
    }

    protected WebappConfiguration getConfiguration() {
        return configuration;
    }

    protected ProctorStore determineStoreFromEnvironment(final Environment branch) {
        final ProctorStore store = stores.get(branch);
        if(store == null) {
            throw new RuntimeException("Unknown store for branch " + branch);
        }
        return store;
    }

    protected TestMatrixVersion getCurrentMatrix(final Environment branch) {
        try {
            return determineStoreFromEnvironment(branch).getCurrentTestMatrix();
        } catch (StoreException e) {
            return null;
        }
    }
}
