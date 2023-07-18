package com.indeed.proctor.webapp.model;

/** @author parker */
public class SessionViewModel {

    /* "Session" Variables */
    private final boolean useCompiledCSS;
    private final boolean useCompiledJavaScript;
    private final String compiledJavaScriptUrl;
    private final String nonCompiledJavaScriptUrl;

    private SessionViewModel(
            boolean useCompiledCSS,
            boolean useCompiledJavaScript,
            String compiledJavaScriptUrl,
            String nonCompiledJavaScriptUrl) {
        this.useCompiledCSS = useCompiledCSS;
        this.useCompiledJavaScript = useCompiledJavaScript;
        this.compiledJavaScriptUrl = compiledJavaScriptUrl;
        this.nonCompiledJavaScriptUrl = nonCompiledJavaScriptUrl;
    }

    public boolean isUseCompiledCSS() {
        return useCompiledCSS;
    }

    public boolean isUseCompiledJavaScript() {
        return useCompiledJavaScript;
    }

    public String getCompiledJavaScriptUrl() {
        return compiledJavaScriptUrl;
    }

    public String getNonCompiledJavaScriptUrl() {
        return nonCompiledJavaScriptUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean _useCompiledCSS;
        private boolean _useCompiledJavaScript;
        private String _compiledJavaScriptUrl;
        private String _nonCompiledJavaScriptUrl;

        public Builder setUseCompiledCSS(boolean useCompiledCSS) {
            _useCompiledCSS = useCompiledCSS;
            return this;
        }

        public Builder setUseCompiledJavaScript(boolean useCompiledJavaScript) {
            _useCompiledJavaScript = useCompiledJavaScript;
            return this;
        }

        public Builder setCompiledJavaScriptUrl(String compiledJavaScriptUrl) {
            _compiledJavaScriptUrl = compiledJavaScriptUrl;
            return this;
        }

        public Builder setNonCompiledJavaScriptUrl(String nonCompiledJavaScriptUrl) {
            _nonCompiledJavaScriptUrl = nonCompiledJavaScriptUrl;
            return this;
        }

        public SessionViewModel build() {
            return new SessionViewModel(
                    _useCompiledCSS,
                    _useCompiledJavaScript,
                    _compiledJavaScriptUrl,
                    _nonCompiledJavaScriptUrl);
        }
    }
}
