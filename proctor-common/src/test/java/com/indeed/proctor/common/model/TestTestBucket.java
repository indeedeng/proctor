package com.indeed.proctor.common.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestTestBucket {

    @Test
    public void testEquals() {
        assertFalse(new TestBucket().equals(null));
        assertFalse(new TestBucket().equals("hello"));
        assertEquals(new TestBucket(), new TestBucket());

        // not sure why equals compares only test name, could be a bug in the code
        assertEquals(new TestBucket("foo", 1, "d1"), new TestBucket("foo", 1, "d1"));
        assertEquals(new TestBucket("foo", 1, "d1"), new TestBucket("foo", 2, "d1"));
        assertEquals(new TestBucket("foo", 1, "d1"), new TestBucket("foo", 1, "d2"));
        final Payload p1 = new Payload();
        p1.setStringValue("p1String");
        final Payload p2 = new Payload();
        p2.setDoubleValue(0.4);
        assertEquals(new TestBucket("foo", 1, "d1", p1), new TestBucket("foo", 1, "d1", p2));
    }

    @Test
    public void testFullEquals() {
        assertFalse(new TestBucket().fullEquals(null));
        assertFalse(new TestBucket().fullEquals("hello"));
        assertTrue(new TestBucket().fullEquals(new TestBucket()));
        assertTrue(new TestBucket("foo", 1, "d1").fullEquals(new TestBucket("foo", 1, "d1")));
        assertFalse(new TestBucket("foo", 1, "d1").fullEquals(new TestBucket("foo", 2, "d1")));
        assertFalse(new TestBucket("foo", 1, "d1").fullEquals(new TestBucket("foo", 1, "d2")));
        final Payload p1 = new Payload();
        p1.setStringValue("p1String");
        final Payload p1b = new Payload();
        p1b.setStringValue("p1String");
        final Payload p2 = new Payload();
        p2.setDoubleValue(0.4);
        assertTrue(new TestBucket("foo", 1, "d1", p1).fullEquals(new TestBucket("foo", 1, "d1", p1b)));
        assertFalse(new TestBucket("foo", 1, "d1", p1).fullEquals(new TestBucket("foo", 1, "d1", p2)));
    }

}
