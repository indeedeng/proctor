package com.indeed.proctor.service.core.config;

import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.common.JsonProctorLoaderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contains beans and core configuration items that are independent of deployment.
 */
@Configuration
public class CoreConfig {

    @Bean
    public AbstractProctorLoader proctorLoader(
            @Value("${proctor.test.matrix.path}") final String testMatrixPath,
            @Value("${proctor.test.specification.path}") final String testSpecPath) {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath(testMatrixPath);
        factory.setSpecificationResource(testSpecPath);
        final AbstractProctorLoader loader = factory.getLoader();
        loader.run(); // ensure ProctorLoader runs once
        return loader;
    }
}
