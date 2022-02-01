package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.extensions.renderer.DefinitionHistoryPageRenderer;
import com.indeed.proctor.webapp.extensions.renderer.DefinitionHistoryPageRenderer.DefinitionHistoryPagePosition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.Map;

/**
 */
public class RenderDefinitionHistoryPageInjectionTemplatesHandler extends TagSupport {
    private static final Logger LOGGER = LogManager.getLogger(RenderDefinitionHistoryPageInjectionTemplatesHandler.class);

    private DefinitionHistoryPagePosition position;
    private String testName;
    private Revision testDefinitionVersion;

    public void setPosition(final DefinitionHistoryPagePosition position) {
        this.position = position;
    }

    public void setTestName(final String testName) {
        this.testName = testName;
    }

    public void setTestDefinitionVersion(final Revision testDefinitionVersion) {
        this.testDefinitionVersion = testDefinitionVersion;
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
            final Map<String, DefinitionHistoryPageRenderer> rendererBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, DefinitionHistoryPageRenderer.class);
            for (final DefinitionHistoryPageRenderer renderer : rendererBeans.values()) {
                if (position == renderer.getDefinitionHistoryPagePositionPosition()) {
                    renderedHTML.append(renderer.getRenderedHtml(testName, testDefinitionVersion));
                    renderedHTML.append(renderer.getRenderedHtml(pageContext, testName, testDefinitionVersion));
                }
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred when attempting to inject template.", e);
        }
        return renderedHTML.toString();
    }
}
