/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.jfr;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CommandOutput {
    private String output;
    private String error;
    private int exitCode;
}
