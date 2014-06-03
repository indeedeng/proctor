package com.indeed.proctor.service.var;

import com.indeed.proctor.service.BadRequestException;

/**
 * Thrown when a converter has some error during conversion that is unrecoverable.
 *
 * Contains information about specifically which variable failed with what value. getMessage() is overridden to
 * include this information.
 *
 * Since ValueConverters do not have access to detailed information, the caller should catch this, set those values,
 * and rethrow the error.
 */
public class ConversionException extends BadRequestException {
    private String varName;
    private String rawValue;
    private String type;

    public ConversionException(final String details) {
        super(details);
    }

    public String getMessage() {
        return String.format("Could not convert raw value '%s' to type '%s' for context variable '%s': %s",
                rawValue, type, varName, super.getMessage());
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
