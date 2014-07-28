package com.indeed.proctor.service.core.config;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.service.core.var.ContextVariable;
import com.indeed.proctor.service.core.var.Converter;
import com.indeed.proctor.service.core.var.Extractor;
import com.indeed.proctor.service.core.var.Identifier;
import com.indeed.proctor.service.core.var.ValueConverter;
import com.indeed.proctor.service.core.var.ValueConverters;
import com.indeed.proctor.service.core.var.VariableConfiguration;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/** @author parker */
public class VariableConfigurationJsonParser {

    private final ConcurrentMap<String, ValueConverter> registeredValueConverters = Maps.newConcurrentMap();

    private final ObjectMapper deserializer = Serializers.strict();

    // additional identifiers to use if those from the JsonServiceConfig are not available
    final List<Identifier> additionalIdentifiers = Collections.synchronizedList(Lists.<Identifier>newArrayList());

    // additional identifiers to use from the JsonServiceConfig
    final List<ContextVariable> additionalVariables = Collections.synchronizedList(Lists.<ContextVariable>newArrayList());


    private VariableConfigurationJsonParser() {
        registerStandardConverters();
    }

    public static VariableConfigurationJsonParser newParser() {
        return new VariableConfigurationJsonParser();
    }

    public VariableConfiguration buildFrom(final InputStream input) throws IOException {
        final JsonServiceConfig config = deserializer.readValue(input, JsonServiceConfig.class);
        return buildFrom(config);
    }

    public VariableConfiguration buildFrom(final Resource input) throws IOException {
        return buildFrom(input.getURL());
    }

    public VariableConfiguration buildFrom(final URL input) throws IOException {
        final JsonServiceConfig config = deserializer.readValue(input, JsonServiceConfig.class);
        return buildFrom(config);
    }

    public VariableConfiguration buildFrom(final JsonServiceConfig config) {
        // additionalVariables are added in createContextList
        final List<ContextVariable> contextVariableList = createContextList(config);
        // additionalIdentifiers are added in createIdentifierList
        final List<Identifier> identifiers = createIdentifierList(config);
        final Extractor extractor = new Extractor(contextVariableList, identifiers);
        final Converter converter = new Converter(contextVariableList);
        return new VariableConfiguration(extractor, converter);
    }

    public VariableConfiguration build() {
        final List<ContextVariable> contextVariableList = ImmutableList.copyOf(additionalVariables);
        final List<Identifier> identifiers = ImmutableList.copyOf(additionalIdentifiers);
        final Extractor extractor = new Extractor(contextVariableList, identifiers);
        final Converter converter = new Converter(contextVariableList);
        return new VariableConfiguration(extractor, converter);
    }

    private List<ContextVariable> createContextList(final JsonServiceConfig jsonServiceConfig) {
        final ImmutableList.Builder<ContextVariable> contextList = ImmutableList.builder();
        for (Map.Entry<String, JsonContextVarConfig> e : jsonServiceConfig.getContext().entrySet()) {
            final JsonContextVarConfig config = e.getValue();
            contextList.add(ContextVariable.newBuilder()
                                .setVarName(e.getKey())
                                .setSource(config.getSource())
                                .setSourceKey(config.getSourceKey())
                                .setDefaultValue(config.getDefaultValue())
                                .setConverter(lookupConverter(config.getType()))
                                .build());
        }
        contextList.addAll(additionalVariables);
        return contextList.build();
    }

    private List<Identifier> createIdentifierList(final JsonServiceConfig jsonServiceConfig) {
        final ImmutableList.Builder<Identifier> identifierList = ImmutableList.builder();
        for (Map.Entry<String, JsonVarConfig> e : jsonServiceConfig.getIdentifiers().entrySet()) {
            final JsonVarConfig config = e.getValue();
            identifierList.add(Identifier.newBuilder()
                                   .setVarName(e.getKey())
                                   .setSource(config.getSource())
                                   .setSourceKey(config.getSourceKey())
                                   .build());
        }
        identifierList.addAll(additionalIdentifiers);
        return identifierList.build();
    }

    public VariableConfigurationJsonParser clearRegisteredConverters() {
        registeredValueConverters.clear();
        return this;
    }

    public VariableConfigurationJsonParser clearAdditionalIdentifiers() {
        additionalIdentifiers.clear();
        return this;
    }

