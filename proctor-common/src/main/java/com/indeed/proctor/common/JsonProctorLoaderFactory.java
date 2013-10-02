package com.indeed.proctor.common;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.FunctionMapper;
import java.io.FileInputStream;
import java.io.IOException;


public class JsonProctorLoaderFactory {
    // Lenient parser used by consumer apps to prevent deployment order depenencies
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    @Nullable
    private String classResourcePath;
    @Nullable
    private String filePath;
    @Nullable
    private ProctorSpecification _specification;

    private FunctionMapper functionMapper = RuleEvaluator.FUNCTION_MAPPER;

    @SuppressWarnings("UnusedDeclaration")
    public void setClassResourcePath(@Nullable final String classResourcePath) {
        this.classResourcePath = classResourcePath;
    }

    public void setFilePath(@Nullable final String filePath) {
        this.filePath = filePath;
    }

    public void setSpecificationResource(@Nonnull final String specificationResource) {
        try {
            if (specificationResource.startsWith("classpath:")) {
                String specificationResourceClasspath= specificationResource.replace("classpath:", "");
                this._specification = OBJECT_MAPPER.readValue(this.getClass().getResourceAsStream(specificationResourceClasspath), ProctorSpecification.class);
            }
            else {
                FileInputStream fis = new FileInputStream(specificationResource);
                this._specification = OBJECT_MAPPER.readValue(fis, ProctorSpecification.class);
                fis.close();
            }

        } catch (@Nonnull final JsonParseException e) {
            throw new IllegalArgumentException("Unable to read proctor specification from " + specificationResource, e);
        } catch (@Nonnull final JsonMappingException e) {
            throw new IllegalArgumentException("Unable to read proctor specification from " + specificationResource, e);
        } catch (@Nonnull final IOException e) {
            throw new IllegalArgumentException("Unable to read proctor specification from " + specificationResource, e);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @VisibleForTesting
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
        return new FileProctorLoader(specification, filePath, functionMapper);
    }
}
