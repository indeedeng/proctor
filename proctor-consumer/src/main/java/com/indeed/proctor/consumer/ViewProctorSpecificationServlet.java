package com.indeed.proctor.consumer;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.SpecificationResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ViewProctorSpecificationServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ViewProctorSpecificationServlet.class);

    private static final String DEFAULT_PROCTOR_SPEC_PATH = "/WEB-INF/proctor/proctor-specification.json";
    // Definitely don't want to blow up while viewing the spec
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    private String proctorSpecPath = DEFAULT_PROCTOR_SPEC_PATH;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        final String proctorSpecPathParameter = config.getInitParameter("proctorSpecPath");
        if (! CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(proctorSpecPathParameter))) {
            proctorSpecPath = proctorSpecPathParameter;
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain;charset=UTF-8");

        final InputStream resourceAsStream;
        if (proctorSpecPath.startsWith("classpath:")) {
            final String proctorSpecClassPath = proctorSpecPath.substring("classpath:".length());
            resourceAsStream = getClass().getClassLoader().getResourceAsStream(proctorSpecClassPath);
        } else {
            resourceAsStream = getServletContext().getResourceAsStream(proctorSpecPath);
        }

        final SpecificationResult results = new SpecificationResult();
        try {
            if (resourceAsStream == null) {
                throw new ServletException("No resource stream for proctorSpecPath " + proctorSpecPath);
            }

            final ProctorSpecification specification = OBJECT_MAPPER.readValue(resourceAsStream, ProctorSpecification.class);
            results.setSpecification(specification);
        } catch (final Throwable t) {
            final String message = "Unable to parse specification in " + proctorSpecPath;

            LOGGER.error(message, t);

            final StringWriter sw = new StringWriter();
            final PrintWriter writer = new PrintWriter(sw);
            t.printStackTrace(writer);

            results.setError(message);
            results.setException(sw.toString());
        }

        final PrintWriter writer = resp.getWriter();

        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(writer, results);
    }
}