    public VariableConfigurationJsonParser clearAdditionalVariables() {
        additionalVariables.clear();
        return this;
    }

    public VariableConfigurationJsonParser clearAll() {
        clearRegisteredConverters();
        clearAdditionalVariables();
        clearAdditionalIdentifiers();
        return this;
    }


    public VariableConfigurationJsonParser registerStandardConverters() {
        // Primitives
        registerValueConverterByType("byte", ValueConverters.byteValueConverter());
        registerValueConverterByType("Byte", ValueConverters.byteValueConverter());
        registerValueConverterByCanonicalName(ValueConverters.byteValueConverter());

        registerValueConverterByType("short", ValueConverters.shortValueConverter());
        registerValueConverterByType("Short", ValueConverters.shortValueConverter());
        registerValueConverterByCanonicalName(ValueConverters.shortValueConverter());

        registerValueConverterByType("int", ValueConverters.integerValueConverter());
        registerValueConverterByType("Integer", ValueConverters.integerValueConverter());
        registerValueConverterByCanonicalName(ValueConverters.integerValueConverter());

        registerValueConverterByType("long", ValueConverters.longValueConverter());
        registerValueConverterByType("Long", ValueConverters.longValueConverter());
        registerValueConverterByCanonicalName(ValueConverters.longValueConverter());

        registerValueConverterByType("float", ValueConverters.floatValueConverter());
        registerValueConverterByType("Float", ValueConverters.floatValueConverter());
        registerValueConverterByCanonicalName(ValueConverters.floatValueConverter());

        registerValueConverterByType("double", ValueConverters.doubleValueConverter());
        registerValueConverterByType("Double", ValueConverters.doubleValueConverter());
        registerValueConverterByCanonicalName(ValueConverters.doubleValueConverter());

        registerValueConverterByType("boolean", ValueConverters.booleanValueConverter());
        registerValueConverterByType("Boolean", ValueConverters.booleanValueConverter());
        registerValueConverterByCanonicalName(ValueConverters.booleanValueConverter());

        registerValueConverterByType("char", ValueConverters.characterValueConverter());
        registerValueConverterByType("Character", ValueConverters.characterValueConverter());
        registerValueConverterByCanonicalName(ValueConverters.characterValueConverter());

        registerValueConverterByType("string", ValueConverters.stringValueConverter());
        registerValueConverterByType("String", ValueConverters.stringValueConverter());
        registerValueConverterByCanonicalName(ValueConverters.stringValueConverter());

        return this;
    }

    public VariableConfigurationJsonParser registerValueConverterByType(final String type, final ValueConverter converter) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(type), "Type must not be empty or null");
        Preconditions.checkNotNull(converter, "ValueConverter cannot be null");
        registeredValueConverters.put(type, converter);
        return this;
    }

    public VariableConfigurationJsonParser registerValueConverterByCanonicalName(final ValueConverter converter) {
        return registerValueConverterByType(converter.getType().getCanonicalName(), converter);
    }

    public VariableConfigurationJsonParser registerValueConverterBySimpleName(final ValueConverter converter) {
        return registerValueConverterByType(converter.getType().getSimpleName(), converter);
    }

    public VariableConfigurationJsonParser registerValueConverterByName(final ValueConverter converter) {
        return registerValueConverterByType(converter.getType().getName(), converter);
    }

    @VisibleForTesting
    ValueConverter lookupConverter(final String type) {
        final ValueConverter converter = registeredValueConverters.get(type);
        if (converter == null) {
            // Unrecognized type name. You should add any custom converters here and as an implementation of ValueConverter.
            throw new ConfigurationException(
                String.format("Type '%s' unrecognized. VariableConfigurationJsonParser missing converter for this type.", type));
        }
        return converter;
    }

    public VariableConfigurationJsonParser addIdentifier(final ExtractorSource extractorSource, final TestType... testTypes) {
        for (final TestType testType : testTypes) {
            additionalIdentifiers.add(Identifier.forTestType(extractorSource, testType));
        }
        return this;
    }

    public VariableConfigurationJsonParser addIdentifier(final Identifier identifier) {
        additionalIdentifiers.add(identifier);
        return this;
    }

    public VariableConfigurationJsonParser addContextVariable(final ContextVariable variable) {
        additionalVariables.add(variable);
        return this;
    }
}
