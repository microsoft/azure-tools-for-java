/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.core.utils;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.swt.widgets.Event;

import com.microsoft.azuretools.telemetry.AppInsightsClient;

public abstract class AzureAbstractHandler extends AbstractHandler {

	public abstract Object onExecute(ExecutionEvent event) throws ExecutionException;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final Map<String, String> properties = new HashMap<>();
		try {
			properties.put("CategoryId", event.getCommand().getCategory().getId());
			properties.put("Category", event.getCommand().getCategory().getName());
			properties.put("CommandId", event.getCommand().getId());
			properties.put("Text", event.getCommand().getName());
			
			AppInsightsClient.createByType(AppInsightsClient.EventType.Action, event.getCommand().getName(),	null, properties);
		} catch (NotDefinedException ignore) {
		}

		return onExecute(event);
	}
}
