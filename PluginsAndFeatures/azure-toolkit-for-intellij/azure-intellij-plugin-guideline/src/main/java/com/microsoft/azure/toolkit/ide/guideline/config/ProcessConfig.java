/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline.config;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProcessConfig {
    private String name;
    private String title;
    private String description;
    private String repository;
    private List<PhaseConfig> phases;
    private Map<String, String> context;
}
