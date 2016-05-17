package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.webapp.extensions.ManualURLInformation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.Map;

/**
 * @author yosukey
 */
public class RenderHelpButtonTagHandler extends TagSupport {
    public enum ManualType {
        TEST_TYPE,
        RULE
    }

    private static final String TEST_TYPE_DEFAULT_URL = "http://opensource.indeedeng.io/proctor/docs/terminology/#test-type";
    private static final String RULE_DEFAULT_URL = "http://opensource.indeedeng.io/proctor/docs/test-rules/";
    private static final Logger LOGGER = Logger.getLogger(RenderHelpButtonTagHandler.class);
    private ManualType manualType;

    public void setManualType(ManualType manualType) { this.manualType = manualType; }

    public int doStartTag() {
        try {
            pageContext.getOut().print(helpButton(manualType));
        } catch (IOException e) {
            LOGGER.error("Failed to write help button to page context", e);
        }
        return SKIP_BODY;
    }

    public String helpButton(final ManualType manualType) {
        return String.format("<a class=\"ui-help-button\" target=\"_blank\" href=\"%s\">?</a>", getManualURL(manualType));
    }

    public String getManualURL(final ManualType manualType) {
        final ServletContext servletContext = pageContext.getServletContext();
        final WebApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        final Map<String, ManualURLInformation> formatterBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ManualURLInformation.class);
        if (formatterBeans.size() == 1) {
            ManualURLInformation manualURLInformation = (ManualURLInformation) formatterBeans.values().toArray()[0];
            switch (manualType) {
                case TEST_TYPE:
                    return manualURLInformation.getTestTypeManualURL();
                case RULE:
                    return manualURLInformation.getRuleManualURL();
                default:
                    return "";
            }
        } else if (formatterBeans.size() > 1) {
            LOGGER.warn("Multiple beans of type " + ManualURLInformation.class.getSimpleName() + " found, expected 0 or 1.");
        }
        return getDefaultManualURL(manualType);
    }

    private String getDefaultManualURL(final ManualType manualType) {
        switch (manualType) {
            case TEST_TYPE:
                return TEST_TYPE_DEFAULT_URL;
            case RULE:
                return RULE_DEFAULT_URL;
            default:
                return "";
        }
    }
}
