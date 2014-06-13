package com.indeed.proctor.service.core.var;

import javax.servlet.http.HttpServletRequest;

public interface ValueExtractor {
    public String extract(final HttpServletRequest request);
}
