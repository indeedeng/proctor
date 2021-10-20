package com.indeed.proctor.integration.sample;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.consumer.ProctorGroupsWriter;
import com.indeed.proctor.consumer.logging.TestMarkingObserver;
import com.indeed.proctor.consumer.logging.TestGroupFormatter;
import com.indeed.proctor.consumer.logging.TestUsageObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ProctorSampleInterceptor extends HandlerInterceptorAdapter {

    public static final String PROCTOR_GROUPS_ATTRIBUTE = "proctorGroups";
    public static final String PROCTOR_OBSERVER_ATTRIBUTE = "proctorObserver";
    private final SampleGroupsManager proctorGroupsManager;
    private final SampleGroupsLogger groupLogger;

    // same logging as current AbstractGroups
    final ProctorGroupsWriter legacyWriter = new ProctorGroupsWriter.Builder(
            TestGroupFormatter.WITHOUT_ALLOC_ID, TestGroupFormatter.WITH_ALLOC_ID
    ).build();
    final ProctorGroupsWriter simpleWriter = ProctorGroupsWriter.Builder.withFormatter(TestGroupFormatter.WITH_ALLOC_ID).build();

    @Autowired
    public ProctorSampleInterceptor(final SampleGroupsManager proctorGroupsManager, final SampleGroupsLogger groupLogger) {
        this.proctorGroupsManager = proctorGroupsManager;
        this.groupLogger = groupLogger;
    }

    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler
    ) throws Exception {
        final String ctk = request.getHeader("ctk");
        final Identifiers identifiers = new Identifiers(ImmutableMap.of(
                TestType.ANONYMOUS_USER, ctk
        ));
        final ProctorResult proctorResult = proctorGroupsManager.determineBuckets(identifiers);
        // prepare exposure logging
        final TestUsageObserver observer = new TestMarkingObserver(proctorResult);
        final SampleProctorGroups proctorGroups = new SampleProctorGroups(proctorResult, observer);

        // log determined buckets for segmentation in later later analysis outside proctor
        groupLogger.setLogFullStringFromAbstractGroups(proctorGroups.toLoggingString());
        // log using new new writer approach, but in same format as abstractGroups
        groupLogger.setLogFullStringFromWriter(legacyWriter.writeGroupsAsString(proctorGroups.getAsProctorResult()));

        // provide groups to request handlers as request property
        request.setAttribute(PROCTOR_GROUPS_ATTRIBUTE, proctorGroups);
        request.setAttribute(PROCTOR_OBSERVER_ATTRIBUTE, observer);
        return true;
    }

    @Override
    public void afterCompletion(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler,
            final Exception ex) throws Exception {
        final TestMarkingObserver observer = (TestMarkingObserver) request.getAttribute(PROCTOR_OBSERVER_ATTRIBUTE);

        // exposure logging using groups observed as used during the request
        groupLogger.setExposureString(simpleWriter.writeGroupsAsString(observer.asProctorResult()));
    }
}
