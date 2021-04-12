/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.entities;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Created by shch on 4/29/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthenticationError {
    @JsonProperty
    public String error;
    @JsonProperty
    public String error_description;

}
