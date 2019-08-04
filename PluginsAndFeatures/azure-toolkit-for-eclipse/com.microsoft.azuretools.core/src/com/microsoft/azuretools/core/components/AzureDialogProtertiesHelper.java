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
package com.microsoft.azuretools.core.components;

import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryProperties;

public interface AzureDialogProtertiesHelper {

	default void addCancelTelemetryProperties(final Map<String, String> properties) {
		
	}
	
	default void addOKTelemetryProperties(final Map<String, String> properties) {
		if(!(this instanceof Dialog)) return;
		Control[] controls = ((Dialog) this).getShell().getChildren();
		java.util.List<Control> controlsList = new ArrayList<Control>();
		for(Control control : controls) {
			controlsList = getAllControls(control);
			for(Control c : controlsList) {
				if(c instanceof Button) {
					Button button = (Button) c;
					String btnInfoString = button.getText();
					if (btnInfoString == null || btnInfoString.trim().isEmpty()) {
						btnInfoString = button.getLocation() == null ? "" : String.valueOf(button.getLocation());
					}
					if((c.getStyle() & SWT.CHECK) != 0) {
						properties.put("JCheckBox." + btnInfoString + ".Selected", String.valueOf(button.getSelection()));
					}
					else if((c.getStyle() & SWT.RADIO) != 0) {
						properties.put("JRadioButton." + btnInfoString + ".Selected", String.valueOf(button.getSelection()));
					}
				}
				if(c instanceof Combo) {
					Combo combo = (Combo) c;
					String comboInfoString = combo.getLocation() == null ? "" : String.valueOf(combo.getLocation());
					int idx = combo.getSelectionIndex();
					String comboItemString = null;
					try {
						comboItemString = combo.getItem(idx);
					} catch (Exception ex) {
						comboItemString = "";
					}
					properties.put("JComboBox." + comboInfoString + ".Selected", comboItemString);
				}
			}
		}
		return;
	}
	
	default java.util.List<Control> getAllControls(Control c) {
		if(!(this instanceof Dialog)) return null;
		java.util.List<Control> controls = new ArrayList<Control>();
		controls.add(c);
		if(c instanceof Composite) {
			Control[] children = ((Composite) c).getChildren();
			for(Control child : children) {
				controls.addAll(getAllControls(child));
			}
		}
		return controls;
	}
	
	default void sentTelemetry(int code) {
		if(!(this instanceof Dialog)) return;
		final Map<String, String> properties = new HashMap<>();
		String action = "";
		
		switch (code) {
		case org.eclipse.jface.window.Window.OK:
			addOKTelemetryProperties(properties);
			action = "OK";
			break;
			
		case org.eclipse.jface.window.Window.CANCEL:
			addCancelTelemetryProperties(properties);
			action = "CANCEL";
			break;

		default:
			return;
		}

		properties.put("Window", ((Dialog) this).getClass().getSimpleName());
		properties.put("Title", ((Dialog) this).getShell().getText());
		if(this instanceof TelemetryProperties) {
			properties.putAll(((TelemetryProperties) this).toProperties());
		}
		AppInsightsClient.createByType(AppInsightsClient.EventType.Dialog, this.getClass().getSimpleName(), action, properties);
		EventUtil.logEvent(EventType.info, TelemetryConstants.DIALOG, action, properties);
	}
}
