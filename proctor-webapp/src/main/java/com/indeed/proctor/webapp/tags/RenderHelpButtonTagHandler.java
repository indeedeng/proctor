package com.indeed.proctor.webapp.tags;

import com.google.common.base.Strings;
import com.indeed.proctor.webapp.extensions.HelpURLInformation;
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
    public enum HelpType {
        TEST_TYPE,
        RULE,
        AUTO_PROMOTION
    }

    private static final String TEST_TYPE_DEFAULT_URL = "http://opensource.indeedeng.io/proctor/docs/terminology/#test-type";
    private static final String RULE_DEFAULT_URL = "http://opensource.indeedeng.io/proctor/docs/test-rules/";
    private static final Logger LOGGER = Logger.getLogger(RenderHelpButtonTagHandler.class);
    private HelpType helpType;

    public void setHelpType(HelpType helpType) { this.helpType = helpType; }

    public int doStartTag() {
        try {
            pageContext.getOut().print(helpButton(helpType));
        } catch (IOException e) {
            LOGGER.error("Failed to write help button to page context", e);
        }
        return SKIP_BODY;
    }

    public String helpButton(final HelpType helpType) {
        final String helpURL = getHelpURL(helpType);
        if (Strings.isNullOrEmpty(helpURL)) {
            return "";
        }
        return String.format("<a class=\"ui-help-button\" target=\"_blank\" href=\"%s\">?</a>", helpURL);
    }

    public String getHelpURL(final HelpType helpType) {
        final ServletContext servletContext = pageContext.getServletContext();
        final WebApplicationContext context = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        final Map<String, HelpURLInformation> formatterBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HelpURLInformation.class);
        if (formatterBeans.size() == 1) {
            HelpURLInformation helpURLInformation = (HelpURLInformation) formatterBeans.values().toArray()[0];
            switch (helpType) {
                case TEST_TYPE:
                    return helpURLInformation.getTestTypeHelpURL();
                case RULE:
                    return helpURLInformation.getRuleHelpURL();
                case AUTO_PROMOTION:
                    return helpURLInformation.getAutoPromotionHelpURL();
                default:
                    return "";
            }
        } else if (formatterBeans.size() > 1) {
            LOGGER.warn("Multiple beans of type " + HelpURLInformation.class.getSimpleName() + " found, expected 0 or 1.");
        }
        return getDefaultHelpURL(helpType);
    }

    private String getDefaultHelpURL(final HelpType helpType) {
        switch (helpType) {
            case TEST_TYPE:
                return TEST_TYPE_DEFAULT_URL;
            case RULE:
                return RULE_DEFAULT_URL;
            default:
                return "";
        }
    }
}
