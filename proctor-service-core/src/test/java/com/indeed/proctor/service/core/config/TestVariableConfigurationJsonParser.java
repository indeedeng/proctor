package com.indeed.proctor.service.core.config;

import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.service.core.var.ValueConverter;
import com.indeed.proctor.service.core.var.ValueConverters;
import com.indeed.proctor.service.core.var.VariableConfiguration;
import org.junit.Assert;
import org.junit.Test;

/** @author parker */
public class TestVariableConfigurationJsonParser {

    @Test
    public void testStandardValueConverters() {
        final VariableConfigurationJsonParser parser = VariableConfigurationJsonParser.newParser();

        doCheckConverterClass(parser, "byte", ValueConverters.byteValueConverter().getClass());
        doCheckConverterClass(parser, "Byte", ValueConverters.byteValueConverter().getClass());
        doCheckConverterClass(parser, "java.lang.Byte", ValueConverters.byteValueConverter().getClass());

        doCheckConverterClass(parser, "short", ValueConverters.shortValueConverter().getClass());
        doCheckConverterClass(parser, "Short", ValueConverters.shortValueConverter().getClass());
        doCheckConverterClass(parser, "java.lang.Short", ValueConverters.shortValueConverter().getClass());

        doCheckConverterClass(parser, "int", ValueConverters.integerValueConverter().getClass());
        doCheckConverterClass(parser, "Integer", ValueConverters.integerValueConverter().getClass());
        doCheckConverterClass(parser, "java.lang.Integer", ValueConverters.integerValueConverter().getClass());

        doCheckConverterClass(parser, "long", ValueConverters.longValueConverter().getClass());
        doCheckConverterClass(parser, "Long", ValueConverters.longValueConverter().getClass());
        doCheckConverterClass(parser, "java.lang.Long", ValueConverters.longValueConverter().getClass());

        doCheckConverterClass(parser, "float", ValueConverters.floatValueConverter().getClass());
        doCheckConverterClass(parser, "Float", ValueConverters.floatValueConverter().getClass());
        doCheckConverterClass(parser, "java.lang.Float", ValueConverters.floatValueConverter().getClass());

        doCheckConverterClass(parser, "double", ValueConverters.doubleValueConverter().getClass());
        doCheckConverterClass(parser, "Double", ValueConverters.doubleValueConverter().getClass());
        doCheckConverterClass(parser, "java.lang.Double", ValueConverters.doubleValueConverter().getClass());

        doCheckConverterClass(parser, "char", ValueConverters.characterValueConverter().getClass());
        doCheckConverterClass(parser, "Character", ValueConverters.characterValueConverter().getClass());
        doCheckConverterClass(parser, "java.lang.Character", ValueConverters.characterValueConverter().getClass());

        doCheckConverterClass(parser, "string", ValueConverters.stringValueConverter().getClass());
        doCheckConverterClass(parser, "String", ValueConverters.stringValueConverter().getClass());
        doCheckConverterClass(parser, "java.lang.String", ValueConverters.stringValueConverter().getClass());
    }

    @Test
    public void testUnknownTypesThrowError() {
        final VariableConfigurationJsonParser parser = VariableConfigurationJsonParser.newParser();
        // first check for a registered type
        doCheckConverterClass(parser, "double", ValueConverters.doubleValueConverter().getClass());
        parser.clearAll();
        try {
            parser.lookupConverter("double");
            Assert.fail("Lookup Converter should throw ConfigurationException for unknown types");
        } catch (ConfigurationException e) {
            // ignored This should be thrown
        }
    }

    @Test
    public void testRegisterCustomConverter() {
        final VariableConfigurationJsonParser parser = VariableConfigurationJsonParser.newParser();
        parser.clearAll();

        final ValueConverter<String> converter = ValueConverters.stringValueConverter();

        parser.registerValueConverterByType("custom", converter);
        doCheckConverterClass(parser, "custom", converter);

        parser.clearAll();
        // should use "String" (the class Name of the Type)
        parser.registerValueConverterByName(converter);
        doCheckConverterClass(parser, "java.lang.String", converter);

        parser.clearAll();
        parser.registerValueConverterBySimpleName(converter);
        doCheckConverterClass(parser, "String", converter);

        parser.clearAll();
        parser.registerValueConverterByCanonicalName(converter);
        doCheckConverterClass(parser, "java.lang.String", converter);
    }

    @Test
    public void testBuildEmptyConfiguration() {
        // TODO (parker) 7/3/14 - should VariableConfigurationJsonParser blow up if no identifiers or context variables are present.
        // No identifiers isn't particularly useful
        final VariableConfigurationJsonParser parser = VariableConfigurationJsonParser.newParser();
        final VariableConfiguration configuration = parser.build();
        Assert.assertEquals(0, configuration.getJsonConfig().getContext().size());
        Assert.assertEquals(0, configuration.getJsonConfig().getIdentifiers().size());
    }

    @Test
    public void testBuildWithAdditionalIdentifiers() {
        final VariableConfigurationJsonParser parser = VariableConfigurationJsonParser.newParser();
        parser.addIdentifier(ExtractorSource.QUERY, TestType.ANONYMOUS_USER);
        final VariableConfiguration configuration = parser.build();
        Assert.assertEquals(0, configuration.getJsonConfig().getContext().size());
        Assert.assertEquals(1, configuration.getJsonConfig().getIdentifiers().size());

        final JsonVarConfig identifier = configuration.getJsonConfig().getIdentifiers().get("user");
        Assert.assertNotNull(identifier);
        Assert.assertEquals(ExtractorSource.QUERY, identifier.getSource());
        Assert.assertEquals("id.user", identifier.getSourceKey());
    }

    private <T extends ValueConverter> void doCheckConverterClass(
        final VariableConfigurationJsonParser parser,
        final String type,
        final Class<T> clazz) {
        final ValueConverter converter = parser.lookupConverter(type);
        Assert.assertNotNull("ValueConverter for " + type + " must not be null", converter);
        Assert.assertEquals(clazz, converter.getClass());
    }

    private void doCheckConverterClass(
        final VariableConfigurationJsonParser parser,
        final String type,
        final ValueConverter instance) {
        final ValueConverter converter = parser.lookupConverter(type);
        Assert.assertNotNull("ValueConverter for " + type + " must not be null", converter);
        Assert.assertEquals(instance, converter);
    }

}
