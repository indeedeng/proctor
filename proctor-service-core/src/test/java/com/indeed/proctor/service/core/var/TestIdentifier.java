package com.indeed.proctor.service.core.var;

import com.indeed.proctor.service.core.config.ExtractorSource;
import junit.framework.Assert;
import org.junit.Test;

/** @author parker */
public class TestIdentifier {
    @Test
    public void testQueryParserBuilder() throws Exception {
        final Identifier country = Identifier.newBuilder()
            .setVarName("user")
            .build();
        Assert.assertEquals("Default source should be <prefix>.<variable_name>", "id.user", country.getSourceKey());
        Assert.assertEquals("user", country.getVarName());
        Assert.assertEquals(null, country.getDefaultValue());
        Assert.assertEquals("default source is QUERY", ExtractorSource.QUERY, country.getSource());
        Assert.assertEquals("default source is QUERY", ValueExtractors.QueryValueExtractor.getClass(), country.getExtractor().getClass());
    }

    @Test
    public void testCustomBuilder() throws Exception {
        final ValueExtractor extractor = ValueExtractors.fromQueryParameter("usr");
        final ValueExtractor header = ValueExtractors.fromQueryParameter("X-USER");
        final ValueExtractor chained = ValueExtractors.chain(extractor, header);

        final Idenfifier = custom = Identifier.newBuilder()
                           .setVarName("user")
                           .setPrefix("")
                           .setSourceKey("usr")
                           .setValueExtractor(chained)
                           .build();
        Assert.assertEquals("Source Key should not contain prefix if empty", "usr", country.getSourceKey());
        Assert.assertEquals("user", country.getVarName());
        Assert.assertEquals(null, country.getDefaultValue());
        Assert.assertEquals("default source is QUERY", ExtractorSource.QUERY, country.getSource());
        Assert.assertEquals("custom extractor provided", chained, country.getExtractor());

    }

    @Test
    public void testHeaderParserBuilder() throws Exception {
        final Identifier country = Identifier.newBuilder()
            .setVarName("country")
            .setSource(ExtractorSource.HEADER)
            .build();
        Assert.assertEquals("Source key should not include prefix if type is HEADER", "country", country.getSourceKey());
        Assert.assertEquals("country", country.getVarName());
        Assert.assertEquals(null, country.getDefaultValue());
        Assert.assertEquals("specified source is HEADER", ExtractorSource.HEADER, country.getSource());
        Assert.assertEquals("extractor for HEADER ", ValueExtractors.HeaderValueExtractor.getClass(), country.getExtractor().getClass());
    }
}
