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

package com.microsoft.tooling.msservices.serviceexplorer.azure.function;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

public class SubFunctionNode extends Node {

    private static final String SUB_FUNCTION_ICON_PATH = "azure-function-trigger-small.png";

    public SubFunctionNode(String id, String name, Node parent) {
        super(id, name, parent, SUB_FUNCTION_ICON_PATH);
    }

    public SubFunctionNode(String id, String name, Node parent, boolean delayActionLoading) {
        super(id, name, parent, SUB_FUNCTION_ICON_PATH, delayActionLoading);
    }

    @Override
    protected void loadActions() {
        addAction("Trigger",
                new WrappedTelemetryNodeActionListener(FUNCTION, TRIGGER_FUNCTION, new NodeActionListener() {
                    @Override
                    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                        // Todo: Implement trigger method according to trigger type
                    }
                }));
        addAction("Enable",
                new WrappedTelemetryNodeActionListener(FUNCTION, ENABLE_FUNCTION, new NodeActionListener() {
                    @Override
                    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                        // Todo: Implement enable method according to trigger type
                    }
                }));
        addAction("Disable",
                new WrappedTelemetryNodeActionListener(FUNCTION, DISABLE_FUNCTION, new NodeActionListener() {
                    @Override
                    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                        // Todo: Implement disable method according to trigger type
                    }
                }));
    }
}
