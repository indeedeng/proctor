package com.indeed.proctor.common;

import com.indeed.proctor.common.model.ChooseResult;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.Map;

interface TestChooser<IdentifierType> {

    void printTestBuckets(@Nonnull final PrintWriter writer);

    @Nullable
    TestBucket getTestBucket(final int value);

    @Nonnull
    String[] getRules();

    @Nonnull
    ConsumableTestDefinition getTestDefinition();

    @Nonnull
    String getTestName();

    @Nonnull
    ChooseResult choose(@Nullable IdentifierType identifier, @Nonnull Map<String, Object> values);
}
