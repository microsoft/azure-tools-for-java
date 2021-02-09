/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.logger;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

public class UILoggerAppenderScenario {
    private Logger l = Logger.getLogger("UILoggerAppenderTest");
    private UILoggerAppender appenderMock = mock(UILoggerAppender.class, CALLS_REAL_METHODS);
    private ArgumentCaptor<LoggingEvent> loggingEventCaptor = ArgumentCaptor.forClass(LoggingEvent.class);;

    @Before
    public void setUp() {
        l.addAppender(appenderMock);
    }

    @Given("^send ERROR '(.+)'$")
    public void sendErrorLog(String message) throws Throwable {
        doNothing().when(appenderMock).append(loggingEventCaptor.capture());

        l.error(message);
    }

    @Then("^get the append call with event message '(.+)'$")
    public void checkAppendParameter(String expectedMessage) throws Throwable {
        assertEquals(expectedMessage, loggingEventCaptor.getValue().getRenderedMessage());
    }
}
