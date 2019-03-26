package com.indeed.proctor.webapp.model;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.Map;

public enum AutopromoteTarget {
    UNKNOWN("unknown"),
    QA("qa"),
    QA_AND_PROD("qa-and-prod");

    final String name;

    AutopromoteTarget(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private static final Map<String, AutopromoteTarget> LOOKUP = Maps.uniqueIndex(
            ImmutableList.copyOf(AutopromoteTarget.values()), Functions.toStringFunction()
    );

    public static AutopromoteTarget fromName(String name) {
        if(name == null) {
            return UNKNOWN;
        } else {
            return LOOKUP.getOrDefault(name.toLowerCase(), UNKNOWN);
        }
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
