package com.indeed.proctor.service.core.var;

import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.service.core.config.ExtractorSource;
import com.indeed.proctor.service.core.config.JsonVarConfig;

/**
 * A thin wrapper implementing PrefixVariable.
 */
public class Identifier extends PrefixVariable {

    public Identifier(final String varName,
                      final ExtractorSource source,
                      final String sourceKey,
                      final ValueExtractor extractor) {
        super(varName, source, sourceKey, extractor);
    }

    public JsonVarConfig toIdentifierJson() {
        final JsonVarConfig config = new JsonVarConfig();
        config.setSourceKey(identifier.getSourceKey());
        config.setSource(identifier.getSource());
        return config;
    }

    @Override
    public String getDefaultValue() {
        // Identifiers are optional and should identify each user uniquely.
        // It wouldn't make sense to have a default because then all users would be put into the same bucket.
        return null;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends PrefixVariable.Builder<Identifier, Builder> {
        private Builder() {
            setPrefix("id");
        }

        public Identifier build() {
            final String varName = getVarName();
            final ExtractorSource source = getSource();
            final String sourceKey = computeSourceKey();
            final ValueExtractor valueExtractor = getOrCreateValueExtractor();
            return new Identifier(varName, source, sourceKey, valueExtractor);
        }
    }

    public static Identifier forTestType(final ExtractorSource extractorSource,
                                         final TestType testType) {
        return Identifier.newBuilder()
            .setVarName(testType.name().toLowerCase())
            .setSource(extractorSource)
            .build();
    }

}
