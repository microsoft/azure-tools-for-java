/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
