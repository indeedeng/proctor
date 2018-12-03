package com.indeed.proctor.common.dynamic;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author due
 */

public interface PatternSupplier {
    @Nonnull
    List<Pattern> getPatterns(final List<String> tags);
}
