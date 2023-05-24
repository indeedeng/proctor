package com.indeed.proctor.consumer;

import com.indeed.proctor.common.model.Payload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * For unit testing
 *
 * @author matts
 */
public class CannedProctorResult<T extends Bucket<?>> {
    @Nonnull public final T testVal;
    @Nullable public final Payload payload;

    public CannedProctorResult(@Nonnull final T testVal, @Nullable Payload payload) {
        this.testVal = testVal;
        this.payload = payload;
    }

    public static <T extends Bucket<?>> CannedProctorResult of(@Nonnull final T testVal) {
        return CannedProctorResult.of(testVal, null);
    }

    public static <T extends Bucket<?>> CannedProctorResult of(
            @Nonnull final T testVal, @Nullable Payload payload) {
        return new CannedProctorResult<T>(testVal, payload);
    }
}
