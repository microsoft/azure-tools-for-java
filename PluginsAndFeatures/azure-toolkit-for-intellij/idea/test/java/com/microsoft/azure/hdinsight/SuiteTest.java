package com.microsoft.azure.hdinsight;

import com.microsoft.azure.hdinsight.spark.common.SubmissionTableModelTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        //add Test class to here for local suite test
        SubmissionTableModelTest.class
})
public class SuiteTest {
}
