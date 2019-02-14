package com.indeed.proctor.pipet.core.var;

import com.indeed.proctor.pipet.core.config.JsonContextVarConfig;
import com.indeed.proctor.pipet.core.config.JsonPipetConfig;
import com.indeed.proctor.pipet.core.config.JsonVarConfig;

import java.util.Map;

/** @author parker */
public final class VariableConfiguration {

    private final Extractor extractor;

    private final Converter converter;

    private final JsonPipetConfig jsonConfig;

    public VariableConfiguration(final Extractor extractor,
                                 final Converter converter) {
        this.extractor = extractor;
        this.converter = converter;

        final Map<String, JsonContextVarConfig> context = extractor.toContextJson();

        final Map<String, JsonVarConfig> identifiers = extractor.toIdentifierJson();
        final JsonPipetConfig config = new JsonPipetConfig();
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

    public JsonPipetConfig getJsonConfig() {
        return jsonConfig;
    }
}
