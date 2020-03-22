/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * The parameters used to update a firewall rule.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateFirewallRuleParameters {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The start IP address for the firewall rule. This can be either ipv4 or
         * ipv6. Start and End should be in the same protocol.
         */
        @JsonProperty(value = "startIpAddress")
        private String startIpAddress;

        /**
         * The end IP address for the firewall rule. This can be either ipv4 or
         * ipv6. Start and End should be in the same protocol.
         */
        @JsonProperty(value = "endIpAddress")
        private String endIpAddress;
    }

    /**
     * The properties
     */
    @JsonProperty(value = "properties")
    private Properties properties;

    /**
     * Get the startIpAddress value.
     *
     * @return the startIpAddress value
     */
    public String startIpAddress() {
        return this.properties == null ? null : properties.startIpAddress;
    }

    /**
     * Set the startIpAddress value.
     *
     * @param startIpAddress the startIpAddress value to set
     * @return the UpdateFirewallRuleParameters object itself.
     */
    public UpdateFirewallRuleParameters withStartIpAddress(String startIpAddress) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.startIpAddress = startIpAddress;
        return this;
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
     * Set the endIpAddress value.
     *
     * @param endIpAddress the endIpAddress value to set
     * @return the UpdateFirewallRuleParameters object itself.
     */
    public UpdateFirewallRuleParameters withEndIpAddress(String endIpAddress) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.endIpAddress = endIpAddress;
        return this;
    }

}
