/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
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
            public Class getFileSystemClass(Invocation invocation, String scheme, Configuration conf) {
                return MockDfs.class;
            }

            @Mock
            public void checkPath(Path path) {}
        };

        System.setProperty("spark.master", master);
    }
}
