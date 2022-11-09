/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.experiment;

import lombok.Getter;

public enum FeatureFlag {
    GETTING_STARTED_UI("showNewUI"),
    ASSIGNMENT_CONTEXT("assignmentContext");
    @Getter
    private final String flagName;

    FeatureFlag(String flagName) {
        this.flagName = flagName;
    }
}
