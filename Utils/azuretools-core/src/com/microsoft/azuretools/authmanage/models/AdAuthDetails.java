/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.models;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Created by shch on 10/15/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdAuthDetails {

    @JsonProperty
    private String accountEmail;

    public Map<String, List<String>> getTidToSidsMap() {
        return tidToSidsMap;
    }

    public void setTidToSidsMap(Map<String, List<String>> tidToSidsMap) {
        this.tidToSidsMap = tidToSidsMap;
    }

    @JsonProperty
    private Map<String, List<String>> tidToSidsMap;

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }
}
