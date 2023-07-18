package com.indeed.proctor.pipet.core.var;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** @author parker */
public class TestValueExtractors {

    @Test
    public void testHeaderExtractor() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final ValueExtractor userAgentExtractor = ValueExtractors.fromHttpHeader("User-Agent");

        Assert.assertNull(
                "Request missing header value should return null",
                userAgentExtractor.extract(request));

        request.addHeader("User-Agent", "Chrome");
        Assert.assertEquals("Chrome", userAgentExtractor.extract(request));
    }

    @Test
    public void testQueryExtractor() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();

        final ValueExtractor countryExtractor = ValueExtractors.fromQueryParameter("country");

        Assert.assertNull(
                "Request missing parameter should return null", countryExtractor.extract(request));

        request.setParameter("country", "US");
        Assert.assertEquals("US", countryExtractor.extract(request));
    }

    @Test
    public void testChainedExtractor() throws Exception {
        final ValueExtractor uaQueryParameter = ValueExtractors.fromQueryParameter("ua");
        final ValueExtractor userAgentHeader = ValueExtractors.fromHttpHeader("User-Agent");

        final ValueExtractor chainedExtractor =
                ValueExtractors.chain(uaQueryParameter, userAgentHeader);
        final MockHttpServletRequest request = new MockHttpServletRequest();

        Assert.assertNull(
                "Request missing header and parameter should return null",
                chainedExtractor.extract(request));
        request.addHeader("User-Agent", "Chrome");
        Assert.assertEquals("Chrome", chainedExtractor.extract(request));
        Assert.assertEquals(
                "ChainedValueExtractor{chained(QUERY:ua, HEADER:User-Agent)}",
                chainedExtractor.toString());

        request.setParameter("ua", "Firefox");
        Assert.assertEquals(
                "Query Parameter (listed first) should extract first",
                "Firefox",
                chainedExtractor.extract(request));
        request.setParameter("ua", "");
        Assert.assertEquals(
                "Query Parameter (listed first) should extract first and allow empty values",
                "",
                chainedExtractor.extract(request));
    }
}
