/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.appservice.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FunctionArtifactModel {
    private String groupId;
    private String artifactId;
    private String version;
    private String packageName;

    public static FunctionArtifactModel getDefaultFunctionArtifactModel() {
        FunctionArtifactModel model = new FunctionArtifactModel();
        model.setArtifactId("FirstProject");
        model.setGroupId("com.example");
        model.setPackageName("com.example");
        model.setVersion("0.1.0-SNAPSHOT");
        return model;
    }
}
