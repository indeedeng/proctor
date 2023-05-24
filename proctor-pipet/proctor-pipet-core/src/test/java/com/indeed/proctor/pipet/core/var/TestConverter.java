package com.indeed.proctor.pipet.core.var;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.pipet.core.web.BadRequestException;
import com.indeed.proctor.pipet.core.web.InternalServerException;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** @author parker */
public class TestConverter {
    private static final String DEFAULT_COUNTRY = "US";

    @Test
    public void testConvertContextVariables() {
        final Converter converter = getBasicConverter();

        final Map<String, String> rawVariables =
                ImmutableMap.of(
                        "loggedin", "1",
                        "char", "B");

        final RawParameters rawParameters =
                new RawParameters(
                        rawVariables,
                        Collections.<TestType, String>emptyMap(),
                        Collections.<String>emptyList(),
                        "");

        final ConvertedParameters convertedParameters = converter.convert(rawParameters);
        assertEquals(Boolean.TRUE, convertedParameters.getContext().get("loggedin"));
        assertEquals('B', convertedParameters.getContext().get("char"));
        assertEquals(0, convertedParameters.getTest().size());
        assertEquals(0, convertedParameters.getForceGroups().size());
    }

    @Test(expected = BadRequestException.class)
    public void testConvertContextVariables_ConversionError() {
        final Converter converter = getBasicConverter();

        final Map<String, String> rawVariables =
                ImmutableMap.of(
                        "loggedin", "1",
                        "char", "ABC");

        final RawParameters rawParameters =
                new RawParameters(
                        rawVariables,
                        Collections.<TestType, String>emptyMap(),
                        Collections.<String>emptyList(),
                        "");

        final ConvertedParameters convertedParameters = converter.convert(rawParameters);
    }

    @Test(expected = InternalServerException.class)
    public void testConvertContextVariables_MissingVariable() {
        final Converter converter = getBasicConverter();

        final Map<String, String> rawVariables = ImmutableMap.of("wrong-variable", "ABC");

        final RawParameters rawParameters =
                new RawParameters(
                        rawVariables,
                        Collections.<TestType, String>emptyMap(),
                        Collections.<String>emptyList(),
                        "");

        final ConvertedParameters convertedParameters = converter.convert(rawParameters);
    }

    @Test
    public void testConvertIdentifiers() {
        final Converter converter = new Converter(Collections.<ContextVariable>emptyList());

        final Map<TestType, String> rawIds =
                ImmutableMap.of(
                        TestType.ANONYMOUS_USER, "user-123",
                        TestType.EMAIL_ADDRESS, "foo@example.com");

        final RawParameters rawParameters =
                new RawParameters(
                        Collections.<String, String>emptyMap(),
                        rawIds,
                        Collections.<String>emptyList(),
                        "");

        final ConvertedParameters convertedParameters = converter.convert(rawParameters);
        final Identifiers ids = convertedParameters.getIdentifiers();
        assertEquals("user-123", ids.getIdentifier(TestType.ANONYMOUS_USER));
        assertEquals("foo@example.com", ids.getIdentifier(TestType.EMAIL_ADDRESS));
        assertTrue(ids.isRandomEnabled());
    }

    @Test
    public void testConvertForceGroups() {
        final Converter converter = new Converter(Collections.<ContextVariable>emptyList());

        final RawParameters rawParameters =
                new RawParameters(
                        Collections.<String, String>emptyMap(),
                        Collections.<TestType, String>emptyMap(),
                        Collections.<String>emptyList(),
                        "mytest1,othertest2");

        final ConvertedParameters convertedParameters = converter.convert(rawParameters);

        final Map<String, Integer> forceGroups = convertedParameters.getForceGroups();
        assertEquals(2, forceGroups.size());
        assertEquals(Integer.valueOf(1), forceGroups.get("mytest"));
        assertEquals(Integer.valueOf(2), forceGroups.get("othertest"));
    }

    private Converter getBasicConverter() {
        // default country
        final ContextVariable country =
                ContextVariable.newBuilder()
                        .setVarName("char")
                        .setDefaultValue("A")
                        .setConverter(ValueConverters.characterValueConverter())
                        .build();
        // no default langauge
        final ContextVariable language =
                ContextVariable.newBuilder()
                        .setVarName("loggedin")
                        .setConverter(ValueConverters.booleanValueConverter())
                        .build();
        return new Converter(ImmutableList.of(country, language));
    }
}
