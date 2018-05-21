/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * The parameters used to create a new firewall rule while creating a new Data
 * Lake Analytics account.
 */
@JsonFlatten
public class CreateFirewallRuleWithAccountParameters {
    /**
     * The unique name of the firewall rule to create.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    /**
     * The start IP address for the firewall rule. This can be either ipv4 or
     * ipv6. Start and End should be in the same protocol.
     */
    @JsonProperty(value = "properties.startIpAddress", required = true)
    private String startIpAddress;

    /**
     * The end IP address for the firewall rule. This can be either ipv4 or
     * ipv6. Start and End should be in the same protocol.
     */
    @JsonProperty(value = "properties.endIpAddress", required = true)
    private String endIpAddress;

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     * @return the CreateFirewallRuleWithAccountParameters object itself.
     */
    public CreateFirewallRuleWithAccountParameters withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the startIpAddress value.
     *
     * @return the startIpAddress value
     */
    public String startIpAddress() {
        return this.startIpAddress;
    }

    /**
     * Set the startIpAddress value.
     *
     * @param startIpAddress the startIpAddress value to set
     * @return the CreateFirewallRuleWithAccountParameters object itself.
     */
    public CreateFirewallRuleWithAccountParameters withStartIpAddress(String startIpAddress) {
        this.startIpAddress = startIpAddress;
        return this;
    }

    /**
     * Get the endIpAddress value.
     *
     * @return the endIpAddress value
     */
    public String endIpAddress() {
        return this.endIpAddress;
    }

    /**
     * Set the endIpAddress value.
     *
     * @param endIpAddress the endIpAddress value to set
     * @return the CreateFirewallRuleWithAccountParameters object itself.
     */
    public CreateFirewallRuleWithAccountParameters withEndIpAddress(String endIpAddress) {
        this.endIpAddress = endIpAddress;
        return this;
    }

}
