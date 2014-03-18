package com.indeed.proctor.webapp.db;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.util.Map;

public enum Environment {
    WORKING("trunk", 0),
    QA("qa", 1),
    PRODUCTION("production", 2);

    private final String name;
    private final int precedence;
    private Environment(final String name, final int precedence) {
        this.name = name;
        this.precedence = precedence;
    }

    public String getName() {
        return name;
    }

    public int getPrecedence() {
        return precedence;
    }

    private static final Map<String, Environment> LOOKUP = Maps.uniqueIndex(ImmutableList.copyOf(Environment.values()), Functions.toStringFunction());
    public static Environment fromName(String name) {
        if(name == null) {
            return null;
        } else {
            return LOOKUP.get(name.toLowerCase());
        }
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
