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
    public void testParseForcedGroups() {
        //empty request
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForcedGroups(mockRequest);
            Assert.assertTrue(forcedGroups.isEmpty());
        }
        //Test that parameter works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(ProctorConsumerUtils.FORCE_GROUPS_PARAMETER, "testing2");
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForcedGroups(mockRequest);
            Assert.assertEquals(1, forcedGroups.size());
            Assert.assertEquals(2, (int) forcedGroups.get("testing"));
        }
        //Test that cookie works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Cookie cookie = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing3");
            mockRequest.setCookies(cookie);
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForcedGroups(mockRequest);
            Assert.assertEquals(1, forcedGroups.size());
            Assert.assertEquals(3, (int) forcedGroups.get("testing"));
        }
        //Test that header works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing4");
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForcedGroups(mockRequest);
            Assert.assertEquals(1, forcedGroups.size());
            Assert.assertEquals(4, (int) forcedGroups.get("testing"));
        }
        //empty parameter
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(ProctorConsumerUtils.FORCE_GROUPS_PARAMETER, "");
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForcedGroups(mockRequest);
            Assert.assertTrue(forcedGroups.isEmpty());
        }
    }


    @Test
    public void testGetForceGroupsStringFromRequest() {
        //Test that parameter works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(ProctorConsumerUtils.FORCE_GROUPS_PARAMETER, "testing1");
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertEquals("testing1", forceGroups);
        }
        //Test that header works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing1");
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
        //Test that parameter beats header
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(ProctorConsumerUtils.FORCE_GROUPS_PARAMETER, "testing1");
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing2");
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertEquals("testing1", forceGroups);
        }
        //Test that parameter beats header and cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(ProctorConsumerUtils.FORCE_GROUPS_PARAMETER, "testing1");
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing2");
            final Cookie cookie = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing3");
            mockRequest.setCookies(cookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertEquals("testing1", forceGroups);
        }
        //Test that header beats cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing1");
            final Cookie cookie = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing2");
            mockRequest.setCookies(cookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertEquals("testing1", forceGroups);
        }
        //Test missing param, missing header, missing cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertEquals("", forceGroups);
        }
        //Test empty param, some cookies
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Cookie junkCookie = new Cookie("random", "totally random");
            final Cookie anotherJunkCookie = new Cookie("what", "yeah");
            mockRequest.setCookies(junkCookie, anotherJunkCookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            Assert.assertEquals("", forceGroups);
        }
    }

    @Test
    public void testParseForceGroupsList() {
        //Test null string
        {
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForceGroupsList(null);
            Assert.assertTrue(forcedGroups.isEmpty());
        }
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
        //Test multiple, duplicate groups, last one wins
        {
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForceGroupsList("testA1, testA2, testB2");
            Assert.assertEquals(2, forcedGroups.size());
            Assert.assertEquals(2, (int) forcedGroups.get("testA"));
            Assert.assertEquals(2, (int) forcedGroups.get("testB"));
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
