package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.WebappConfiguration;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

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

    protected List<Revision> queryMatrixHistory(final Environment branch, final int start, final int limit) throws StoreException {
        return determineStoreFromEnvironment(branch).getMatrixHistory(start, limit);
    }

    protected TestMatrixVersion queryMatrixFromBranchOrRevision(final String branchOrRevision) throws StoreException {
        final BranchOrRevision bor = new BranchOrRevision(branchOrRevision);
        return bor.queryTestMatrixVersion();
    }

    protected TestDefinition queryTestDefinition(final String branchOrRevision, final String testName) throws StoreException {
        final BranchOrRevision bor = new BranchOrRevision(branchOrRevision);
        return bor.queryTestDefinition(testName);
    }

    protected List<Revision> queryTestDefinitionHistory(final String branchOrRevision, final String testName, final int start, final int limit) throws StoreException {
        final BranchOrRevision bor = new BranchOrRevision(branchOrRevision);
        return bor.queryTestDefinitionHistory(testName, start, limit);
    }

    @Nullable
    private ProctorStore determineStoreFromRevision(final String revision) throws StoreException {
        for (final ProctorStore store : stores.values()) {
            if (store.getTestMatrix(revision) != null) {
                return store;
            }
        }
        return null;
    }

    private class BranchOrRevision {
        final String stringValue;
        @Nullable
        final Environment branch;
        final ProctorStore store;

        private BranchOrRevision(final String branchOrRevision) throws StoreException {
            stringValue = branchOrRevision;
            branch = Environment.fromName(stringValue);
            if (isBranch()) {
                store = determineStoreFromEnvironment(branch);
            } else {
                store = determineStoreFromRevision(stringValue);
            }

            if (store == null) {
                throw new StoreException("Invalid branch or revision name "+stringValue);
            }
        }

        private boolean isBranch() {
            return null != branch;
        }

        private TestMatrixVersion queryTestMatrixVersion() throws StoreException {
            if (isBranch()) {
                return store.getCurrentTestMatrix();
            } else {
                return store.getTestMatrix(stringValue);
            }
        }

        private TestDefinition queryTestDefinition(final String testName) throws StoreException {
            if (isBranch()) {
                return store.getCurrentTestDefinition(testName);
            } else {
                return store.getTestDefinition(testName, stringValue);
            }
        }

        private List<Revision> queryTestDefinitionHistory(final String testName, final int start, final int limit) throws StoreException {
            if (isBranch()) {
                return store.getHistory(testName, start, limit);
            } else {
                return store.getHistory(testName, stringValue, start, limit);
            }
        }
    }
}
