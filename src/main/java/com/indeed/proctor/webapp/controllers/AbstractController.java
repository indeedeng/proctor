package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.ui.Model;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author parker
 */
public abstract class AbstractController {
    private static final Logger LOGGER = Logger.getLogger(AbstractController.class);

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
        if (store == null) {
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

    protected String doErrorView(final String error, final Throwable throwable, final int responseCode, final HttpServletResponse response, final Model model) {
        response.setStatus(responseCode);
        if (StringUtils.isNotEmpty(error)) {
            model.addAttribute("error", error);
        }
        if (throwable != null) {
            model.addAttribute("exception", throwable);
        }
        return "error";
    }

    private class BranchOrRevision {
        final String stringValue;
        final Environment branch;

        private BranchOrRevision(final String branchOrRevision) throws StoreException {
            stringValue = branchOrRevision;
            branch = Environment.fromName(stringValue);
        }

        private boolean isBranch() {
            return null != branch;
        }

        @Nullable
        private TestMatrixVersion queryTestMatrixVersion() throws StoreException {
            if (isBranch()) {
                return determineStoreFromEnvironment(branch).getCurrentTestMatrix();
            } else {
                return queryMatrixFromRevision(stringValue);
            }
        }

        @Nullable
        private TestDefinition queryTestDefinition(final String testName) throws StoreException {
            final TestMatrixVersion testMatrixVersion = queryTestMatrixVersion();
            if (testMatrixVersion == null) {
                return null;
            }
            return testMatrixVersion.getTestMatrixDefinition().getTests().get(testName);
        }

        private List<Revision> queryTestDefinitionHistory(final String testName, final int start, final int limit) throws StoreException {
            if (isBranch()) {
                return determineStoreFromEnvironment(branch).getHistory(testName, start, limit);
            } else {
                return queryTestHistoryFromRevision(testName, stringValue, start, limit);
            }
        }

        private TestMatrixVersion queryMatrixFromRevision(final String revision) {
            for (final ProctorStore store : stores.values()) {
                try {
                    final TestMatrixVersion testMatrix = store.getTestMatrix(revision);
                    if (testMatrix != null) {
                        return testMatrix;
                    }
                } catch (final StoreException e) {
                    LOGGER.info(String.format("Failed to find revision %s in %s", revision, store.getName()));
                }
            }
            return null;
        }

        private List<Revision> queryTestHistoryFromRevision(final String testName, final String revision, final int start, final int limit) throws StoreException {
            for (final ProctorStore store : stores.values()) {

                final Map<String, List<Revision>> allHistories = store.getAllHistories();
                if (allHistories.containsKey(testName)) {
                    for (final Revision r : allHistories.get(testName)) {
                        if (revision.equals(r.getRevision())) {
                            LOGGER.debug(String.format("Found revision [%s] in history of test [%s] in store [%s]", revision, testName, store.getName()));
                            return store.getHistory(testName, revision, start, limit);
                        }
                    }
                }
            }
            LOGGER.info(String.format("Can not find revision %s in any of stores", revision));
            return Collections.emptyList();
        }
    }
}
