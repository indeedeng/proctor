package com.indeed.proctor.pipet.core.config;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.AbsentProctorSpecification;
import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.common.JsonProctorLoaderFactory;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.UrlProctorLoader;
import com.indeed.proctor.pipet.core.var.VariableConfiguration;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.util.Map;

/**
 * Contains beans and core configuration items that are independent of deployment.
 */
@Configuration
public class CoreConfig {

    @Bean
    @Autowired
    public AbstractProctorLoader proctorLoader(
            @Value("${proctor.test.matrix.path:}") final String testMatrixPath,
            @Value("${proctor.test.matrix.url:}") final String testMatrixUrl,
            final ProctorSpecification specification) {
        if (StringUtils.isNotBlank(testMatrixUrl)) {
            try {
                final UrlProctorLoader urlProctorLoader = new UrlProctorLoader(specification, testMatrixUrl);
                urlProctorLoader.run(); // ensure ProctorLoader runs once
                return urlProctorLoader;
            }
            catch(MalformedURLException mue) {
                System.err.println(mue.getMessage());
            }
        }
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath(testMatrixPath);
        factory.setSpecification(specification);
        final AbstractProctorLoader loader = factory.getLoader();
        loader.run(); // ensure ProctorLoader runs once
        return loader;
    }

    @Bean
    @Autowired
    public ProctorSpecification proctorSpecification(final VariableConfiguration varConfig) {
        // Don't use JsonPipetConfig autowired in method parameters.
        // We need the fully qualified type name for ProctorUtils to load the class without errors.
        // VariableConfiguration converted types so that "UserAgent" -> "com.indeed.proctor.pipet.deploy.useragent.UserAgent"
        final JsonPipetConfig jsonPipetConfig = varConfig.getJsonConfig();

        // Let API have access to all existing tests.
        final ProctorSpecification spec = new AbsentProctorSpecification();

        // Generate a provided context from our own type configuration.
        final Map<String, String> contextMap = Maps.newHashMap();
        for (final Map.Entry<String, JsonContextVarConfig> e : jsonPipetConfig.getContext().entrySet()) {
            final String varName = e.getKey();
            final JsonContextVarConfig var = e.getValue();
            contextMap.put(varName, var.getType());
        }
        spec.setProvidedContext(contextMap);

        return spec;
    }
}
