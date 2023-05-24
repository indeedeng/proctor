package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the `libs` extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final SpringLibraryAccessors laccForSpringLibraryAccessors = new SpringLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

        /**
         * Creates a dependency provider for ant (org.apache.ant:ant)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAnt() {
            return create("ant");
    }

        /**
         * Creates a dependency provider for assertj (org.assertj:assertj-core)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getAssertj() {
            return create("assertj");
    }

        /**
         * Creates a dependency provider for commonsIo (commons-io:commons-io)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCommonsIo() {
            return create("commonsIo");
    }

        /**
         * Creates a dependency provider for commonsLang (org.apache.commons:commons-lang3)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getCommonsLang() {
            return create("commonsLang");
    }

        /**
         * Creates a dependency provider for easymock (org.easymock:easymock)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getEasymock() {
            return create("easymock");
    }

        /**
         * Creates a dependency provider for easymockclassext (org.easymock:easymockclassextension)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getEasymockclassext() {
            return create("easymockclassext");
    }

        /**
         * Creates a dependency provider for guava (com.google.guava:guava)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getGuava() {
            return create("guava");
    }

        /**
         * Creates a dependency provider for jacksonAnnotations (com.fasterxml.jackson.core:jackson-annotations)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJacksonAnnotations() {
            return create("jacksonAnnotations");
    }

        /**
         * Creates a dependency provider for jacksonCore (com.fasterxml.jackson.core:jackson-core)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJacksonCore() {
            return create("jacksonCore");
    }

        /**
         * Creates a dependency provider for jacksonDatabind (com.fasterxml.jackson.core:jackson-databind)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJacksonDatabind() {
            return create("jacksonDatabind");
    }

        /**
         * Creates a dependency provider for jsr305 (com.google.code.findbugs:jsr305)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJsr305() {
            return create("jsr305");
    }

        /**
         * Creates a dependency provider for junit (junit:junit)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJunit() {
            return create("junit");
    }

        /**
         * Creates a dependency provider for log4jApi (org.apache.logging.log4j:log4j-api)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLog4jApi() {
            return create("log4jApi");
    }

        /**
         * Creates a dependency provider for log4jCore (org.apache.logging.log4j:log4j-core)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getLog4jCore() {
            return create("log4jCore");
    }

        /**
         * Creates a dependency provider for mockito (org.mockito:mockito-core)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getMockito() {
            return create("mockito");
    }

        /**
         * Creates a dependency provider for mySpringTest (org.springframework:spring-test)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getMySpringTest() {
            return create("mySpringTest");
    }

        /**
         * Creates a dependency provider for servletApi (javax.servlet:javax.servlet-api)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getServletApi() {
            return create("servletApi");
    }

        /**
         * Creates a dependency provider for springBeans (org.springframework:spring-beans)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringBeans() {
            return create("springBeans");
    }

        /**
         * Creates a dependency provider for springContext (org.springframework:spring-context)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringContext() {
            return create("springContext");
    }

        /**
         * Creates a dependency provider for springCore (org.springframework:spring-core)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringCore() {
            return create("springCore");
    }

        /**
         * Creates a dependency provider for springJdbc (org.springframework:spring-jdbc)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringJdbc() {
            return create("springJdbc");
    }

        /**
         * Creates a dependency provider for springWeb (org.springframework:spring-web)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringWeb() {
            return create("springWeb");
    }

        /**
         * Creates a dependency provider for springWebmvc (org.springframework:spring-webmvc)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSpringWebmvc() {
            return create("springWebmvc");
    }

        /**
         * Creates a dependency provider for svnkit (org.tmatesoft.svnkit:svnkit)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getSvnkit() {
            return create("svnkit");
    }

        /**
         * Creates a dependency provider for tomcatElApi (org.apache.tomcat:tomcat-el-api)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTomcatElApi() {
            return create("tomcatElApi");
    }

        /**
         * Creates a dependency provider for tomcatJasperEl (org.apache.tomcat:tomcat-jasper-el)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTomcatJasperEl() {
            return create("tomcatJasperEl");
    }

        /**
         * Creates a dependency provider for tomcatJspApi (org.apache.tomcat:tomcat-jsp-api)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTomcatJspApi() {
            return create("tomcatJspApi");
    }

        /**
         * Creates a dependency provider for tomcatServletApi (org.apache.tomcat:tomcat-servlet-api)
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getTomcatServletApi() {
            return create("tomcatServletApi");
    }

    /**
     * Returns the group of libraries at spring
     */
    public SpringLibraryAccessors getSpring() {
        return laccForSpringLibraryAccessors;
    }

    /**
     * Returns the group of versions at versions
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Returns the group of bundles at bundles
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Returns the group of plugins at plugins
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class SpringLibraryAccessors extends SubDependencyFactory {

        public SpringLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for test (org.springframework:spring-test)
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getTest() {
                return create("spring.test");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

}
