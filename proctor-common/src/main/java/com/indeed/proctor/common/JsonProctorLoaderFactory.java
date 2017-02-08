package com.indeed.proctor.common;


import com.google.common.base.Preconditions;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.FunctionMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class JsonProctorLoaderFactory {
    // Lenient parser used by consumer apps to prevent deployment order dependencies
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    @Nullable
    protected String classResourcePath;
    @Nullable
    protected String filePath;
    @Nullable
    protected ProctorSpecification _specification;

    protected FunctionMapper functionMapper = RuleEvaluator.FUNCTION_MAPPER;

    protected AbstractProctorDiffReporter diffReporter = new AbstractProctorDiffReporter();

    @SuppressWarnings("UnusedDeclaration")
    public void setClassResourcePath(@Nullable final String classResourcePath) {
        this.classResourcePath = classResourcePath;
    }

    public void setFilePath(@Nullable final String filePath) {
        this.filePath = filePath;
    }

    public void setSpecificationResource(@Nonnull final Resource specificationResource) {
        try {
            readSpecificationResource(specificationResource.getInputStream(), specificationResource.toString());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read proctor specification from " + specificationResource, e);
        }
    }

    public void setSpecificationResource(@Nonnull final String specificationResource) {
        try {
            if (specificationResource.startsWith("classpath:")) {
                final String specificationResourceClasspath= specificationResource.replace("classpath:", "");
                final InputStream is = this.getClass().getResourceAsStream(specificationResourceClasspath);
                readSpecificationResource(is, specificationResource);
                is.close();

            } else {
                final FileInputStream fis = new FileInputStream(specificationResource);
                readSpecificationResource(fis, specificationResource);
                fis.close();
            }

        } catch (@Nonnull final IOException e) {
            throw new IllegalArgumentException("Unable to read proctor specification from " + specificationResource, e);
        }
    }

    private void readSpecificationResource(@Nonnull final InputStream stream, @Nonnull final String specificationResource) {
        try {
            this._specification = OBJECT_MAPPER.readValue(stream, ProctorSpecification.class);

        } catch (@Nonnull final JsonParseException e) {
            throw new IllegalArgumentException("Unable to read proctor specification from " + specificationResource, e);
        } catch (@Nonnull final JsonMappingException e) {
            throw new IllegalArgumentException("Unable to read proctor specification from " + specificationResource, e);
        } catch (@Nonnull final IOException e) {
            throw new IllegalArgumentException("Unable to read proctor specification from " + specificationResource, e);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    /**
     * setSpecificationResource() is likely more convenient to use instead of this method.
     */
    public void setSpecification(@Nonnull final ProctorSpecification specification) {
        this._specification = Preconditions.checkNotNull(specification, "Null specifications are not supported");
    }

    public void setFunctionMapper(@Nonnull final FunctionMapper functionMapper) {
        this.functionMapper = functionMapper;
    }

    @Nonnull
    public AbstractJsonProctorLoader getLoader() {
        if ((classResourcePath == null) == (filePath == null)) {
            throw new IllegalStateException("Must have exactly one of classResourcePath or filePath");
        }

        final ProctorSpecification specification = Preconditions.checkNotNull(this._specification, "Missing specification");
        if (classResourcePath != null) {
            return new ClasspathProctorLoader(specification, classResourcePath, functionMapper);
        }

        final AbstractJsonProctorLoader loader = new FileProctorLoader(specification, filePath, functionMapper);
        loader.setDiffReporter(this.diffReporter);

        return loader;
    }

    public void setDiffReporter(final AbstractProctorDiffReporter diffReporter) {

        if (diffReporter == null) {
            throw new UnsupportedOperationException("diff reporter can't be null use AbstractProctorDiffReporter for nop implementation");
        }

        this.diffReporter = diffReporter;
    }
}
