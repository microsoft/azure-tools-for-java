/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.experiment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AssignmentResponse {
    @JsonProperty("Features")
    private List<String> features;
    @JsonProperty("Flights")
    private Map<String, String> flights;
    @JsonProperty("Configs")
    private List<Config> configs;
    @JsonProperty("ParameterGroups")
    private List<Object> parameterGroups;
    @JsonProperty("FlightingVersion")
    private int flightingVersion;
    @JsonProperty("ImpressionId")
    private String impressionId;
    @JsonProperty("AssignmentContext")
    private String assignmentContext;
    // debug info
    @JsonProperty("FlightingEnrichments")
    private Map<String, Object> flightingEnrichments;
    @Data
    public static class Config {
        @JsonProperty("Id")
        private String id;
        @JsonProperty("Parameters")
        private Map<String, String> parameters;
    }

    // debug info
    public static class FlightingEnrichment {
        @JsonProperty("NumberlineToFlightMap")
        private Map<String, Map<String, Flight>> numberlineToFlightMap;
        @JsonProperty("DiagnosticMessages")
        private List<String> diagnosticMessages;
    }

    // debug info
    public static class Flight {
        @JsonProperty("DeferredFilters")
        private String deferredFilters;
        @JsonProperty("HasDeferredFilters")
        private boolean hasDeferredFilters;
        @JsonProperty("VisibleToPartners")
        private boolean visibleToPartners;
        @JsonProperty("VisibleToLegacyPartners")
        private boolean visibleToLegacyPartners;
        @JsonProperty("OriginalHashBucket")
        private int originalHashBucket;
        @JsonProperty("HashBucket")
        private int hashBucket;
        @JsonProperty("FeatureFlags")
        private Map<String, String> featureFlags;
        @JsonProperty("Variants")
        private List<String> variants;
        @JsonProperty("Features")
        private List<String> features;
        @JsonProperty("IsStandardDefault")
        private boolean isStandardDefault;
        @JsonProperty("VariantAllocationId")
        private int variantAllocationId;
        @JsonProperty("FlightAllocationId")
        private int flightAllocationId;
        @JsonProperty("Numberline")
        private String numberline;
        @JsonProperty("AssignmentSource")
        private int assignmentSource;
        @JsonProperty("FlightName")
        private String flightName;
    }

}
