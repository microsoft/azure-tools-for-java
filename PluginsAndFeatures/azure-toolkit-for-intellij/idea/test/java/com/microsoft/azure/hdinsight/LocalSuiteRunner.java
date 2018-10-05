package com.microsoft.azure.hdinsight;

import org.junit.runner.*;
import org.junit.runner.notification.Failure;

public class LocalSuiteRunner {
    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(SuiteTest.class);
        for(Failure failure : result.getFailures()) {
            // TODO: Not sure how we show the failure message
            System.out.println(failure.getMessage());
        }
    }
}
