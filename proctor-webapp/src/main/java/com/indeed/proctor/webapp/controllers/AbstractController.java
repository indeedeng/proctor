package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.RevisionDetails;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.util.TestDefinitionUtil;
import com.indeed.proctor.webapp.views.ProctorView;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ui.Model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** @author parker */
public abstract class AbstractController {
    private static final Logger LOGGER = LogManager.getLogger(AbstractController.class);

    private final WebappConfiguration configuration;
    private final Map<Environment, ProctorStore> stores;
    /**
     * List of stores with Production first, for fastest search lookup. (For git, since all stores
     * use the same repo, it might not have any effect.)
     */
    private final List<ProctorStore> storesList;

    public AbstractController(
            final WebappConfiguration configuration,
            final ProctorStore trunkStore,
            final ProctorStore qaStore,
            final ProctorStore productionStore) {
        this.configuration = configuration;

        stores =
                ImmutableMap.of(
                        Environment.WORKING, trunkStore,
                        Environment.QA, qaStore,
                        Environment.PRODUCTION, productionStore);
        storesList = ImmutableList.of(productionStore, qaStore, trunkStore);
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

    protected Environment determineEnvironmentFromParameter(final String branch) {
        final Environment b = Environment.fromName(branch);
        return b != null ? b : Environment.WORKING;
    }

    protected WebappConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @return a store for QA, WORKING, PRODUCTION
     * @throws IllegalArgumentException if branch is not known
     */
    @Nonnull
    protected ProctorStore determineStoreFromEnvironment(final Environment branch) {
        final ProctorStore result = stores.get(branch);
        if (result == null) {
            throw new IllegalArgumentException("Unknown store for branch " + branch);
        }
        return result;
    }

    protected TestMatrixVersion getCurrentMatrix(final Environment branch) {
        try {
            return determineStoreFromEnvironment(branch).getCurrentTestMatrix();
        } catch (final StoreException e) {
            return null;
        }
    }

    protected RevisionDetails getRevisionDetails(final Environment branch, final String revisionId)
            throws StoreException {
        return determineStoreFromEnvironment(branch).getRevisionDetails(revisionId);
    }

    protected List<Revision> queryMatrixHistory(
            final Environment branch, final int start, final int limit) throws StoreException {
        return determineStoreFromEnvironment(branch).getMatrixHistory(start, limit);
    }

    protected Map<String, List<Revision>> queryHistories(final Environment branch)
            throws StoreException {
        return determineStoreFromEnvironment(branch).getAllHistories();
    }

    protected TestMatrixVersion queryMatrixFromBranchOrRevision(final String branchOrRevision)
            throws StoreException {
        final BranchOrRevisionRef bor = getBranchOrRevision(branchOrRevision);
        return bor.queryTestMatrixVersion();
    }

    protected TestDefinition queryTestDefinition(
            final String branchOrRevision, final String testName) throws StoreException {
        final BranchOrRevisionRef bor = getBranchOrRevision(branchOrRevision);
        return bor.queryTestDefinition(testName);
    }

    protected List<Revision> queryTestDefinitionHistory(
            final String branchOrRevision, final String testName, final int start, final int limit)
            throws StoreException {
        final BranchOrRevisionRef bor = getBranchOrRevision(branchOrRevision);
        return bor.queryTestDefinitionHistory(testName, start, limit);
    }

    protected String doErrorView(
            final String error,
            final Throwable throwable,
            final int responseCode,
            final HttpServletResponse response,
            final Model model) {
        response.setStatus(responseCode);
        if (StringUtils.isNotEmpty(error)) {
            model.addAttribute("error", error);
        }
        if (throwable != null) {
            model.addAttribute("exception", throwable);
        }
        return ProctorView.ERROR.getName();
    }

    /** Abstract Factory method */
    private BranchOrRevisionRef getBranchOrRevision(final String branchOrRevisionName)
            throws StoreException {
        final Environment branch = Environment.fromName(branchOrRevisionName);
        if (branch == null) {
            return new RevisionRef(branchOrRevisionName, storesList);
        } else {
            return new BranchRef(determineStoreFromEnvironment(branch));
        }
    }

    /** Either branch (Trunk, QA, Production), or revision by id */
    private interface BranchOrRevisionRef {
        @Nullable
        TestDefinition queryTestDefinition(final String testName);

        @Nullable
        TestMatrixVersion queryTestMatrixVersion() throws StoreException;

        List<Revision> queryTestDefinitionHistory(
                final String testName, final int start, final int limit) throws StoreException;
    }

    private static class BranchRef implements BranchOrRevisionRef {
        final ProctorStore store;

        private BranchRef(final ProctorStore store) {
            this.store = store;
        }

        @Override
        @Nullable
        public TestDefinition queryTestDefinition(final String testName) {
            return TestDefinitionUtil.getTestDefinition(store, testName);
        }

        @Override
        @Nullable
        public TestMatrixVersion queryTestMatrixVersion() throws StoreException {
            return store.getCurrentTestMatrix();
        }

        @Override
        public List<Revision> queryTestDefinitionHistory(
                final String testName, final int start, final int limit) throws StoreException {
            return store.getHistory(testName, start, limit);
        }
    }

    private static class RevisionRef implements BranchOrRevisionRef {
        final String revisionNumber;
        final List<ProctorStore> stores;

        private RevisionRef(final String revisionNumber, final List<ProctorStore> stores) {
            this.revisionNumber = revisionNumber;
            this.stores = stores;
        }

        @Override
        @Nullable
        public TestDefinition queryTestDefinition(final String testName) {
            for (final ProctorStore store : stores) {
                final TestDefinition test =
                        TestDefinitionUtil.getTestDefinition(store, testName, revisionNumber);
                if (test != null) {
                    return test;
                }
            }
            return null;
        }

        @Override
        @Nullable
        public TestMatrixVersion queryTestMatrixVersion() {
            for (final ProctorStore store : stores) {
                try {
                    final TestMatrixVersion testMatrix = store.getTestMatrix(revisionNumber);
                    if (testMatrix != null) {
                        return testMatrix;
                    }
                } catch (final StoreException e) {
                    LOGGER.info(
                            String.format(
                                    "Failed to find revision %s in %s",
                                    revisionNumber, store.getName()));
                }
            }
            return null;
        }

        @Override
        public List<Revision> queryTestDefinitionHistory(
                final String testName, final int start, final int limit) throws StoreException {
            for (final ProctorStore store : stores) {

                final Map<String, List<Revision>> allHistories = store.getAllHistories();
                for (final Revision r :
                        allHistories.getOrDefault(testName, Collections.emptyList())) {
                    if (revisionNumber.equals(r.getRevision())) {
                        LOGGER.debug(
                                String.format(
                                        "Found revision [%s] in history of test [%s] in store [%s]",
                                        revisionNumber, testName, store.getName()));
                        return store.getHistory(testName, revisionNumber, start, limit);
                    }
                }
            }
            LOGGER.info(String.format("Can not find revision %s in any of stores", revisionNumber));
            return Collections.emptyList();
        }
    }
}
