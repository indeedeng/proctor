package com.indeed.proctor.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.util.core.DataLoadingTimerTask;
import com.indeed.util.varexport.Export;
import com.indeed.util.varexport.ManagedVariable;
import com.indeed.util.varexport.VarExporter;
import org.apache.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.FunctionMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractProctorLoader extends DataLoadingTimerTask implements Supplier<Proctor> {
    private static final Logger LOGGER = Logger.getLogger(AbstractProctorLoader.class);
    protected static final VarExporter VAR_EXPORTER = VarExporter
            .forNamespace(AbstractProctorLoader.class.getSimpleName())
            .includeInGlobal();

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
    private final DynamicFilters dynamicFilters;

    private final List<ProctorLoadReporter> reporters = new ArrayList<>();

    /**
     * @param cls name will be used as namespace for timer
     * @param specification provides tests, context, dynamic filters
     * @param functionMapper evaluates functions in allocation rules
     */
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
        this.dynamicFilters = specification.getDynamicFilters();
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

    @CheckForNull
    abstract TestMatrixArtifact loadTestMatrix() throws IOException, MissingTestMatrixException;

    /**
     * @return informative String for log/error messages
     */
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

            reportNoChange();
            return true; // mark this cycle as success so that healthcheck recovers
        }

        reportReloaded(current, newProctor);

        current = newProctor;

        final Audit lastAudit = Preconditions.checkNotNull(this.lastAudit, "Missing last audit");
        setDataVersion(lastAudit.getVersion() + " @ " + lastAudit.getUpdated() + " by " + lastAudit.getUpdatedBy());
        LOGGER.info("Successfully loaded new test matrix definition: " + lastAudit.getVersion() + " @ " + lastAudit.getUpdated() + " by " + lastAudit.getUpdatedBy());
        return true;
    }

    @CheckForNull
    public Proctor doLoad() throws IOException, MissingTestMatrixException {
        final TestMatrixArtifact testMatrix = loadTestMatrix();
        if (testMatrix == null) {
            throw new MissingTestMatrixException("Failed to load Test Matrix from " + getSource());
        }

        final Set<String> dynamicTests = dynamicFilters.determineTests(
                testMatrix.getTests(),
                requiredTests.keySet()
        );
        exportDynamicTests(dynamicTests);
        final ProctorLoadResult loadResult = ProctorUtils.verifyAndConsolidate(
                testMatrix,
                getSource(),
                requiredTests,
                functionMapper,
                providedContext,
                dynamicTests
        );

        loadResult.getTestErrorMap().forEach((testName, exception) -> {
            LOGGER.error(String.format("Unable to load test matrix for a required test %s", testName), exception);
        });
        loadResult.getMissingTests().forEach((testName) ->
            LOGGER.error(String.format("A required test %s is missing from test matrix", testName))
        );
        loadResult.getDynamicTestErrorMap().forEach((testName, exception) -> {
            // Intentionally not adding stack trace to log, to reduce log size,
            // message contains all the information that is valuable.
            LOGGER.warn(String.format(
                    "Unable to load test matrix for a dynamic test %s. Cause: %s Message: %s",
                    testName,
                    exception.getCause(),
                    exception.getMessage()
            ));
        });

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
        setLastAudit(newAudit);
        return proctor;
    }

    @CheckForNull
    public Proctor get() {
        return current;
    }

    @CheckForNull
    @Export(name = "last-audit")
    public Audit getLastAudit() {
        return lastAudit;
    }

    @VisibleForTesting
    void setLastAudit(final Audit newAudit) {
        lastAudit = newAudit;
    }

    @CheckForNull
    @Export(name = "last-error", doc = "The last error message thrown by the loader. null indicates a successful load.")
    public String getLastLoadErrorMessage() {
        return lastLoadErrorMessage;
    }

    // this can be used in subclasses for healthchecks
    /**
     * @return true if there was a success and it came after the last error
     */
    public boolean isLoadedDataSuccessfullyRecently() {
        // most dataLoadTimer methods already exposed in parent class DataLoadingTimerTask
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

    protected void exportDynamicTests(final Set<String> dynamicTests) {
        final ManagedVariable<Set<String>> managedVariable =
                ManagedVariable.<Set<String>>builder()
                        .setName("dynamic-tests")
                        .setValue(dynamicTests)
                        .build();
        VAR_EXPORTER.export(managedVariable);
    }
}
