package com.indeed.proctor.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.indeed.util.varexport.ManagedVariable;
import com.indeed.util.varexport.VarExporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.FunctionMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JsonProctorLoaderFactory {
    // Lenient parser used by consumer apps to prevent deployment order dependencies
    private static final Logger LOGGER = LogManager.getLogger(JsonProctorLoaderFactory.class);
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    // Prefix of export variable name for specifications.
    private static final String EXPORT_NAME_PREFIX_FOR_SPECIFICATION = "specification";

    protected static final VarExporter VAR_EXPORTER = VarExporter.forNamespace(JsonProctorLoaderFactory.class.getSimpleName());

    @Nullable
    protected String classResourcePath;
    @Nullable
    protected String filePath;
    @Nullable
    protected ProctorSpecification _specification;

    protected FunctionMapper functionMapper = RuleEvaluator.FUNCTION_MAPPER;

    protected List<ProctorLoadReporter> reporters = new ArrayList<>();

    @SuppressWarnings("UnusedDeclaration")
    public void setClassResourcePath(@Nullable final String classResourcePath) {
        this.classResourcePath = classResourcePath;
    }

    public void setFilePath(@Nullable final String filePath) {
        this.filePath = filePath;
    }

    public void setSpecificationResource(@Nonnull final Resource specificationResource) {
        try (InputStream stream = specificationResource.getInputStream()) {
            this._specification = OBJECT_MAPPER.readValue(stream, ProctorSpecification.class);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Unable to read proctor specification from " + specificationResource, e);
        }

        exportJsonSpecification(
                generateExportVariableNameFromResource(specificationResource),
                this._specification
        );
    }

    public void setSpecificationResource(@Nonnull final String specificationLocation) {
        if (specificationLocation.startsWith("classpath:")) {
            final String specificationClasspath = specificationLocation.replace("classpath:", "");
            setSpecificationResource(new ClassPathResource(specificationClasspath, this.getClass()));
        } else {
            setSpecificationResource(new FileSystemResource(specificationLocation));
        }
    }

    /**
     * setSpecificationResource() is likely more convenient to use instead of this method.
     */
    public void setSpecification(@Nonnull final ProctorSpecification specification) {
        this._specification = Objects.requireNonNull(
                specification,
                "Null specifications are not supported"
        );

        exportJsonSpecification(
                generateExportVariableNameFromObject(specification),
                specification
        );
    }

    public void setFunctionMapper(@Nonnull final FunctionMapper functionMapper) {
        this.functionMapper = functionMapper;
    }

    @Nonnull
    public AbstractJsonProctorLoader getLoader() {
        if ((classResourcePath == null) == (filePath == null)) {
            throw new IllegalStateException("Must have exactly one of classResourcePath or filePath");
        }

        final ProctorSpecification specification = Preconditions.checkNotNull(
                this._specification,
                "Missing specification"
        );

        if (classResourcePath != null) {
            return new ClasspathProctorLoader(specification, classResourcePath, functionMapper);
        }

        final AbstractJsonProctorLoader loader = new FileProctorLoader(specification, filePath, functionMapper);
        loader.addLoadReporter(reporters);
        return loader;
    }

    /**
     * @param diffReporter a reporter to report changes of new proctor
     * @deprecated use {@link JsonProctorLoaderFactory#setLoadReporters} instead
     */
    @Deprecated
    public void setDiffReporter(final AbstractProctorDiffReporter diffReporter) {
        Preconditions.checkNotNull(diffReporter, "diff reporter can't be null use AbstractProctorDiffReporter for nop implementation");
        setLoadReporters(ImmutableList.of(diffReporter));
    }

    public void setLoadReporters(final List<ProctorLoadReporter> reporters) {
        this.reporters = reporters;
    }

    private void exportJsonSpecification(
            final String variableName,
            final ProctorSpecification specification
    ) {
        try {
            final ManagedVariable<String> managedVariable =
                    ManagedVariable.<String>builder()
                            .setName(variableName)
                            .setValue(OBJECT_MAPPER.writeValueAsString(specification))
                            .build();
            VAR_EXPORTER.export(managedVariable);
        } catch (final JsonProcessingException e) {
            LOGGER.warn("Failed to expose json specification in VarExporter.", e);
        }
    }

    private static String generateExportVariableNameFromResource(final Resource resource) {
        return EXPORT_NAME_PREFIX_FOR_SPECIFICATION + "-" + resource.getFilename();
    }

    private static String generateExportVariableNameFromObject(final ProctorSpecification specification) {
        return EXPORT_NAME_PREFIX_FOR_SPECIFICATION + "-anonymous-" + Integer.toHexString(specification.hashCode());
    }
}
