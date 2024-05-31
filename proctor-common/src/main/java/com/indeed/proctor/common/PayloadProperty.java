package com.indeed.proctor.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayloadProperty {
    private String testName;
    private JsonNode value;
}
