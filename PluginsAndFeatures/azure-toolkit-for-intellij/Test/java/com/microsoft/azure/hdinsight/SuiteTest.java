/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

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
