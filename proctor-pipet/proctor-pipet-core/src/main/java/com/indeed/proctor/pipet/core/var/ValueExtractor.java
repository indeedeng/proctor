package com.indeed.proctor.pipet.core.var;

import javax.servlet.http.HttpServletRequest;

public interface ValueExtractor {
    String extract(final HttpServletRequest request);
}
