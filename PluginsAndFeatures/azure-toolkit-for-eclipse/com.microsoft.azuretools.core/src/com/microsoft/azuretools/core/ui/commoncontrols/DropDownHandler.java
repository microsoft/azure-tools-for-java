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
