package com.indeed.proctor.integration;

import com.indeed.proctor.integration.sample.SampleGroupsLogger;
import com.indeed.proctor.integration.sample.SampleSpringWebAppConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


/**
 * This test demonstrates and tests the integration with Spring using an HandlerInterceptorAdapter to
 * setup an AbstractGroups instance and logging a String provided by it
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes={SampleSpringWebAppConfig.class})
@WebAppConfiguration
public class SampleSpringApplicationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SampleGroupsLogger groupsLogger;

    private MockMvc mockMvc;

    @Before
    public void setUp () {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    public void testDeterminedBucketResultAndLog() throws Exception {
        // group memberships determined from hashes of identifiers
        runRequestAndCheck(
                "user1",
                "sample1_tst0",
                "sample1_tst0,unused_tst0,#A1:sample1_tst0,#A1:unused_tst0",
                "#A1:sample1_tst0",
                "#A1:sample1_tst0"
        );
        runRequestAndCheck(
                "user3",
                "sample1_tst-1",
                "unused_tst0,#A1:unused_tst0",
                "",
                ""
        );
        runRequestAndCheck(
                "user5",
                "sample1_tst0",
                "dynamic_include_tst0,sample1_tst0,unused_tst0,#A1:dynamic_include_tst0,#A1:sample1_tst0,#A1:unused_tst0",
                "#A1:sample1_tst0",
                "#A1:dynamic_include_tst0,#A1:sample1_tst0"
        );
    }

    private void runRequestAndCheck(
            final String identifier,
            final String expectedBody,
            final String expectedFullLogString,
            final String expectedExposureLogString,
            final String expectedExposureWithDynamicLogString
    ) throws Exception {

        final MvcResult result = mockMvc.perform(get("/test").header("ctk", identifier))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        final String body = result.getResponse().getContentAsString();
        assertThat(body)
                .isEqualTo(expectedBody);
        assertThat(groupsLogger.getLogFullStringFromAbstractGroups())
                .isEqualTo(expectedFullLogString);
        assertThat(groupsLogger.getLogFullStringFromWriter())
                .isEqualTo(expectedFullLogString);
        assertThat(groupsLogger.getExposureString())
                .isEqualTo(expectedExposureLogString);

        mockMvc.perform(get("/testWithDynamic").header("ctk", identifier))
                .andExpect(MockMvcResultMatchers.status().isOk());
        assertThat(groupsLogger.getExposureString())
                .isEqualTo(expectedExposureWithDynamicLogString);
    }



}
