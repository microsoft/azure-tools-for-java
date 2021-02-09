/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.mock;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class SparkLocalRunner {
    private String master;
    private String jobClassName;
    private List<String> jobArguments;

    private Logger log() {
        return LoggerFactory.getLogger(this.getClass());
    }

    public static void main(String[] args) {
        SparkLocalRunner localRunner = new SparkLocalRunner();

        localRunner.setArguments(args);
        localRunner.setUp();
        localRunner.runJobMain();
    }

    private void setArguments(String[] args) {
        // get master from `--master local[2]`
        master = args[0].split(" ")[1];

        // get job main class
        jobClassName = args[1];

        // get job arguments
        jobArguments = Arrays.asList(args).subList(2, args.length);
    }

    private void runJobMain() {

        log().info("HADOOP_HOME: " + System.getenv("HADOOP_HOME"));
        log().info("Hadoop user default directory: " + System.getProperty("user.dir"));

        try {
            final Class<?> jobClass = Class.forName(jobClassName);

            log().info("Run Spark Job: " + jobClass.getName());

            final Method jobMain = jobClass.getMethod("main", String[].class);

            final Object[] jobArgs = new Object[]{ jobArguments.toArray(new String[0]) };
            jobMain.invoke(null, jobArgs);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.getTargetException().printStackTrace();
        }

    }

    private void setUp() {

        new MockUp<FileSystem>() {

            @Mock
            public Class getFileSystemClass(Invocation invocation, String scheme, Configuration conf) throws IOException {
                if (scheme.toLowerCase().equals("jar")) {
                    throw new IOException("Unsupported file system scheme: jar");
                }

                return MockDfs.class;
            }

            @Mock
            public void checkPath(Path path) {}
        };

        System.setProperty("spark.master", master);
    }
}
