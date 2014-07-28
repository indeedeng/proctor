package com.indeed.proctor.service.core.var;

import com.google.common.base.Preconditions;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.service.core.config.ExtractorSource;
import com.indeed.proctor.service.core.config.JsonVarConfig;

import java.util.Collection;

/**
 * A thin wrapper implementing PrefixVariable.
 */
public class Identifier extends PrefixVariable {

    private final TestType testType;

    protected Identifier(final String varName,
                         final TestType testType,
                         final ExtractorSource source,
                         final String sourceKey,
                         final ValueExtractor extractor) {
        super(varName, source, sourceKey, extractor);
        this.testType = testType;
    }

    public JsonVarConfig toIdentifierJson() {
        final JsonVarConfig config = new JsonVarConfig();
        config.setSourceKey(getSourceKey());
        config.setSource(getSource());
        return config;
    }

    public TestType getTestType() {
        return testType;
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
        private TestType testType;

        private Builder() {
            setPrefix("id");
        }

        public Builder setTestType(final TestType testType) {
            this.testType = testType;
            setVarName(testType.name().toLowerCase());
            return cast();
        }

        private TestType getTestType() {
            if (testType != null) {
                return testType;
            }

            final Collection<TestType> registered = TestType.all();
            // we cannot use registered.contains(TestType) because the TestType constructor is private
            for (final TestType type : registered) {
                if (type.name().equalsIgnoreCase(getVarName())) {
                    return type;
                }
            }
            return null;
        }

        public Identifier build() {
            final TestType testType = Preconditions.checkNotNull(getTestType(), "TestType must be specified");
            final String varName = Preconditions.checkNotNull(getVarName(), "VarName must be specified");
            final ExtractorSource source = getSource();
            final String sourceKey = computeSourceKey();
            final ValueExtractor valueExtractor = getOrCreateValueExtractor();
            return new Identifier(varName, testType, source, sourceKey, valueExtractor);
        }
    }

    public static Identifier forTestType(final ExtractorSource extractorSource,
                                         final TestType testType) {
        return Identifier.newBuilder()
            .setTestType(testType)
            .setSource(extractorSource)
            .build();
    }

}
