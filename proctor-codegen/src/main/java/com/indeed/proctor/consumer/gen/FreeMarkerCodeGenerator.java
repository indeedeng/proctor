package com.indeed.proctor.consumer.gen;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Possibly destined for another home one day
 *
 * @author ketan
 */
public abstract class FreeMarkerCodeGenerator {
    abstract Map<String, Object> populateRootMap(
            String input,
            Map<String, Object> baseContext,
            String packageName,
            String className
    );

    public static String toJavaIdentifier(final String s) {
        final StringBuilder sb = new StringBuilder();
        sb.append(Character.toLowerCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (Character.isLetter(c) || Character.isDigit(c) || (c == '_')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String toEnumName(final String s) {
        final StringBuilder sb = new StringBuilder();
        boolean lastWasUpper = true;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (Character.isLetter(c)) {
                if (Character.isLowerCase(c)) {
                    sb.append(Character.toUpperCase(c));
                    lastWasUpper = false;
                } else {
                    if (!lastWasUpper) {
                        sb.append('_');
                    }
                    sb.append(c);
                    lastWasUpper = true;
                }
            } else {
                if (c == '-') {
                    sb.append('_');
                } else {
                    //  TODO: validate allowed characters
                    sb.append(c);
                }
                lastWasUpper = false;
            }
        }
        return sb.toString();
    }

    public static String uppercaseFirstChar(final String name) {
        final StringBuilder sb = new StringBuilder(name);
        sb.setCharAt(0, Character.toUpperCase(name.charAt(0)));
        final String javaClassName = sb.toString();
        return javaClassName;
    }

    public static String packageToPath(final String packageName) {
        return packageName.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
    }

    protected Configuration getFreemarkerConfiguration(final String templatePath) {
        final Configuration config = new Configuration();
        config.setObjectWrapper(new DefaultObjectWrapper());
        config.setClassForTemplateLoading(getClass(), templatePath);
        return config;
    }

    protected void generate(
            final String input,
            final String target,
            final Map<String, Object> baseContext,
            final String packageName,
            final String className,
            final String templatePath,
            final String templateName,
            final String fileExtension
    ) throws CodeGenException {
        final Configuration config = getFreemarkerConfiguration(templatePath);

        final Template template = loadTemplate(templateName, templatePath, config);
        final File fullPath = new File(target + File.separator + packageToPath(packageName) + File.separator + className + fileExtension);
        try {
            fullPath.getParentFile().mkdirs();
            final PrintWriter out = new PrintWriter(fullPath);

            final Map<String, Object> rootMap = populateRootMap(input, baseContext, packageName, className);

            final TemplateModel model = new SimpleHash(rootMap);

            template.process(model, out);
            out.close();
        } catch (final TemplateException e) {
            throw new RuntimeException("Unable to run template " + templateName + " to: " + fullPath, e);
        } catch (final IOException e) {
            throw new RuntimeException("Unable to write to target file: " + fullPath, e);
        }
    }

    public static Template loadTemplate(
            final String templateName,
            final String templatePath,
            final Configuration config
    ) throws CodeGenException {
        try {
            return config.getTemplate(templateName);
        } catch (final IOException e) {
            throw new CodeGenException("Unable to load template " + templateName + " from " + templatePath, e);
        }
    }

}
