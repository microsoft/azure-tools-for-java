/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun;

import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.IntelliJFunctionContext;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionRunModel extends IntelliJFunctionContext {

    private Artifact artifact;
    private String debugOptions;
    private String stagingFolder;
    private String funcPath;
    private String hostJsonPath;
    private String localSettingsJsonPath;
    private String functionHostArguments;
}
