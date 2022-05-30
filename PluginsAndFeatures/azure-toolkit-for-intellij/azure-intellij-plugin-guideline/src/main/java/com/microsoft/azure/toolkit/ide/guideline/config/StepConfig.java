/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline.config;

import lombok.Data;

@Data
public class StepConfig {
    private String name;
    private String title;
    private String description;
    private String task;
}
