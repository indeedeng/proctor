package com.indeed.proctor.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.util.core.DataLoadingTimerTask;
import com.indeed.util.varexport.Export;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.FunctionMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractProctorLoader extends DataLoadingTimerTask implements Supplier<Proctor> {
    private static final Logger LOGGER = Logger.getLogger(AbstractProctorLoader.class);

    @Nullable
    protected final Map<String, TestSpecification> requiredTests;
    @Nullable
    private Proctor current = null;
    @Nullable
    private Audit lastAudit = null;
    @Nullable
    private String lastLoadErrorMessage= "load never attempted";
    @Nonnull
    private final FunctionMapper functionMapper;
    private final ProvidedContext providedContext;

    @Nullable
    private AbstractProctorDiffReporter diffReporter = new AbstractProctorDiffReporter();

    public AbstractProctorLoader(@Nonnull final Class<?> cls, @Nonnull final ProctorSpecification specification, @Nonnull final FunctionMapper functionMapper) {
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

            return false;
        }

        if (this.current != null && newProctor != null) {
            this.diffReporter.reportProctorDiff(this.current.getArtifact(), newProctor.getArtifact());
        }

        this.current = newProctor;

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
            loadResult = ProctorUtils.verifyAndConsolidate(testMatrix, getSource(), requiredTests, functionMapper, providedContext);
        }

        final Audit newAudit = testMatrix.getAudit();
        if (lastAudit != null) {
            final Audit audit = Preconditions.checkNotNull(newAudit, "Missing audit");
            if(lastAudit.getVersion().equals(audit.getVersion())) {
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

    // this is used to provide custom reporting of changes in the tests, e.g. reporting to datadog
    @SuppressWarnings({"UnusedDeclaration"})
    public void setDiffReporter(@Nonnull final AbstractProctorDiffReporter diffReporter) {

        if (diffReporter == null) {
            throw new UnsupportedOperationException("diffReporter can't be null, use AbstractProctorDiffReporter for nop implementation");
        }

        this.diffReporter = diffReporter;
    }
}
