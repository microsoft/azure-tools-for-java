/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureSdkFeatureEntity {
    private String id;
    private String name;
    private String description;
    private String msdocs;
    private List<AzureSdkArtifactEntity> artifacts;

    public String toString() {
        return this.name;
    }

    public List<AzureSdkArtifactEntity> getClientArtifacts() {
        return null;
    }

    public List<AzureSdkArtifactEntity> getSpringArtifacts() {
        return artifacts;
    }

    public List<AzureSdkArtifactEntity> getManagementArtifacts() {
        return null;
    }
}
