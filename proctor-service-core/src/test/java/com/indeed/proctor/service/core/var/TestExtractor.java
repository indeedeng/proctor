package com.indeed.proctor.service.core.var;

import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.service.core.config.ExtractorSource;
import com.indeed.proctor.service.core.web.BadRequestException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

/** @author parker */
public class TestExtractor {

    private static final String DEFAULT_COUNTRY = "US";
    private static final String COUNTRY_QUERY_PARAM = "ctx.co";
    private static final String LANGUAGE_HEADER_NAME = "X-Lang";
    private static final String USER_IDENTIFIER_QUERY_PARAM = "id.user";

    @Test
    public void testExtractWithAllDataProvided() {
        final Extractor extractor = getBasicExtractor();

        final String country = "CA";
        final String language = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(COUNTRY_QUERY_PARAM, country);
        request.addHeader(LANGUAGE_HEADER_NAME, language);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);

        final RawParameters parameters = extractor.extract(request);
        final Map<String, String> context = parameters.getContext();
        assertEquals(country, context.get("country"));
        assertEquals(language, context.get("language"));
        assertEquals(2, context.size());

        final Map<TestType, String> ids = parameters.getIdentifiers();
        assertEquals(1, ids.size());
        assertEquals(userId, ids.get(TestType.ANONYMOUS_USER));

        assertEquals(0, parameters.getTest().size());
        assertEquals("", parameters.getForceGroups());
    }

    @Test
    public void testDefaultValues() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);

        final RawParameters parameters = extractor.extract(request);
        final Map<String, String> context = parameters.getContext();
        assertEquals(DEFAULT_COUNTRY, context.get("country"));
        assertEquals("fr", context.get("language"));
        assertEquals(2, context.size());

        final Map<TestType, String> ids = parameters.getIdentifiers();
        assertEquals(1, ids.size());
        assertEquals(userId, ids.get(TestType.ANONYMOUS_USER));

        assertEquals(0, parameters.getTest().size());
        assertEquals("", parameters.getForceGroups());
    }

    @Test
    public void testForceGroupsProvided() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);


        request.setParameter("prforceGroups", "mytestbucket1");

        final RawParameters parameters = extractor.extract(request);


        assertEquals(0, parameters.getTest().size());
        assertEquals("mytestbucket1", parameters.getForceGroups());
    }

    @Test
    public void testFilterTestsProvided() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);


        request.setParameter("test", "firsttest,,secondtest,thirdtest,,");

        final RawParameters parameters = extractor.extract(request);


        assertEquals(3, parameters.getTest().size());
        assertTrue(parameters.getTest().contains("firsttest"));
        assertTrue(parameters.getTest().contains("secondtest"));
        assertTrue(parameters.getTest().contains("thirdtest"));
    }


    @Test(expected = BadRequestException.class)
    public void testExtractWithMissingIdentifier() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);

        final RawParameters parameters = extractor.extract(request);
    }

    @Test(expected = BadRequestException.class)
    public void testExtractWithMissingContextParameter() {
        final Extractor extractor = getBasicExtractor();

        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);

        final RawParameters parameters = extractor.extract(request);
    }

    @Test(expected = BadRequestException.class)
    public void testExtractWithExtraQueryParameters() {
        final Extractor extractor = getBasicExtractor();

        final String fr = "fr";
        final String userId = "123456";
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LANGUAGE_HEADER_NAME, fr);
        request.setParameter(USER_IDENTIFIER_QUERY_PARAM, userId);

        request.setParameter("extra.parameter", "this.is.extra");

        final RawParameters parameters = extractor.extract(request);
    }


    private Extractor getBasicExtractor() {
        // default country
        final ContextVariable country =
            ContextVariable.newBuilder()
                .setVarName("country")
                .setSourceKey("co")
                .setDefaultValue(DEFAULT_COUNTRY)
                .setConverter(ValueConverters.stringValueConverter())
                .build();
        // no default langauge
        final ContextVariable language =
                    ContextVariable.newBuilder()
                        .setVarName("language")
                        .setSourceKey("X-Lang")
                        .setSource(ExtractorSource.HEADER)
                        .setConverter(ValueConverters.stringValueConverter())
                        .build();
        final Identifier user = Identifier.forTestType(ExtractorSource.QUERY, TestType.ANONYMOUS_USER);
        return new Extractor(ImmutableList.of(country, language), ImmutableList.of(user));
    }
}
