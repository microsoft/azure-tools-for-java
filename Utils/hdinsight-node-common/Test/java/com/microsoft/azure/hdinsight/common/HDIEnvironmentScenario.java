/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.microsoft.azuretools.authmanage.Environment;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

import static org.junit.Assert.assertEquals;

public class HDIEnvironmentScenario {
    private HDIEnvironment hdiEnvironment;

    @Given("send environment string '(.+)'$")
    public void getAzureEnvironment(String environmentMessage) {
        Environment environment = null;
        switch (environmentMessage) {
            case "global":
                environment = Environment.GLOBAL;
                break;
            case "china":
                environment = Environment.CHINA;
                break;
            case "germany":
                environment = Environment.GERMAN;
                break;
            case "us_government":
                environment = Environment.US_GOVERNMENT;
        }
        hdiEnvironment = new HDIEnvironment(environment);
    }

    @Then("^the portal url '(.+)', HDInsight url '(.+)', blob full name '(.+)'$")
    public void checkPortalUrl(String portalUrl, String hdiUrl, String blobFullName) throws Throwable {
        assertEquals(portalUrl, hdiEnvironment.getPortal());
        assertEquals(hdiUrl, hdiEnvironment.getClusterConnectionFormat());
        assertEquals(blobFullName, hdiEnvironment.getBlobFullNameFormat());
    }
}
