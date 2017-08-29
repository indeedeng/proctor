package com.indeed.proctor.webapp.util.temp;

import com.indeed.util.varexport.Export;
import com.indeed.util.varexport.VarExporter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ServletContextAware;

import com.google.common.base.Supplier;

/**
 * <p>Allows specifying the location of a .properties file (using Spring's resource syntax) in the servlet context in order to separate structural (object
 * relationships and implementation classes) configuration from the injection of particular values (strings, numbers, etc.).
 * </p>
 *<p>
 * Use it like this in your Spring configuration:
 *
 * <pre>
 * &lt;bean name="propertyPlaceholderConfigurer" class="com.indeed.util.core.ServletContextConfiguredPropertyPlaceholderConfigurer"&gt;
 *     &lt;property name="initParameterName" value="propertyPlaceholderResourceLocation" /&gt;&lt;!-- this happens to be the default --&gt;
 * &lt;/bean&gt;
 * </pre>
 * Then in your servlet context do something like:
 * <pre>
 *      &lt;Parameter name="propertyPlaceholderResourceLocation" value="/WEB-INF/config/settings.properties" /&gt;
 * </pre>
 * </p>
 * @author ketan
 *
 */
public class ServletContextConfiguredPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements ServletContextAware, ApplicationContextAware, Supplier<Properties> {
    private static final Logger LOGGER = Logger.getLogger(ServletContextConfiguredPropertyPlaceholderConfigurer.class);
    private ServletContext servletContext = null;
    private ApplicationContext applicationContext = null;
    private String initParameterName = "propertyPlaceholderResourceLocation";
    private String optionalPropertiesInitParameterName = "optionalPropertyPlaceholderResourceLocation";
    private String initParameterValue = null;
    private final List<Resource> resourcesLoaded = new ArrayList<Resource>();
    private String optionalInitParameterValue;
    private Properties properties = new Properties();

    public ServletContextConfiguredPropertyPlaceholderConfigurer() {
        VarExporter.forNamespace("ResourceConfig").includeInGlobal().export(this, "");
    }

    public void setInitParameterName(final String initParameterName) {
        this.initParameterName = initParameterName;
    }

    public void setOptionalPropertiesInitParameterName(final String optionalPropertiesInitParameterName) {
        this.optionalPropertiesInitParameterName = optionalPropertiesInitParameterName;
    }

    @Override
    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;

        final Resource[] resources = createResources();
        setLocations(resources);
    }

    protected Resource[] createResources() {
        initParameterValue = servletContext.getInitParameter(initParameterName);
        if (initParameterValue == null) {
            throw new IllegalArgumentException("No value specified in servlet context for " + initParameterName);
        }

        loadResources(initParameterValue, resourcesLoaded, true);
        optionalInitParameterValue = servletContext.getInitParameter(optionalPropertiesInitParameterName);
        if (optionalInitParameterValue != null) {
            loadResources(optionalInitParameterValue, resourcesLoaded, false);
        }

        return resourcesLoaded.toArray(new Resource[resourcesLoaded.size()]);
    }

    @Export(name="properties-resources-parameter-required")
    public String getInitParameterValue() {
        return initParameterValue;
    }

    @Export(name="properties-resources-parameter-optional")
    public String getOptionalInitParameterValue() {
        return optionalInitParameterValue;
    }

    @Export(name="properties-resources-loaded")
    public List<Resource> getResourcesLoaded() {
        return resourcesLoaded;
    }

    protected void loadResources(final String resourcesString, final List<Resource> resources, final boolean required) {
        final String[] resourceLocations = resourcesString.split("[, ]");
        for (int i = 0; i < resourceLocations.length; i++) {
            final Resource resource = this.applicationContext.getResource(resourceLocations[i]);
            if (attemptToLoadResource(resource, required)) {
                resources.add(resource);
            }
        }
    }

    private boolean attemptToLoadResource(final Resource resource, final boolean required) {
        if (resource == null) {
            if (required) {
                throw new IllegalArgumentException("Unable to find resource " + resource + " specified by " + initParameterName);
            }
            LOGGER.info("Unable to find optional resource " + resource);
            return false;
        }
        try {
            final InputStream inputStream = resource.getInputStream();
            inputStream.close();
            return true;
        } catch (final IOException e) {
            if (required) {
                throw new IllegalArgumentException("Unable to find resource " + resource + " specified by " + initParameterName, e);
            }
            LOGGER.info("Unable to find optional resource " + resource, e);
        }
        return false;
    }

    @Override
    protected void processProperties(final ConfigurableListableBeanFactory beanFactoryToProcess, final Properties properties) throws BeansException {
        super.processProperties(beanFactoryToProcess, properties);
        this.properties = properties;
    }

    @Override
    public Properties get() {
        return properties;
    }

    @Override
    public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (this.servletContext == null) {
            LOGGER.warn("No servletContext set; " + ServletContextConfiguredPropertyPlaceholderConfigurer.class.getName() + " cannot be used outside a WebApplicationContext");
        }
        super.postProcessBeanFactory(beanFactory);
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}