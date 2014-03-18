package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.webapp.extensions.renderer.EditPageRenderer;
import com.indeed.proctor.webapp.extensions.renderer.EditPageRenderer.EditPagePosition;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.Map;

/**
 */
public class RenderEditPageInjectionTemplatesTagHandler extends TagSupport {
    private static final Logger LOGGER = Logger.getLogger(RenderEditPageInjectionTemplatesTagHandler.class);

    private EditPagePosition position;
    private String testName;
    private String testDefinitionJson;
    private boolean isCreate;

    public void setPosition(final EditPagePosition position) {
        this.position = position;
    }

    public void setTestName(final String testName) {
        this.testName = testName;
    }

    public void setTestDefinitionJson(final String testDefinitionJson) {
        this.testDefinitionJson = testDefinitionJson;
    }

    public void setIsCreate(final boolean isCreate) {
        this.isCreate = isCreate;
    }

    public int doStartTag() {
        try {
            pageContext.getOut().print(renderTemplates());
        } catch (IOException e) {
            LOGGER.error("Failed to write rendered html to page context", e);
        }

        return SKIP_BODY;
    }

    private String renderTemplates() {
        final StringBuilder renderedHTML = new StringBuilder();
        final ServletContext servletContext = pageContext.getServletContext();
        final WebApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        try {
            final Map<String, EditPageRenderer> rendererBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, EditPageRenderer.class);
            for (final EditPageRenderer renderer : rendererBeans.values()) {
                if (position == renderer.getEditPagePosition()) {
                    renderedHTML.append(renderer.getRenderedHtml(testName, testDefinitionJson, isCreate));
                }
            }
        } catch (Exception e) {
            LOGGER.error("An error occured when attempting to inject template.", e);
        }
        return renderedHTML.toString();
    }
}
