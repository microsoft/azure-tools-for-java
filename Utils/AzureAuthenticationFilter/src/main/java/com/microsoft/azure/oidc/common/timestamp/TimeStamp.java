/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.timestamp;

public interface TimeStamp extends Comparable<TimeStamp> {

    Long getTime();

    boolean equals(Object object);

    int hashCode();

}
