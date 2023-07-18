package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.extensions.renderer.BasePageRenderer;
import com.indeed.proctor.webapp.extensions.renderer.BasePageRenderer.BasePagePosition;
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
public class RenderBasePageInjectionTemplatesHandler extends TagSupport {
    private static final Logger LOGGER =
            LogManager.getLogger(RenderBasePageInjectionTemplatesHandler.class);

    private BasePagePosition position;
    private Environment branch;

    public void setPosition(final BasePageRenderer.BasePagePosition position) {
        this.position = position;
    }

    public void setBranch(final Environment branch) {
        this.branch = branch;
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
            final Map<String, BasePageRenderer> rendererBeans =
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(context, BasePageRenderer.class);
            for (final BasePageRenderer renderer : rendererBeans.values()) {
                if (position == renderer.getBasePagePosition()) {
                    renderedHTML.append(renderer.getRenderedHtml(branch));
                    renderedHTML.append(renderer.getRenderedHtml(pageContext, branch));
                }
            }
        } catch (final Exception e) {
            LOGGER.error("An error occurred when attempting to inject template.", e);
        }
        return renderedHTML.toString();
    }
}
