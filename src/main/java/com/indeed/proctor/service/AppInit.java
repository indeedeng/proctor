package com.indeed.proctor.service;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;


/**
 * Alternative to creating a web.xml
 */
public class AppInit implements WebApplicationInitializer {

    @Override
    public void onStartup(final ServletContext container) {
        AnnotationConfigWebApplicationContext root = new AnnotationConfigWebApplicationContext();
        root.register(AppConfig.class);
        container.addListener(new ContextLoaderListener(root));

        // No registered dispatcher configuration because it looks like we don't need one.
        AnnotationConfigWebApplicationContext dispatcherContext = new AnnotationConfigWebApplicationContext();
        ServletRegistration.Dynamic dispatcher =
                container.addServlet("dispatcher", new DispatcherServlet(dispatcherContext));

        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");
    }
}
