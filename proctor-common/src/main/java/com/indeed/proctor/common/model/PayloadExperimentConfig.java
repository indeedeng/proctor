package com.indeed.proctor.common.model;

import lombok.Builder;
import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Data
@Builder
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
}
