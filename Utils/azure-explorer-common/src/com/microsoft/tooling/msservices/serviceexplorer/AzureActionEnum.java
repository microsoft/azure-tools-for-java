/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import lombok.Getter;

public enum AzureActionEnum {

    REFRESH("Refresh", "Refreshing"),
    CREATE("Create", "Creating"),

    OPEN_IN_PORTAL("Open In Portal", "Opening in portal"),
    SHOW_PROPERTIES("Show Properties", "Showing properties"),

    START("Start", "Starting", 50, 1),
    STOP("Stop", "Stopping", 50, 2),
    RESTART("Restart", "Restarting", 50, 3),
    DELETE("Delete", "Deleting", 50, 4);

    @Getter
    private final String name;
    @Getter
    private final String doingName;
    @Getter
    private final Integer group;
    @Getter
    private final Integer priority;

    AzureActionEnum(String name, String doingName) {
        this(name, doingName, null, null);
    }

    AzureActionEnum(String name, String doingName, Integer group, Integer priority) {
        this.name = name;
        this.doingName = doingName;
        this.group = group;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return name();
    }

}
