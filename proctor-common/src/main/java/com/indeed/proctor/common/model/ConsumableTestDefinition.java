package com.indeed.proctor.common.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.ProctorUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Models a single test
 *
 * @author ketan
 */
public class ConsumableTestDefinition {
    @Nonnull private Map<String, Object> constants = Collections.emptyMap();
    private String version;
    @Nullable private String salt;
    @Nullable private String rule;
    @Nonnull private List<TestBucket> buckets = Collections.emptyList();
    @Nonnull private List<Allocation> allocations = Collections.emptyList();
    private boolean silent = false;

    @Nonnull private TestType testType;
    @Nullable private String description;
    @Nonnull private List<String> metaTags = Collections.emptyList();

    /** @see TestDefinition#getDependsOn() */
    @Nullable private TestDependency dependsOn;

    private boolean isDynamic = false;
    private boolean evaluteForIncognitoUsers = false;

    public ConsumableTestDefinition() {
        /* intentionally empty */
    }

    /**
     * @deprecated Use {@link #fromTestDefinition(TestDefinition)} and {@link
     *     TestDefinition#builder()}
     */
    @Deprecated
    public ConsumableTestDefinition(
            final String version,
            @Nullable final String rule,
            @Nonnull final TestType testType,
            @Nullable final String salt,
            @Nonnull final List<TestBucket> buckets,
            @Nonnull final List<Allocation> allocations,
            @Nonnull final Map<String, Object> constants,
            @Nullable final String description) {
        this(
                version,
                rule,
                testType,
                salt,
                buckets,
                allocations,
                false,
                constants,
                description,
                Collections.emptyList());
    }

    /**
     * @deprecated Use {@link #fromTestDefinition(TestDefinition)} and {@link
     *     TestDefinition#builder()}
     */
    @Deprecated
    public ConsumableTestDefinition(
            final String version,
            @Nullable final String rule,
            @Nonnull final TestType testType,
            @Nullable final String salt,
            @Nonnull final List<TestBucket> buckets,
            @Nonnull final List<Allocation> allocations,
            final boolean silent,
            @Nonnull final Map<String, Object> constants,
            @Nullable final String description) {
        this(
                version,
                rule,
                testType,
                salt,
                buckets,
                allocations,
                silent,
                constants,
                description,
                Collections.emptyList());
    }

    /**
     * @deprecated Use {@link #fromTestDefinition(TestDefinition)} and {@link
     *     TestDefinition#builder()}
     */
    @Deprecated
    public ConsumableTestDefinition(
            final String version,
            @Nullable final String rule,
            @Nonnull final TestType testType,
            @Nullable final String salt,
            @Nonnull final List<TestBucket> buckets,
            @Nonnull final List<Allocation> allocations,
            final boolean silent,
            @Nonnull final Map<String, Object> constants,
            @Nullable final String description,
            @Nonnull final List<String> metaTags) {
        this.constants = constants;
        this.version = version;
        this.salt = salt;
        this.rule = rule;
        this.buckets = buckets;
        this.allocations = allocations;
        this.silent = silent;
        this.testType = testType;
        this.description = description;
        this.metaTags = metaTags;
    }

    // intentionally private to avoid creating deprecated constructors
    private ConsumableTestDefinition(
            final String version,
            @Nullable final String rule,
            @Nonnull final TestType testType,
            @Nullable final String salt,
            @Nonnull final List<TestBucket> buckets,
            @Nonnull final List<Allocation> allocations,
            final boolean silent,
            @Nonnull final Map<String, Object> constants,
            @Nullable final String description,
            @Nonnull final List<String> metaTags,
            @Nullable final TestDependency dependsOn,
            final boolean bypassIncognito) {
        this.constants = constants;
        this.version = version;
        this.salt = salt;
        this.rule = rule;
        this.buckets = buckets;
        this.allocations = allocations;
        this.silent = silent;
        this.testType = testType;
        this.description = description;
        this.metaTags = metaTags;
        this.dependsOn = dependsOn;
        this.evaluteForIncognitoUsers = bypassIncognito;
    }

