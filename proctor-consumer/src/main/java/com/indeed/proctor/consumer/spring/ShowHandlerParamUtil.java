package com.indeed.proctor.consumer.spring;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

/**
 * @author piotr
 */
public class ShowHandlerParamUtil {
    private static final String TEST_NAME_PARAM = "test";

    /**
     * Returns collection of test names that are comma-separated in the request's query parameter.
     *
     * Returns null if there was no query parameter.
     *
     * Ex: ?test=bgcolortst,example2
     * returns ['bgcolortst, example2']
     *
     * @param request request
     * @return a {@link Collection} of query parameters
     */
    public static Collection<String> getTestQueryParameters(final HttpServletRequest request)
    {
        final String testNameParam = request.getParameter(TEST_NAME_PARAM);
        List<String> testNameList = null;
        if (testNameParam != null) {
            testNameList = Lists.newArrayList(
                    Splitter.on(',').trimResults().omitEmptyStrings().split(testNameParam));
        }
        return testNameList;
    }
}
