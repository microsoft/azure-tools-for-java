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
package com.microsoft.azure.hdinsight.spark.common;

import com.jcraft.jsch.Session;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;

import static org.junit.Assert.assertEquals;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SparkBatchDebugSessionScenario {
    private SparkBatchDebugSession debugSessionMock =
            mock(SparkBatchDebugSession.class, CALLS_REAL_METHODS);
    private Session jschSessionMock = mock(Session.class, CALLS_REAL_METHODS);

    @Before
    public void setUp() {
        when(debugSessionMock.getPortForwardingSession()).thenReturn(jschSessionMock);
    }

    @Then("^parsing local port from getting Port Forwarding Local result '(.+)' with host '(.+)' and (\\d+) should get local port (\\d+)$")
    public void checkGetForwardedLocalPortResult(
            String forwardingMock,
            String remoteHost,
            int remotePort,
            int expectedPort) throws Throwable{
        when(jschSessionMock.getPortForwardingL()).thenReturn(new String[] { forwardingMock });

        assertEquals(expectedPort, debugSessionMock.getForwardedLocalPort(remoteHost, remotePort));
    }
}
