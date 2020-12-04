package com.indeed.proctor.integration.sample;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.StringProctorLoader;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.dynamic.MetaTagsFilter;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.indeed.proctor.integration.sample.SampleProctorGroups.DYNAMIC_INCLUDE_TST;
import static com.indeed.proctor.integration.sample.SampleProctorGroups.SAMPLE_1_TST;
import static com.indeed.proctor.integration.sample.SampleProctorGroups.UNUSED_TST;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

@Configuration
@EnableWebMvc
@Import({SampleController.class, ProctorSampleInterceptor.class})
public class SampleSpringWebAppConfig extends WebMvcConfigurerAdapter {

    public static final String SAMPLETAG = "sampletag";
    @Autowired
    private ProctorSampleInterceptor proctorGroupsInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(proctorGroupsInterceptor);
    }

    @Bean
    public SampleGroupsLogger groupsLogger() {
        return new SampleGroupsLogger();
    }

    /**
     * Stub Bean to replace json specification
     */
    @Bean
    public ProctorSpecification staticSpecification() {
        return new ProctorSpecification(
                Collections.emptyMap(),
                ImmutableMap.<String, TestSpecification>builder()
                        .put(SAMPLE_1_TST, new TestSpecification())
                        // DYNAMIC_INCLUDE_TST included dynamically via SAMPLETAG
                        //.put(DYNAMIC_INCLUDE_TST, ...
                        .put(UNUSED_TST, new TestSpecification())
                        .build(),
                new DynamicFilters(singletonList(new MetaTagsFilter(singleton(SAMPLETAG)))));
    }

    /*
     * Stub Bean to replace definition from loader
     */
    @Bean
    public TestMatrixArtifact testMatrixArtifact() {
        final TestMatrixArtifact testMatrixArtifact = new TestMatrixArtifact();
        testMatrixArtifact.setTests(ImmutableMap.<String, ConsumableTestDefinition>builder()
                .put(SAMPLE_1_TST, stubTestDefinition(emptyList()))
                .put(DYNAMIC_INCLUDE_TST, stubTestDefinition(singletonList(SAMPLETAG)))
                .put(UNUSED_TST, stubTestDefinition(emptyList()))
                .build());
        final Audit audit = new Audit();
        audit.setVersion("v1");
        testMatrixArtifact.setAudit(audit);
        return testMatrixArtifact;
    }

    private ConsumableTestDefinition stubTestDefinition(final List<String> metatags) {
        return new ConsumableTestDefinition("", "", TestType.ANONYMOUS_USER, "salt1",
                Arrays.asList(
                        new TestBucket("inactive", -1, ""),
                        new TestBucket("control", 0, ""),
                        new TestBucket("active", 1, "")),
                Arrays.asList(new Allocation(
                        "",
                        Arrays.asList(new Range(-1, 0.5), new Range(0, 0.5)),
                        "#A1"
                )),
                false, Collections.emptyMap(), "", metatags);
    }

    @Bean
    public StringProctorLoader proctorSupplier(
            final ProctorSpecification proctorSpecification,
            final TestMatrixArtifact testMatrixArtifact
    ) {
        final StringProctorLoader stringProctorLoader = new StringProctorLoader(proctorSpecification, "fixed", "") {
            @Override
            protected TestMatrixArtifact loadTestMatrix() throws IOException {
                // stubbed load
                return testMatrixArtifact;
            }
        };
        stringProctorLoader.load(); // simulate load
        return stringProctorLoader;
    }

    @Bean
    public SampleGroupsManager groupsManager(final StringProctorLoader proctorSupplier) {
        return new SampleGroupsManager(proctorSupplier::get);
    }
}
