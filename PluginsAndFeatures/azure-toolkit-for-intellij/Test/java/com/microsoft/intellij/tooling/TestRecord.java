/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.tooling;

import java.util.LinkedList;

class TestRecord {
    public LinkedList<NetworkCallRecord> networkCallRecords;

    public LinkedList<String> variables;

    public TestRecord() {
        networkCallRecords = new LinkedList<>();
        variables = new LinkedList<>();
    }
}
