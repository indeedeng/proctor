package com.indeed.proctor.integration.sample;

import com.indeed.proctor.consumer.logging.TestMarkingObserver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static com.indeed.proctor.integration.sample.ProctorSampleInterceptor.PROCTOR_GROUPS_ATTRIBUTE;
import static com.indeed.proctor.integration.sample.ProctorSampleInterceptor.PROCTOR_OBSERVER_ATTRIBUTE;
import static com.indeed.proctor.integration.sample.SampleProctorGroups.SAMPLE_1_TST;

@RestController
public class SampleController {

    @RequestMapping("test")
    @ResponseBody
    public String getTest(final HttpServletRequest request) {
        final SampleProctorGroups proctorGroups = (SampleProctorGroups) request.getAttribute(PROCTOR_GROUPS_ATTRIBUTE);
        return SAMPLE_1_TST + proctorGroups.getSample1_tstValue();
    }

    @RequestMapping("testWithDynamic")
    @ResponseBody
    public String getTestWithDynamic(final HttpServletRequest request) {
        final TestMarkingObserver observer = (TestMarkingObserver) request.getAttribute(PROCTOR_OBSERVER_ATTRIBUTE);
        final SampleProctorGroups proctorGroups = (SampleProctorGroups) request.getAttribute(PROCTOR_GROUPS_ATTRIBUTE);
        // typically this would happen in Interceptor instead, placed here only to have 2 different behaviors
        observer.markTestsUsedForLogging(proctorGroups.getAsProctorResult().getDynamicallyLoadedTests());
        return SAMPLE_1_TST + proctorGroups.getSample1_tstValue();
    }
}
