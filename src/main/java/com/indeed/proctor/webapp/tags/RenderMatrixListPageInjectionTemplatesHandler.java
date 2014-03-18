package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.webapp.extensions.renderer.MatrixListPageRenderer;
import com.indeed.proctor.webapp.extensions.renderer.MatrixListPageRenderer.MatrixListPagePosition;
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
public class RenderMatrixListPageInjectionTemplatesHandler extends TagSupport {
    private static final Logger LOGGER = Logger.getLogger(RenderMatrixListPageInjectionTemplatesHandler.class);

    private MatrixListPagePosition position;
    private String testName;
    private TestMatrixVersion testMatrixVersion;
    private TestDefinition testDefinition;

    public void setPosition(final MatrixListPagePosition position) {
        this.position = position;
    }

    public void setTestName(final String testName) {
        this.testName = testName;
    }

    public void setTestMatrixVersion(final TestMatrixVersion testMatrixVersion) {
        this.testMatrixVersion = testMatrixVersion;
    }

    public void setTestDefinition(final TestDefinition testDefinition) {
        this.testDefinition = testDefinition;
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
            final Map<String, MatrixListPageRenderer> rendererBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, MatrixListPageRenderer.class);
            for (final MatrixListPageRenderer renderer : rendererBeans.values()) {
                if (position == renderer.getMatrixListPagePosition()) {
                    renderedHTML.append(renderer.getRenderedHtml(testName, testMatrixVersion, testDefinition));
                }
            }
        } catch (Exception e) {
            LOGGER.error("An error occured when attempting to inject template.", e);
        }
        return renderedHTML.toString();
    }
}
