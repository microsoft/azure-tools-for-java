/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.mockito.Mockito;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HDIEnvironmentScenario {
    private AzureManager azureManagerMock = mock(AzureManager.class, Mockito.RETURNS_MOCKS);
    private HDIEnvironment hdiEnvironment;

    @Given("send environment string '(.+)'$")
    public void getAzureEnvironment(String environmentMessage) {
        switch (environmentMessage) {
            case "global":
                when(azureManagerMock.getEnvironment()).thenReturn(Environment.GLOBAL);
                break;
            case "china":
                when(azureManagerMock.getEnvironment()).thenReturn(Environment.CHINA);
                break;
            case "germany":
                when(azureManagerMock.getEnvironment()).thenReturn(Environment.GERMAN);
                break;
            case "us_government":
                when(azureManagerMock.getEnvironment()).thenReturn(Environment.US_GOVERNMENT);
        }
        Environment environment = azureManagerMock.getEnvironment();
        hdiEnvironment = new HDIEnvironment(environment);
    }

    @Then("^the portal url '(.+)', HDInsight url '(.+)', blob full name '(.+)'$")
    public void checkPortalUrl(String portalUrl, String hdiUrl, String blobFullName) throws Throwable {
        assertEquals(portalUrl, hdiEnvironment.getPortal());
        assertEquals(hdiUrl, hdiEnvironment.getClusterConnectionFormat());
        assertEquals(blobFullName, hdiEnvironment.getBlobFullNameFormat());
    }
}
