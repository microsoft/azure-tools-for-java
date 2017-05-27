package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import org.junit.runner.*;
import org.junit.runner.notification.Failure;

public class LocalSuiteRunner {
    public static void main(String[] args) {
        Result result = JUnitCore.runClasses(SuiteTest.class);
        for(Failure failure : result.getFailures()) {
            System.out.println(failure.getMessage());
        }
    }
}

