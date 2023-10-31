/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code.model;

import lombok.*;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class MethodIdentifier {
    private String className;
    private String methodName;
    private List<String> parameterTypes;
}
