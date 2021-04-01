/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.mock;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class SparkLocalConsoleMockFsAgent {
    public static void premain(String arg) {
        SparkLocalConsoleMockFsAgent agent = new SparkLocalConsoleMockFsAgent();
        agent.setUp();
    }

    private void setUp() {
        new MockUp<FileSystem>() {
            @Mock
            public Class getFileSystemClass(Invocation invocation, String scheme, Configuration conf) {
                return MockDfs.class;
            }

            @Mock
            public void checkPath(Path path) {}
        };
    }
}
