package com.indeed.proctor.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.dynamic.DynamicFilter;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.util.core.DataLoadingTimerTask;
import com.indeed.util.varexport.Export;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.FunctionMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractProctorLoader extends DataLoadingTimerTask implements Supplier<Proctor> {
    private static final Logger LOGGER = Logger.getLogger(AbstractProctorLoader.class);

    @Nullable
    protected final Map<String, TestSpecification> requiredTests;
    @Nullable
    private Proctor current = null;
    @Nullable
    private Audit lastAudit = null;
    @Nullable
    private String lastLoadErrorMessage = "load never attempted";
    @Nonnull
    private final FunctionMapper functionMapper;
    private final ProvidedContext providedContext;

    private final List<ProctorLoadReporter> reporters = new ArrayList<>();
    private DynamicFilters dynamicFilters = new DynamicFilters();

    public AbstractProctorLoader(
            @Nonnull final Class<?> cls,
            @Nonnull final ProctorSpecification specification,
            @Nonnull final FunctionMapper functionMapper
    ) {
        super(cls.getSimpleName());
        this.requiredTests = specification.getTests();
        this.providedContext = createProvidedContext(specification);
        if (!this.providedContext.shouldEvaluate()) {
            LOGGER.debug("providedContext Objects missing necessary functions for validation, rules will not be tested.");
        }
        this.functionMapper = functionMapper;
    }

    /**
     * user can override this function to provide a context for rule verification
     *
     * @return a context for rule verification
     **/
    protected Map<String, Object> getRuleVerificationContext() {
        return Collections.<String, Object>emptyMap();
    }

    protected ProvidedContext createProvidedContext(final ProctorSpecification specification) {
        return ProctorUtils.convertContextToTestableMap(specification.getProvidedContext(), getRuleVerificationContext());
    }

    @Nullable
    abstract TestMatrixArtifact loadTestMatrix() throws IOException, MissingTestMatrixException;

    @Nonnull
    abstract String getSource();

    @Override
    public boolean load() {
        final Proctor newProctor;
        try {
            newProctor = doLoad();
        } catch (@Nonnull final Throwable t) {
            reportFailed(t);
            lastLoadErrorMessage = t.getMessage();
            throw new RuntimeException("Unable to reload proctor from " + getSource(), t);
        }
        lastLoadErrorMessage = null;

        if (newProctor == null) {
            // This should only happen if the versions of the matrix files are the same.

            if (!dataLoadTimer.isLoadedDataSuccessfullyRecently()) {
                // Clear healthcheck dependency status if the last load attempt failed but version has not changed.
                dataLoadTimer.loadComplete();
            }

            reportNoChange();
            return false;
        }

        reportReloaded(current, newProctor);

        current = newProctor;

        final Audit lastAudit = Preconditions.checkNotNull(this.lastAudit, "Missing last audit");
        setDataVersion(lastAudit.getVersion() + " @ " + lastAudit.getUpdated() + " by " + lastAudit.getUpdatedBy());
        LOGGER.info("Successfully loaded new test matrix definition: " + lastAudit.getVersion() + " @ " + lastAudit.getUpdated() + " by " + lastAudit.getUpdatedBy());
        return true;
    }

    @Nullable
    public Proctor doLoad() throws IOException, MissingTestMatrixException {
        final TestMatrixArtifact testMatrix = loadTestMatrix();
        if (testMatrix == null) {
            throw new MissingTestMatrixException("Failed to load Test Matrix from " + getSource());
        }

        final ProctorLoadResult loadResult;
        if (requiredTests == null) {
            // Probably an absent specification.
            loadResult = ProctorUtils.verifyWithoutSpecification(testMatrix, getSource());
        } else {
            final Set<String> dynamicTests = dynamicFilters.determineTests(testMatrix.getTests(), requiredTests.keySet());
            loadResult = ProctorUtils.verifyAndConsolidate(
                    testMatrix,
                    getSource(),
                    requiredTests,
                    functionMapper,
                    providedContext,
                    dynamicTests
            );
        }

        if (!loadResult.getTestErrorMap().isEmpty()) {
            for (final Map.Entry<String, IncompatibleTestMatrixException> errorTest : loadResult.getTestErrorMap().entrySet()) {
                final String testName = errorTest.getKey();
                if (requiredTests.containsKey(testName)) {
                    LOGGER.error(String.format("Unable to load test matrix for %s in a specification", testName), errorTest.getValue());
                } else {
                    LOGGER.warn(String.format("Unable to load test matrix for %s matching dynamic filters", testName), errorTest.getValue());
                }
            }
        }

        final Audit newAudit = testMatrix.getAudit();
        if (lastAudit != null) {
            final Audit audit = Preconditions.checkNotNull(newAudit, "Missing audit");
            if (lastAudit.getVersion().equals(audit.getVersion())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Not reloading " + getSource() + " test matrix definition because audit is unchanged: " + lastAudit.getVersion() + " @ " + lastAudit.getUpdated() + " by " + lastAudit.getUpdatedBy());
                }

                return null;
            }
        }

        final Proctor proctor = Proctor.construct(testMatrix, loadResult, functionMapper);
        //  kind of lame to modify lastAudit here but current in load(), but the interface is a little constraining
        this.lastAudit = newAudit;
        return proctor;
    }

    @Nullable
    public Proctor get() {
        return current;
    }

    @Nullable
    @Export(name = "last-audit")
    public Audit getLastAudit() {
        return lastAudit;
    }

    @Nullable
    @Export(name = "last-error", doc = "The last error message thrown by the loader. null indicates a successful load.")
    public String getLastLoadErrorMessage() {
        return lastLoadErrorMessage;
    }

    // this is used for healthchecks
    @SuppressWarnings({"UnusedDeclaration"})
    public boolean isLoadedDataSuccessfullyRecently() {
        return dataLoadTimer.isLoadedDataSuccessfullyRecently();
    }

    /**
     * this is used to provide custom reporting of changes in the tests, e.g. reporting to datadog
     *
     * @param diffReporter a reporter to report change of new proctor
     * @deprecated use {@link AbstractProctorLoader#addLoadReporter}
     */
    @SuppressWarnings({"UnusedDeclaration"})
    @Deprecated
    public void setDiffReporter(@Nonnull final AbstractProctorDiffReporter diffReporter) {
        addLoadReporter(diffReporter);
    }

    public void addLoadReporter(@Nonnull final ProctorLoadReporter diffReporter) {
        Preconditions.checkNotNull(diffReporter, "ProctorLoadReporter can't be null");
        addLoadReporter(ImmutableList.of(diffReporter));
    }

    public void addLoadReporter(@Nonnull final List<ProctorLoadReporter> newReporters) {
        Preconditions.checkNotNull(newReporters, "new reporters shouldn't be empty");
        reporters.addAll(newReporters);
    }

    public void setDynamicFilters(@Nonnull final Collection<? extends DynamicFilter> dynamicFilters) {
        this.dynamicFilters = new DynamicFilters(dynamicFilters);
    }

    void reportFailed(final Throwable t) {
        for (final ProctorLoadReporter reporter : reporters) {
            reporter.reportFailed(t);
        }
    }

    void reportReloaded(final Proctor oldProctor, final Proctor newProctor) {
        for (final ProctorLoadReporter reporter : reporters) {
            reporter.reportReloaded(oldProctor, newProctor);
        }
    }

    void reportNoChange() {
        for (final ProctorLoadReporter reporter : reporters) {
            reporter.reportNoChange();
        }
    }

}
