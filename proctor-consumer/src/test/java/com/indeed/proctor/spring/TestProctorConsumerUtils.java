package com.indeed.proctor.spring;

import com.indeed.proctor.consumer.ProctorConsumerUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import java.util.Map;

/**
 * @author gaurav
 */
public class TestProctorConsumerUtils {

    @Test
    public void testGetForceGroupsStringFromRequest() {
        //Test that parameter works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(ProctorConsumerUtils.FORCE_GROUPS_PARAMETER, "testing1");
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertEquals("testing1", forceGroups);
        }
        //Test that cookie works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Cookie cookie = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing1");
            mockRequest.setCookies(cookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertEquals("testing1", forceGroups);
        }
        //Test that parameter beats cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(ProctorConsumerUtils.FORCE_GROUPS_PARAMETER, "testing1");
            final Cookie cookie = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing2");
            mockRequest.setCookies(cookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertEquals("testing1", forceGroups);
        }
        //Test empty param, empty cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertNull(forceGroups);
        }
        //Test empty param, some cookies
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Cookie junkCookie = new Cookie("random", "totally random");
            final Cookie anotherJunkCookie = new Cookie("what", "yeah");
            mockRequest.setCookies(junkCookie, anotherJunkCookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertNull(forceGroups);
        }
    }

    @Test
    public void testParseForceGroupsList() {
        //Test empty string
        {
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForceGroupsList("");
            Assert.assertTrue(forcedGroups.isEmpty());
        }
        //Test invalid string
        {
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForceGroupsList("fasdfasdf;zxcvwasdf");
            Assert.assertTrue(forcedGroups.isEmpty());
        }
        //Test single group
        {
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForceGroupsList("somerandomtst1");
            Assert.assertEquals(1, (int) forcedGroups.get("somerandomtst"));
        }
        //Test multiple groups
        {
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForceGroupsList("somerandomtst1, someothertst0, notanothertst2");
            Assert.assertEquals(1, (int) forcedGroups.get("somerandomtst"));
            Assert.assertEquals(0, (int) forcedGroups.get("someothertst"));
            Assert.assertEquals(2, (int) forcedGroups.get("notanothertst"));
        }
        //Test multiple groups with some invalid stuff
        {
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForceGroupsList("somerandomtst1, someothertst0, notanothertst2,asdf;alksdfjzvc");
            Assert.assertEquals(1, (int) forcedGroups.get("somerandomtst"));
            Assert.assertEquals(0, (int) forcedGroups.get("someothertst"));
            Assert.assertEquals(2, (int) forcedGroups.get("notanothertst"));
            Assert.assertEquals(3, forcedGroups.size());
        }
    }

}
