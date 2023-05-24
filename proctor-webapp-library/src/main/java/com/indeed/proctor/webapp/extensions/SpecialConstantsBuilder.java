package com.indeed.proctor.webapp.extensions;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** */
public class SpecialConstantsBuilder {

    public static String build(
            final Map<String, Object> definitionSpecialConstants,
            final Set<String> supportedConstants,
            final String specialConstantsName) {
        final List<String> selectedSpecialConstants =
                definitionSpecialConstants != null
                                && definitionSpecialConstants.containsKey(specialConstantsName)
                        ? ((List<String>) definitionSpecialConstants.get(specialConstantsName))
                        : Collections.emptyList();

        final StringBuilder specialConstantsHtml = new StringBuilder(300);
        specialConstantsHtml.append("<div class='row  '>");
        specialConstantsHtml.append(
                "<div class='three columns'><h6>" + specialConstantsName + "</h6></div>");
        specialConstantsHtml.append("<div class='three columns'></div>");
        specialConstantsHtml.append("<div class='nine columns'></div>");

        final Object[] supportedConstantsArray = supportedConstants.toArray();
        for (int columnNum = 0; columnNum < 9; columnNum++) {
            specialConstantsHtml.append("<div class='one columns'>");
            for (int step = columnNum; step < supportedConstantsArray.length; step += 9) {
                final String constant = (String) supportedConstantsArray[step];
                specialConstantsHtml.append(
                        "<label for='special_constants_"
                                + specialConstantsName
                                + "_"
                                + step
                                + "'>");
                final String checked =
                        selectedSpecialConstants.contains(constant) ? "checked='checked'" : "";
                specialConstantsHtml.append(
                        "<input id='special_constants_"
                                + specialConstantsName
                                + "_"
                                + step
                                + "' name='specialConstants."
                                + specialConstantsName
                                + "[]' class='mrs json' type='checkbox' value='"
                                + constant
                                + "'"
                                + checked
                                + ">"
                                + constant);
                specialConstantsHtml.append("</label>");
            }
            specialConstantsHtml.append("</div>");
        }
        specialConstantsHtml.append("</div>");

        return specialConstantsHtml.toString();
    }
}
