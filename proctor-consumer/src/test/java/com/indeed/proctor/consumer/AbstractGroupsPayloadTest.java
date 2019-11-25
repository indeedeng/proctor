package com.indeed.proctor.consumer;

import com.indeed.proctor.common.model.Payload;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AbstractGroupsPayloadTest {

    public static final String MAP_KEY = "fooKey";
    private final GroupsPayloadTestClass subclass = new GroupsPayloadTestClass();

    @Test
    public void testConvertToStringValue() {
        assertThat(subclass.convertToStringValue(makeStubMapPayload(MAP_KEY, null), MAP_KEY)).isNull();
        assertThat(subclass.convertToStringValue(makeStubMapPayload(MAP_KEY, "footest"), MAP_KEY)).isEqualTo("footest");
        assertThatThrownBy(() -> subclass.convertToStringValue(makeStubMapPayload("notexist", "footest"), MAP_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(MAP_KEY)
                .hasMessageContaining("notexist");
    }

    @Test
    public void testConvertToDoubleValue() {
        assertThatThrownBy(() -> subclass.convertToDoubleValue(makeStubMapPayload(MAP_KEY, null), MAP_KEY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(MAP_KEY);
        assertThat(subclass.convertToDoubleValue(makeStubMapPayload(MAP_KEY, 42), MAP_KEY)).isEqualTo(42.0);
        assertThatThrownBy(() -> subclass.convertToDoubleValue(makeStubMapPayload("notexist", "footest"), MAP_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(MAP_KEY)
                .hasMessageContaining("notexist");
    }

    @Test
    public void testConvertToLongValue() {
        assertThatThrownBy(() -> subclass.convertToLongValue(makeStubMapPayload(MAP_KEY, null), MAP_KEY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(MAP_KEY);
        assertThat(subclass.convertToLongValue(makeStubMapPayload(MAP_KEY, 42), MAP_KEY)).isEqualTo(42L);
        assertThatThrownBy(() -> subclass.convertToLongValue(makeStubMapPayload("notexist", "footest"), MAP_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(MAP_KEY)
                .hasMessageContaining("notexist");
    }

    @Test
    public void testConvertToStringArray() {
        assertThatThrownBy(() -> subclass.convertToStringArray(makeStubMapPayload(MAP_KEY, null), MAP_KEY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(MAP_KEY);
        assertThat(subclass.convertToStringArray(makeStubMapPayload(MAP_KEY, singletonList("footest")), MAP_KEY)).isEqualTo(new String[]{"footest"});
        assertThatThrownBy(() -> subclass.convertToStringArray(makeStubMapPayload("notexist", "footest"), MAP_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(MAP_KEY)
                .hasMessageContaining("notexist");
    }

    @Test
    public void testConvertToDoubleArray() {
        assertThatThrownBy(() -> subclass.convertToDoubleArray(makeStubMapPayload(MAP_KEY, null), MAP_KEY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(MAP_KEY);
        assertThat(subclass.convertToDoubleArray(makeStubMapPayload(MAP_KEY, singletonList(42)), MAP_KEY)).isEqualTo(new Double[]{42.0});
        assertThatThrownBy(() -> subclass.convertToDoubleArray(makeStubMapPayload("notexist", "footest"), MAP_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(MAP_KEY)
                .hasMessageContaining("notexist");
    }

    @Test
    public void testConvertToLongArray() {
        assertThatThrownBy(() -> subclass.convertToLongArray(makeStubMapPayload(MAP_KEY, null), MAP_KEY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining(MAP_KEY);
        assertThat(subclass.convertToLongArray(makeStubMapPayload(MAP_KEY, singletonList(42)), MAP_KEY)).isEqualTo(new Long[]{42L});
        assertThatThrownBy(() -> subclass.convertToLongArray(makeStubMapPayload("notexist", "footest"), MAP_KEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(MAP_KEY)
                .hasMessageContaining("notexist");
    }

    private static <T> Payload makeStubMapPayload(@Nullable final String key, final T mapValue) {
        final Payload payload = new Payload();
        final Map<String, Object> map = Collections.singletonMap(key, mapValue);
        payload.setMap(map);
        return payload;
    }

    private static class GroupsPayloadTestClass extends AbstractGroupsPayload {
    }
}
