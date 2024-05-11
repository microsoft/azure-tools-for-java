/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.components;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.core.utils.AccessibilityUtils;
import com.microsoft.azuretools.telemetry.TelemetryProperties;

public abstract class AzureTitleAreaDialogWrapper extends TitleAreaDialog implements AzureDialogProtertiesHelper, TelemetryProperties{
    public AzureTitleAreaDialogWrapper(Shell parentShell) {
        super(parentShell);
    }

	@Override
	public void setMessage(String newMessage) {
		// TODO Auto-generated method stub
		super.setMessage(newMessage);
        Optional.ofNullable(getMessageLabel()).ifPresent(label -> AccessibilityUtils.addAccessibilityNameForUIComponent(label, "message"));
	}

	@Nullable
    protected Control getMessageLabel() {
    	 try {
             Field field = TitleAreaDialog.class.getDeclaredField("messageLabel");
             field.setAccessible(true);
             return (Control) field.get(this);
         } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
             return null;
         }
    }
    
    @Override
    protected void okPressed() {
        sentTelemetry(OK);
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        sentTelemetry(CANCEL);
        super.cancelPressed();
    }

    @Override
    public Map<String, String> toProperties() {
        return new HashMap<>();
    }
}
