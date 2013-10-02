package com.indeed.proctor.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.indeed.util.core.DataLoadingTimerTask;
import com.indeed.util.varexport.Export;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.FunctionMapper;
import java.io.IOException;
import java.util.Map;

public abstract class AbstractProctorLoader extends DataLoadingTimerTask implements Supplier<Proctor> {
    private static final Logger LOGGER = Logger.getLogger(AbstractProctorLoader.class);

    @Nonnull
    protected final Map<String, TestSpecification> requiredTests;
    @Nullable
    private Proctor current = null;
    @Nullable
    private Audit lastAudit = null;
    @Nullable
    private String lastLoadErrorMessage= "load never attempted";
    @Nonnull
    private final FunctionMapper functionMapper;

    public AbstractProctorLoader(@Nonnull final Class<?> cls, @Nonnull final ProctorSpecification specification, @Nonnull final FunctionMapper functionMapper) {
        super(cls.getSimpleName());
        this.requiredTests = specification.getTests();
        this.functionMapper = functionMapper;
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
            return false;
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

        final ProctorLoadResult loadResult = ProctorUtils.verifyAndConsolidate(testMatrix, getSource(), requiredTests, functionMapper);
        final Audit newAudit = testMatrix.getAudit();
        if (lastAudit != null) {
            final Audit audit = Preconditions.checkNotNull(newAudit, "Missing audit");
            if(lastAudit.getVersion() == audit.getVersion()) {
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
}
