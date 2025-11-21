/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.jcraft.jsch.Session;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;

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
