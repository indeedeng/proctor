package com.indeed.proctor.consumer.spring;

import com.google.common.base.Throwables;
import com.indeed.proctor.common.AbstractProctorLoader;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.PropertyDescriptor;
import java.io.IOException;

/**
 * Take a random sampling of group determinations for a test or set of tests.
 *
 * Usage requires subclassing this controller and implementing {@link #getRandomGroups} which should return the Proctor groups
 * object for a given context and Identifiers. Generally this just means passing fields from the ProctorContext to
 * a groups manager and calling determineBuckets.
 *
 * The ProctorContext class should be a bean that has all of the fields you need to run groups determination - usually
 * all of the fields in the specification's providedContext. Any field with a bean-style setter can be set using URL
 * parameters. The default implementation using reflection to populate the {@link ProctorContext} object.
 *
 * Override {@link #resolveContext} and
 * {@link #printProctorContext}
 * to provide an implementation of context resolving that doesn't use reflection.
 *
 * The page will be mapped at /sampleRandomGroups under wherever the controller is mapped.
 *
 * @author jsgroth
 */
public abstract class AbstractSampleRandomGroupsController<ProctorContext>  implements SampleRandomGroupsHttpHandler.ContextSupplier<ProctorContext> {

    private final Class<ProctorContext> contextClass;
    private final SampleRandomGroupsHttpHandler handler;

    protected AbstractSampleRandomGroupsController(final AbstractProctorLoader proctorLoader,
                                                   final Class<ProctorContext> contextClass) {
        this.handler = new SampleRandomGroupsHttpHandler<ProctorContext>(proctorLoader, this);
        this.contextClass = contextClass;
    }

    @RequestMapping(value = "/sampleRandomGroups", method = RequestMethod.GET)
    public void handleSampleRandomGroups(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        handler.handleRequest(request, response);
    }

    @Override
    public ProctorContext resolveContext(final HttpServletRequest request) {
        try {
            return getProctorContext(request);
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        } catch (InstantiationException e) {
            throw Throwables.propagate(e);
        }
    }

    // Do some magic to turn request parameters into a context object
    private ProctorContext getProctorContext(final HttpServletRequest request) throws IllegalAccessException, InstantiationException {
        final ProctorContext proctorContext = contextClass.newInstance();
        final BeanWrapper beanWrapper = new BeanWrapperImpl(proctorContext);
        for (final PropertyDescriptor descriptor : beanWrapper.getPropertyDescriptors()) {
            final String propertyName = descriptor.getName();
            if (!"class".equals(propertyName)) { // ignore class property which every object has
                final String parameterValue = request.getParameter(propertyName);
                if (parameterValue != null) {
                    beanWrapper.setPropertyValue(propertyName, parameterValue);
                }
            }
        }
        return proctorContext;
    }

    @Override
    public String printProctorContext(final ProctorContext proctorContext)  {
        final StringBuilder sb = new StringBuilder();
        final BeanWrapper beanWrapper = new BeanWrapperImpl(proctorContext);
        for (final PropertyDescriptor descriptor : beanWrapper.getPropertyDescriptors()) {
            final String propertyName = descriptor.getName();
            if (!"class".equals(propertyName)) { // ignore class property which every object has
                final Object propertyValue = beanWrapper.getPropertyValue(propertyName);
                sb.append(propertyName).append(": '").append(propertyValue).append("'").append("\n");
            }
        }
        return sb.toString();
    }

}
