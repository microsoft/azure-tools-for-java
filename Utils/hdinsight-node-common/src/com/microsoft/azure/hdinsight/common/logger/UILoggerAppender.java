/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common.logger;

import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.UIHelper;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Wrap UIHelper logger interface as an log4j appender
 */
public class UILoggerAppender extends AppenderSkeleton {
    @Override
    public void append(LoggingEvent event) {
        UIHelper uiHelper = DefaultLoader.getUIHelper();

        if (event.getLevel() == Level.ERROR) {
            if (uiHelper != null) {
                uiHelper.logError(
                        event.getRenderedMessage(),
                        event.getThrowableInformation().getThrowable());
            }
        }

        //TODO: To add other levels log functions with extending UIHelper interface API
    }

    @Override
    public void close() { }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
