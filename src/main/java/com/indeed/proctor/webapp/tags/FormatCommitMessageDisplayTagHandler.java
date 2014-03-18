package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.webapp.extensions.CommitMessageDisplayFormatter;
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
public class FormatCommitMessageDisplayTagHandler extends TagSupport {
    private static final Logger LOGGER = Logger.getLogger(FormatCommitMessageDisplayTagHandler.class);

    private String commitMessage;

    public void setCommitMessage(final String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public int doStartTag() {
        try {
            pageContext.getOut().print(formatMessage(commitMessage));
        } catch (IOException e) {
            LOGGER.error("Failed to write formatted commit message to page context", e);
        }

        return SKIP_BODY;
    }

    public String formatMessage(final String commitMessage) {
        final ServletContext servletContext = pageContext.getServletContext();
        final WebApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        try {
            final Map<String, CommitMessageDisplayFormatter> formatterBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context,CommitMessageDisplayFormatter.class);

            if (formatterBeans.size() == 0) {
                //No bean found, which is acceptable.
                return commitMessage;
            } else if (formatterBeans.size() == 1) {
                CommitMessageDisplayFormatter formatter = (CommitMessageDisplayFormatter) formatterBeans.values().toArray()[0];
                return formatter.formatMessage(commitMessage);
            } else {
                throw new IllegalArgumentException("Multiple beans of type " + CommitMessageDisplayFormatter.class.getSimpleName()  + " found, expected 0 or 1.");
            }
        } catch (Exception e) {
            LOGGER.error("An error occured when formatting commit message.", e);
            return commitMessage;
        }
    }
}
