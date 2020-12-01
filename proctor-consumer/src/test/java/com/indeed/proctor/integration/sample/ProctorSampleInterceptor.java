package com.indeed.proctor.integration.sample;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ProctorSampleInterceptor extends HandlerInterceptorAdapter {

    public static final String PROCTOR_GROUPS_ATTRIBUTE = "proctorGroups";
    private final SampleGroupsManager proctorGroupsManager;
    private final SampleGroupsLogger groupLogger;

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
        final SampleProctorGroups proctorGroups = new SampleProctorGroups(proctorResult);

        // log determined buckets for segmentation in later later analysis outside proctor
        groupLogger.setLogFullStringFromAbstractGroups(proctorGroups.toLoggingString());

        // provide groups to request handlers as request property
        request.setAttribute(PROCTOR_GROUPS_ATTRIBUTE, proctorGroups);
        return true;
    }

}
