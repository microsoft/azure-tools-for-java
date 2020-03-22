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
package com.microsoft.azuretools.azureexplorer.editors;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;

public class FileEditorVirtualNode<T extends EditorPart> extends Node implements TelemetryProperties {
    private T editorPart;

    public FileEditorVirtualNode(final T t, final String name) {
        super(t.getClass().getSimpleName(), name);
        this.editorPart = t;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        if (editorPart instanceof TelemetryProperties) {
            properties.putAll(((TelemetryProperties) editorPart).toProperties());
        }
        return properties;
    }

    protected Action createPopupAction(final String actionName) {
        return new Action(actionName) {
            @Override
            public void run(){
                final NodeAction nodeAction = getNodeActionByName(actionName);
                if(nodeAction != null){
                    nodeAction.fireNodeActionEvent();
                }
            }
        };
    }

    protected void doAction(final String actionName){
        final NodeAction nodeAction = getNodeActionByName(actionName);
        if(nodeAction != null){
            nodeAction.fireNodeActionEvent();
        }
    }
}
