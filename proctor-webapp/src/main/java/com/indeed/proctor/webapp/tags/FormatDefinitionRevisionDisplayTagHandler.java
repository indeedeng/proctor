package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.extensions.DefinitionRevisionDisplayFormatter;
import org.apache.commons.lang3.StringEscapeUtils;
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
public class FormatDefinitionRevisionDisplayTagHandler extends TagSupport {
    private static final Logger LOGGER = LogManager.getLogger(FormatDefinitionRevisionDisplayTagHandler.class);

    private Revision revision;

    public void setRevision(final Revision revision) {
        this.revision = revision;
    }

    public int doStartTag() {
        try {
            pageContext.getOut().print(formatRevisionDisplay(revision));
        } catch (final IOException e) {
            LOGGER.error("Failed to write formatted revision to page context", e);
        }

        return SKIP_BODY;
    }

    public String formatRevisionDisplay(final Revision revision) {
        final String defaultFormattedRevision = revision.getAuthor() + " @ " + revision.getDate() + " (" + revision.getRevision() + ")";
        final ServletContext servletContext = pageContext.getServletContext();
        final WebApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        try {
            final Map<String, DefinitionRevisionDisplayFormatter> formatterBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context,DefinitionRevisionDisplayFormatter.class);

            if (formatterBeans.isEmpty()) {
                //No bean found, which is acceptable.
                return StringEscapeUtils.escapeHtml4(defaultFormattedRevision);
            } else if (formatterBeans.size() == 1) {
                final DefinitionRevisionDisplayFormatter formatter = formatterBeans.values().iterator().next();
                return formatter.formatRevision(revision);
            } else {
                throw new IllegalArgumentException("Multiple beans of type " + DefinitionRevisionDisplayFormatter.class.getSimpleName()  + " found, expected 0 or 1.");
            }
        } catch (final Exception e) {
            LOGGER.error("An error occurred when retrieving revision url.", e);
            return defaultFormattedRevision;
        }
    }
}
