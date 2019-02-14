package com.indeed.proctor.webapp.model;

import com.google.common.base.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author parker
 * Encapsulate all primitive-type configuration properties
 */
@Component
public class WebappConfiguration {
    private final boolean useCompiledCSS;
    private final boolean useCompiledJavaScript;
    private final int verifyHttpTimeout;
    private final int verifyExecutorThreads;

    @SuppressWarnings({"SpringJavaAutowiringInspection"})
    @Autowired
    public WebappConfiguration(@Value("${use.compiled.css:true}") boolean useCompiledCSS,
                               @Value("${use.compiled.javascript:true}") boolean useCompiledJavaScript,
                               @Value("${verify.http.timeout:1000}") int verifyHttpTimeout,
                               @Value("${verify.executor.threads:10}") int verifyExecutorThreads) {
        this.useCompiledCSS = useCompiledCSS;
        this.useCompiledJavaScript = useCompiledJavaScript;
        this.verifyHttpTimeout = verifyHttpTimeout;
        this.verifyExecutorThreads = verifyExecutorThreads;
        Preconditions.checkArgument(verifyHttpTimeout > 0, "verifyHttpTimeout > 0");
        Preconditions.checkArgument(verifyExecutorThreads > 0, "verifyExecutorThreads > 0");
    }

    public boolean isUseCompiledCSS() {
        return useCompiledCSS;
    }

    public boolean isUseCompiledJavaScript() {
        return useCompiledJavaScript;
    }

    public int getVerifyHttpTimeout() {
        return verifyHttpTimeout;
    }

    public int getVerifyExecutorThreads() {
        return verifyExecutorThreads;
    }
}
