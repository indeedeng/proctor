package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.webapp.extensions.renderer.DefinitionDeletePageRenderer;
import com.indeed.proctor.webapp.extensions.renderer.DefinitionDeletePageRenderer.DefinitionDeletePagePosition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.Map;

/** */
public class RenderDefinitionDeletePageInjectionTemplatesHandler extends TagSupport {
    private static final Logger LOGGER =
            LogManager.getLogger(RenderDefinitionDeletePageInjectionTemplatesHandler.class);

    private DefinitionDeletePagePosition position;
    private String testName;

    public void setPosition(final DefinitionDeletePagePosition position) {
        this.position = position;
    }

    public void setTestName(final String testName) {
        this.testName = testName;
    }

    public int doStartTag() {
        try {
            pageContext.getOut().print(renderTemplates());
        } catch (final IOException e) {
            LOGGER.error("Failed to write rendered html to page context", e);
        }

        return SKIP_BODY;
    }

    private String renderTemplates() {
        final StringBuilder renderedHTML = new StringBuilder();
        final ServletContext servletContext = pageContext.getServletContext();
        final WebApplicationContext context =
                WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        try {
            final Map<String, DefinitionDeletePageRenderer> rendererBeans =
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(
                            context, DefinitionDeletePageRenderer.class);
            for (final DefinitionDeletePageRenderer renderer : rendererBeans.values()) {
                if (position == renderer.getDefinitionDeletePagePosition()) {
                    renderedHTML.append(renderer.getRenderedHtml(testName));
                    renderedHTML.append(renderer.getRenderedHtml(pageContext, testName));
                }
            }
        } catch (final Exception e) {
            LOGGER.error("An error occurred when attempting to inject template.", e);
        }
        return renderedHTML.toString();
    }
}
