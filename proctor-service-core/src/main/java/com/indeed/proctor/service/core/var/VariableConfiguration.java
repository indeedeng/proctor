package com.indeed.proctor.service.core.var;

import com.indeed.proctor.service.core.config.JsonContextVarConfig;
import com.indeed.proctor.service.core.config.JsonServiceConfig;
import com.indeed.proctor.service.core.config.JsonVarConfig;

import java.util.Map;

/** @author parker */
public final class VariableConfiguration {

    private final Extractor extractor;

    private final Converter converter;

    private final JsonServiceConfig jsonConfig;

    public VariableConfiguration(final Extractor extractor,
                                 final Converter converter) {
        this.extractor = extractor;
        this.converter = converter;

        final Map<String, JsonContextVarConfig> context = extractor.toContextJson();

        final Map<String, JsonVarConfig> identifiers = extractor.toIdentifierJson();
        final JsonServiceConfig config = new JsonServiceConfig();
        config.setContext(context);
        config.setIdentifiers(identifiers);

        this.jsonConfig = config;
    }

    public Extractor getExtractor() {
        return extractor;
    }

    public Converter getConverter() {
        return converter;
    }

    public JsonServiceConfig getJsonConfig() {
        return jsonConfig;
    }
}
