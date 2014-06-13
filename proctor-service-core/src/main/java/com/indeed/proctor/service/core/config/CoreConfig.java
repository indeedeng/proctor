package com.indeed.proctor.service.core.config;

import com.google.common.collect.Lists;
import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.common.JsonProctorLoaderFactory;
import com.indeed.proctor.service.core.var.ContextVariable;
import com.indeed.proctor.service.core.var.Converter;
import com.indeed.proctor.service.core.var.Extractor;
import com.indeed.proctor.service.core.var.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Contains beans and core configuration items that are independent of deployment.
 */
@Configuration
public class CoreConfig {

    @Bean
    @Autowired
    public Extractor extractor(final JsonServiceConfig jsonServiceConfig) {
        return new Extractor(createContextList(jsonServiceConfig), createIdentifierList(jsonServiceConfig));
    }

    @Bean
    @Autowired
    public Converter converter(final JsonServiceConfig jsonServiceConfig) {
        return new Converter(createContextList(jsonServiceConfig));
    }

    @Bean
    public AbstractProctorLoader proctorLoader() {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath("/var/lucene/proctor/proctor-tests-matrix.json");
        factory.setSpecificationResource("/var/lucene/proctor/spec.json");
        return factory.getLoader();
    }

    /**
     * Helper methods to create lists of variables ready for the Extractor and Converter to use.
     */

    private List<ContextVariable> createContextList(final JsonServiceConfig jsonServiceConfig) {
        // Unfortunately, this is called by two different beans, so this processing is done twice.
        // This can't be a bean because Lists and arrays can't easily be beans.

        final List<ContextVariable> contextList = Lists.newArrayList();
        for (Map.Entry<String, JsonContextVarConfig> e : jsonServiceConfig.getContext().entrySet()) {
            contextList.add(new ContextVariable(e.getKey(), e.getValue()));
        }
        return contextList;
    }

    private List<Identifier> createIdentifierList(final JsonServiceConfig jsonServiceConfig) {
        final List<Identifier> identifierList = Lists.newArrayList();
        for (Map.Entry<String, JsonVarConfig> e : jsonServiceConfig.getIdentifiers().entrySet()) {
            identifierList.add(new Identifier(e.getKey(), e.getValue()));
        }
        return identifierList;
    }
}
