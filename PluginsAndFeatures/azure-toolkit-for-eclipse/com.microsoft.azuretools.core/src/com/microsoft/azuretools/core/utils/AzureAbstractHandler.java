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

package com.microsoft.azuretools.core.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.telemetry.AppInsightsClient;

public abstract class AzureAbstractHandler extends AbstractHandler {

    public abstract Object onExecute(ExecutionEvent event) throws ExecutionException;

    public void sendTelemetryOnAction(ExecutionEvent event, final String action, Map<String, String> extraInfo) {
        final Map<String, String> properties = new HashMap<>();
        String handlerName = null;
        try {
            final Command cmd = event.getCommand();
            handlerName = this.getClass().getSimpleName();

            final Collection<?> callingMenus = HandlerUtil.getActiveMenus(event);
            boolean fromProjectMenu = callingMenus != null
                    && callingMenus.contains("org.eclipse.ui.navigator.ProjectExplorer#PopupMenu");
            properties.put("FromProjectMenu", String.valueOf(fromProjectMenu));

            if (cmd != null) {
                properties.put("CategoryId", cmd.getCategory().getId());
                properties.put("Category", cmd.getCategory().getName());
                properties.put("CommandId", cmd.getId());
                properties.put("Text", cmd.getName());
                if (null == handlerName || handlerName.isEmpty()) {
                    handlerName = cmd.getName();
                }
            }
            if (extraInfo != null) {
                properties.putAll(extraInfo);
            }
            AppInsightsClient.createByType(AppInsightsClient.EventType.Action, handlerName, action, properties);
        } catch (NotDefinedException ignore) {
        }
    }

    public void sendTelemetryOnSuccess(ExecutionEvent event, Map<String, String> extraInfo) {
        sendTelemetryOnAction(event, "Success", extraInfo);
    }

    public void sendTelemetryOnException(ExecutionEvent event, Throwable e) {
        Map<String, String> extraInfo = new HashMap<>();
        extraInfo.put("ErrorMessage", e.getMessage());
        this.sendTelemetryOnAction(event, "Exception", extraInfo);
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        sendTelemetryOnAction(event, "Execute", null);
        return onExecute(event);
    }
}
