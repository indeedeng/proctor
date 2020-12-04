package com.indeed.proctor.integration.sample;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.consumer.AbstractGroupsManager;

import java.util.Collections;
import java.util.Map;

import static com.indeed.proctor.integration.sample.SampleProctorGroups.SAMPLE_1_TST;

public class SampleGroupsManager extends AbstractGroupsManager {
    protected SampleGroupsManager(final Supplier<Proctor> proctorSource) {
        super(proctorSource);
    }

    @Override
    protected Map<String, TestBucket> getDefaultBucketValues() {
        return ImmutableMap.<String, TestBucket>builder()
                .put(SAMPLE_1_TST, new TestBucket("inactive", -1, "fallback inactive"))
                .build();
    }

    @Override
    public Map<String, String> getProvidedContext() {
        return null;
    }

    public ProctorResult determineBuckets(final Identifiers identifiers) {
        return super.determineBucketsInternal(identifiers, Collections.emptyMap());
    }
}
