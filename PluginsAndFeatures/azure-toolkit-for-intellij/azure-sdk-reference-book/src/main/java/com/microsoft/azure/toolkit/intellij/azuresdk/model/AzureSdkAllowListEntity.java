/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
public class AzureSdkAllowListEntity {

    @JsonProperty("Package")
    private String artifactId;
    @JsonProperty("GroupId")
    private String groupId;

    public String getPackageName() {
        return String.format("%s/%s", groupId, artifactId);
    }

}