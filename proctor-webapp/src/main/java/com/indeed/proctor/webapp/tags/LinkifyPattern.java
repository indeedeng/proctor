package com.indeed.proctor.webapp.tags;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 */
public class LinkifyPattern {

    private final String name;
    private final Pattern pattern;
    private final String replacement;


    LinkifyPattern(final String name, final String pattern, final String replacement) {
        this.name = name;
        this.pattern = Pattern.compile(pattern);
        this.replacement = replacement;
    }

    public String apply(final String s) {
        final Matcher m = pattern.matcher(s);
        return m.replaceAll(replacement);
    }

    public String getName() {
        return name;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getReplacement() {
        return replacement;
    }

    public boolean matches(final String s) {
        final Matcher m = pattern.matcher(s);
        return m.matches();
    }

    public boolean presentIn(final String s) {
        final Matcher m = pattern.matcher(s);
        return m.find();
    }

    // based on the linkify from charm
}
