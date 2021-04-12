/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;
import com.microsoft.azure.SubResource;

/**
 * Data Lake Analytics firewall rule information.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class FirewallRule extends SubResource {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The start IP address for the firewall rule. This can be either ipv4 or
         * ipv6. Start and End should be in the same protocol.
         */
        @JsonProperty(value = "startIpAddress", access = JsonProperty.Access.WRITE_ONLY)
        private String startIpAddress;

        /**
         * The end IP address for the firewall rule. This can be either ipv4 or
         * ipv6. Start and End should be in the same protocol.
         */
        @JsonProperty(value = "endIpAddress", access = JsonProperty.Access.WRITE_ONLY)
        private String endIpAddress;
    }

    /**
     * The properties
     */
    @JsonProperty(value = "properties")
    private Properties properties;

    /**
     * The resource name.
     */
    @JsonProperty(value = "name", access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    /**
     * The resource type.
     */
    @JsonProperty(value = "type", access = JsonProperty.Access.WRITE_ONLY)
    private String type;

    /**
     * Get the startIpAddress value.
     *
     * @return the startIpAddress value
     */
    public String startIpAddress() {
        return this.properties == null ? null : properties.startIpAddress;
    }

    /**
     * Get the endIpAddress value.
     *
     * @return the endIpAddress value
     */
    public String endIpAddress() {
        return this.properties == null ? null : properties.endIpAddress;
    }

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Get the type value.
     *
     * @return the type value
     */
    public String type() {
        return this.type;
    }

}
