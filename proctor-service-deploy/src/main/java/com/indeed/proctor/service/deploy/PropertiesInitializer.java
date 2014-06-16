package com.indeed.proctor.service.deploy;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Adds Spring property sources for the following locations in this order:
 * WEB-INF/config/service-base.properties
 * ${catalina.base}/conf/service.properties
 * path pointed to by propertyPlaceholderResourceLocation Tomcat context parameter
 *
 * Extend to customize properties file name and/or paths.
 */
public class PropertiesInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String contextPropertiesParameterName = "propertyPlaceholderResourceLocation";

    @Override
    public void initialize(final ConfigurableApplicationContext applicationContext) {
        final ConfigurableEnvironment springEnv = applicationContext.getEnvironment();

        final MutablePropertySources propSources = springEnv.getPropertySources();
        for(String location : getPropertyLocations(applicationContext)) {
            tryAddPropertySource(applicationContext, propSources, location);
        }
        addPropertySources(applicationContext, propSources);
    }

    protected List<String> getPropertyLocations(final ConfigurableApplicationContext applicationContext) {
        List<String> propertyLocations = Lists.newArrayList();

        propertyLocations.addAll(getBasePropertyLocations(applicationContext));
        propertyLocations.addAll(getTomcatConfPropertyLocations(applicationContext));
        propertyLocations.addAll(getTomcatContextPropertyLocations(applicationContext));

        return propertyLocations;
    }

    protected boolean tryAddPropertySource(final ConfigurableApplicationContext applicationContext,
                                           final MutablePropertySources propSources,
                                           final String filePath) {

        if(filePath == null) {
            return false;
        }
        Resource propertiesResource = applicationContext.getResource(filePath);
        if(!propertiesResource.exists()) {
            return false;
        }
        try {
            ResourcePropertySource propertySource = new ResourcePropertySource(propertiesResource);
            propSources.addFirst(propertySource);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Can be overridden to add custom property sources.
     * @param applicationContext Context to use for loading
     * @param propSources Where to append to
     */
    protected void addPropertySources(final ConfigurableApplicationContext applicationContext,
                                      final MutablePropertySources propSources) {
    }

    protected String getWebappName() {
        return "service";
    }

    protected String getConfigFileName(String suffix) {
        String fileName = getWebappName();
        if(!Strings.isNullOrEmpty(suffix)) {
            fileName += "-" + suffix;
        }
        fileName += ".properties";
        return fileName;
    }

    protected List<String> getBasePropertyLocations(ConfigurableApplicationContext applicationContext) {
        String configFile = getRepoConfigLocation() + getConfigFileName("base");
        return Lists.newArrayList(configFile);
    }

    protected String getRepoConfigLocation() {
        return "WEB-INF/config/";
    }

    protected List<String> getTomcatConfPropertyLocations(ConfigurableApplicationContext applicationContext) {
        String tomcatPropFile = getTomcatConfDir() + getConfigFileName(null);
        return Lists.newArrayList(tomcatPropFile);
    }

    protected String getTomcatConfDir() {
        return "file:" + System.getProperty("catalina.base") + "/conf/";
    }

    protected List<String> getTomcatContextPropertyLocations(ConfigurableApplicationContext applicationContext) {
        if(!(applicationContext instanceof ConfigurableWebApplicationContext)) {
            return Collections.emptyList();
        }
        ConfigurableWebApplicationContext webApplicationContext = (ConfigurableWebApplicationContext) applicationContext;
        List<String> locations = Lists.newArrayList();
        final String tomcatContextPropertiesFile = webApplicationContext.getServletContext().getInitParameter(contextPropertiesParameterName);
        locations.add("file:" + tomcatContextPropertiesFile);
        return locations;
    }

}
