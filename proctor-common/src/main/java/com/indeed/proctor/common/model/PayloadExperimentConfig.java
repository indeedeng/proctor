package com.indeed.proctor.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class PayloadExperimentConfig {
    @Nullable private String priority;
    @Nonnull private List<String> namespaces = Collections.emptyList();

    @Override
    public String toString() {
        return "PayloadExperimentConfig{"
                + "priority='"
                + priority
                + '\''
                + ", namespaces="
                + namespaces
                + '}';
    }

    public boolean isHigherPriorityThan(final PayloadExperimentConfig otherPayloadConfig) {
        final long currPriority =
                this.getPriority() == null ? Long.MIN_VALUE : Long.parseLong(this.getPriority());
        final long otherPriority =
                otherPayloadConfig == null || otherPayloadConfig.getPriority() == null
                        ? Long.MIN_VALUE
                        : Long.parseLong(otherPayloadConfig.getPriority());
        return currPriority < otherPriority;
    }
}
