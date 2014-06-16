package com.indeed.proctor.service.deploy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.ServletRegistration;

/**
 * Alternative to creating a web.xml by overriding methods in a class.
 *
 * Consider using WebApplicationInitializer if you need something more advanced in the future.
 * (See commit history for previous usage of that.)
 */
public class AppInit extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return null;
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[] { AppConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    @Bean
    public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        // this is *required* to get ${...} replacements to work
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // PropertiesInitializer customizes the location of our properties files.
        registration.setInitParameter("contextInitializerClasses", PropertiesInitializer.class.getName());
        super.customizeRegistration(registration);
    }
}
