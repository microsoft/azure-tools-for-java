/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui.commoncontrols;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

import com.microsoft.azuretools.core.utils.AzureAbstractHandler;

public class DropDownHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        if (event.getTrigger() != null && (event.getTrigger() instanceof Event)) {
            pullDropdown((Event) event.getTrigger());
        }
        return null;
    }

    public void pullDropdown(Event event) {
        Widget widget = event.widget;
        if (widget instanceof ToolItem) {
            ToolItem toolItem = (ToolItem) widget;
            Listener[] listeners = toolItem.getListeners(SWT.Selection);
            if (listeners.length > 0) {
                Listener listener = listeners[0];
                Event eve = new Event();
                eve.type = SWT.Selection;
                eve.widget = toolItem;
                eve.detail = SWT.DROP_DOWN;
                eve.x = toolItem.getBounds().x;
                eve.y = toolItem.getBounds().height;
                listener.handleEvent(eve);
            }
        }
    }
}
