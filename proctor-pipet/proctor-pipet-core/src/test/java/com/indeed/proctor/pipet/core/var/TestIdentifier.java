package com.indeed.proctor.pipet.core.var;

import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.pipet.core.config.ExtractorSource;
import junit.framework.Assert;
import org.junit.Test;

/** @author parker */
public class TestIdentifier {
    @Test
    public void testQueryParserBuilder() throws Exception {
        final Identifier country = Identifier.newBuilder().setVarName("user").build();
        Assert.assertEquals(
                "Default source should be <prefix>.<variable_name>",
                "id.user",
                country.getSourceKey());
        Assert.assertEquals("user", country.getVarName());
        Assert.assertEquals(null, country.getDefaultValue());
        Assert.assertEquals("default source is QUERY", ExtractorSource.QUERY, country.getSource());
    }

    @Test
    public void testCustomBuilder() throws Exception {
        final ValueExtractor extractor = ValueExtractors.fromQueryParameter("usr");
        final ValueExtractor header = ValueExtractors.fromQueryParameter("X-USER");
        final ValueExtractor chained = ValueExtractors.chain(extractor, header);

        final Identifier custom =
                Identifier.newBuilder()
                        .setVarName("UsEr")
                        .setPrefix("")
                        .setSourceKey("usr")
                        .setValueExtractor(chained)
                        .build();
        Assert.assertEquals(
                "Source Key should not contain prefix if empty", "usr", custom.getSourceKey());
        Assert.assertEquals("UsEr", custom.getVarName());
        Assert.assertEquals(null, custom.getDefaultValue());
        Assert.assertEquals("default source is QUERY", ExtractorSource.QUERY, custom.getSource());
    }

    @Test
    public void testHeaderParserBuilder() throws Exception {
        final Identifier country =
                Identifier.newBuilder()
                        .setTestType(TestType.ANONYMOUS_USER)
                        .setSource(ExtractorSource.HEADER)
                        .setSourceKey("usr")
                        .build();
        Assert.assertEquals(
                "Source key should not include prefix if type is HEADER",
                "usr",
                country.getSourceKey());
        Assert.assertEquals("user", country.getVarName());
        Assert.assertEquals(null, country.getDefaultValue());
        Assert.assertEquals(
                "specified source is HEADER", ExtractorSource.HEADER, country.getSource());
    }
}
