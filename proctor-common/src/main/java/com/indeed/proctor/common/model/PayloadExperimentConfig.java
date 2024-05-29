package com.indeed.proctor.common.model;

import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Data
public class PayloadExperimentConfig {
    @Nullable private String priority;
    @Nonnull private List<String> namespace = Collections.emptyList();
}
