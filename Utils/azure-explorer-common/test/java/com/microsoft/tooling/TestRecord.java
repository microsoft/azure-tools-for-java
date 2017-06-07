package com.microsoft.tooling;

import java.util.LinkedList;

class TestRecord {
    public LinkedList<NetworkCallRecord> networkCallRecords;

    public LinkedList<String> variables;

    public TestRecord() {
        networkCallRecords = new LinkedList<>();
        variables = new LinkedList<>();
    }
}
