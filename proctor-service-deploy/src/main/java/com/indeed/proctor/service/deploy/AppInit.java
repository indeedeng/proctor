package com.indeed.proctor.service.deploy;

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

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // PropertiesInitializer customizes the location of our properties files.
        registration.setInitParameter("contextInitializerClasses", PropertiesInitializer.class.getName());
        super.customizeRegistration(registration);
    }
}