    @Nonnull
    public Map<String, Object> getConstants() {
        return constants;
    }

    public void setConstants(@Nonnull final Map<String, Object> constants) {
        this.constants = constants;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    @Nullable
    public String getRule() {
        return rule;
    }

    public void setRule(@Nullable final String rule) {
        this.rule = rule;
    }

    @Nullable
    public String getSalt() {
        return salt;
    }

    public void setSalt(@Nullable final String salt) {
        this.salt = salt;
    }

    @Nonnull
    public List<TestBucket> getBuckets() {
        return buckets;
    }

    public void setBuckets(@Nonnull final List<TestBucket> buckets) {
        this.buckets = buckets;
    }

    @Nonnull
    public List<Allocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(@Nonnull final List<Allocation> allocations) {
        this.allocations = allocations;
    }

    public void setSilent(final boolean silent) {
        this.silent = silent;
    }

    public boolean getSilent() {
        return silent;
    }

    public void setDynamic(final boolean dynamic) {
        this.isDynamic = dynamic;
    }

    public boolean getDynamic() {
        return isDynamic;
    }

    @Nonnull
    public TestType getTestType() {
        return testType;
    }

    public void setTestType(@Nonnull final TestType testType) {
        this.testType = testType;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable final String description) {
        this.description = description;
    }

    /** metaTags allow to group and filter tests. */
    @Nonnull
    public List<String> getMetaTags() {
        return this.metaTags;
    }

    public void setMetaTags(final List<String> metaTags) {
        this.metaTags = metaTags;
    }

    /** @see TestDefinition#getDependsOn() */
    @Nullable
    public TestDependency getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(@Nullable final TestDependency dependsOn) {
        this.dependsOn = dependsOn;
    }

    public void setEvaluteForIncognitoUsers(final boolean evaluteForIncognitoUsers) {
        this.evaluteForIncognitoUsers = evaluteForIncognitoUsers;
    }

    public boolean getEvaluteForIncognitoUsers() {
        return evaluteForIncognitoUsers;
    }

    @Nonnull
    public static ConsumableTestDefinition fromTestDefinition(@Nonnull final TestDefinition td) {
        final Map<String, Object> specialConstants = td.getSpecialConstants();

        final List<String> ruleComponents = Lists.newArrayList();
        //noinspection unchecked
        final List<String> countries = (List<String>) specialConstants.get("__COUNTRIES");
        if (countries != null) {
            ruleComponents.add("proctor:contains(__COUNTRIES, country)");
        }
        final String rawRule = ProctorUtils.removeElExpressionBraces(td.getRule());
        if (!StringUtils.isBlank(rawRule)) {
            ruleComponents.add(rawRule);
        }

        final String rule;
        if (ruleComponents.isEmpty()) {
            rule = null;
        } else {
            rule = "${" + String.join(" && ", ruleComponents) + '}';
        }

        final List<Allocation> allocations = td.getAllocations();
        for (final Allocation alloc : allocations) {
            final String rawAllocRule = ProctorUtils.removeElExpressionBraces(alloc.getRule());
            if (StringUtils.isBlank(rawAllocRule)) {
                alloc.setRule(null);
            } else {
                // ensure that all rules in the generated test-matrix are wrapped in "${" ... "}"
                if (!(rawAllocRule.startsWith("${") && rawAllocRule.endsWith("}"))) {
                    final String newAllocRule = "${" + rawAllocRule + "}";
                    alloc.setRule(newAllocRule);
                }
            }
        }

        final Map<String, Object> constants = Maps.newLinkedHashMap();
        constants.putAll(td.getConstants());
        constants.putAll(specialConstants);

        return new ConsumableTestDefinition(
                td.getVersion(),
                rule,
                td.getTestType(),
                td.getSalt(),
                td.getBuckets(),
                allocations,
                td.getSilent(),
                constants,
                td.getDescription(),
                td.getMetaTags(),
                td.getDependsOn(),
                td.getEvaluteForIncognitoUsers());
    }
}
