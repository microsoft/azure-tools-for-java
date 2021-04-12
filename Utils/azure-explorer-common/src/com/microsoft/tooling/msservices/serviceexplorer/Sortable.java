/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import org.apache.commons.lang3.StringUtils;

public interface Sortable {

    int HIGH_PRIORITY = 50;
    int DEFAULT_PRIORITY = 100;
    int LOW_PRIORITY = 200;

    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    static int compare(final Sortable first, final Sortable second) {
        return first.getPriority() == second.getPriority() ?
                StringUtils.compare(first.toString(), second.toString()) : first.getPriority() - second.getPriority();
    }
}
