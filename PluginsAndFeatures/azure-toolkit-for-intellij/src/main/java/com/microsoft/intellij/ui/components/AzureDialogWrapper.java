/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.components;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.util.AzureLoginHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by juniwang on 4/19/2017.
 * Subclass of DialogWrapper. Do some common implementation here like the telemetry.
 */
public abstract class AzureDialogWrapper extends DialogWrapper implements TelemetryProperties {
    protected static final int HELP_CODE = -1;
    private Subscription subscription;

    protected AzureDialogWrapper(@Nullable Project project, boolean canBeParent) {
        super(project, canBeParent);
    }

    protected AzureDialogWrapper(@Nullable Project project,
                                 boolean canBeParent,
                                 @NotNull IdeModalityType ideModalityType) {
        super(project, canBeParent, ideModalityType);
    }

    protected AzureDialogWrapper(@Nullable Project project,
                                 @Nullable Component parentComponent,
                                 boolean canBeParent,
                                 @NotNull IdeModalityType ideModalityType) {
        super(project, parentComponent, canBeParent, ideModalityType);
    }

    protected AzureDialogWrapper(@Nullable Project project) {
        super(project);
    }

    protected AzureDialogWrapper(boolean canBeParent) {
        super(canBeParent);
    }

    protected AzureDialogWrapper(Project project, boolean canBeParent, boolean applicationModalIfPossible) {
        super(project, canBeParent, applicationModalIfPossible);
    }

    protected AzureDialogWrapper(@NotNull Component parent, boolean canBeParent) {
        super(parent, canBeParent);
    }

    /*
    Add custom properties to telemetry while Cancel button is pressed.
     */
    protected void addCancelTelemetryProperties(final Map<String, String> properties) {
    }

    /*
    Add custom properties to telemetry while OK button is pressed.
     */
    protected void addOKTelemetryProperties(final Map<String, String> properties) {
        final JComponent centerPanel = this.createCenterPanel();
        for (final Component component : getAllComponents(this.getContentPane())) {
            if (!component.isEnabled() || !component.isVisible()) {
                continue;
            }

            if (component instanceof JRadioButton) {
                JRadioButton radioButton = (JRadioButton) component;
                String name = radioButton.getName() == null
                              ? radioButton.getText().replaceAll("[\\s+.]", "")
                              : radioButton.getName();
                properties.put("JRadioButton." + name + ".Selected", String.valueOf(radioButton.isSelected()));
            } else if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                String name = checkBox.getName() == null
                              ? checkBox.getText().replaceAll("[\\s+.]", "")
                              : checkBox.getName();
                properties.put("JCheckBox." + name + ".Selected", String.valueOf(checkBox.isSelected()));
            } else if (component instanceof JComboBox) {
                JComboBox comboBox = (JComboBox) component;
                StringBuilder stringBuilder = new StringBuilder();
                String name = comboBox.getName();
                for (final Object object : comboBox.getSelectedObjects()) {
                    stringBuilder.append(object.toString());
                    stringBuilder.append(";");
                    if (StringUtils.isNullOrEmpty(name)) {
                        name = object.getClass().getSimpleName();
                    }
                }
                properties.put("JComboBox." + name + ".Selected", stringBuilder.toString());
            }
        }
    }

    protected java.util.List<Component> getAllComponents(final Container c) {
        java.util.List<Component> compList = new ArrayList<Component>();
        if (c == null) {
            return compList;
        }
        Component[] comps = c.getComponents();
        for (Component comp : comps) {
            compList.add(comp);
            if (comp instanceof Container) {
                compList.addAll(getAllComponents((Container) comp));
            }
        }
        return compList;
    }

    protected void sendTelemetry(int code) {
        final Map<String, String> properties = new HashMap<>();
        String action = "OK";
        properties.put("Window", this.getClass().getSimpleName());
        if (!StringUtils.isNullOrEmpty(this.getTitle())) {
            properties.put("Title", this.getTitle());
        }
        if (this instanceof TelemetryProperties) {
            properties.putAll(((TelemetryProperties) this).toProperties());
        }

        switch (code) {
            case HELP_CODE:
                action = "Help";
                break;
            case OK_EXIT_CODE:
                addOKTelemetryProperties(properties);
                break;
            case CANCEL_EXIT_CODE:
                addCancelTelemetryProperties(properties);
                action = "Cancel";
                break;
            default:
                return;
        }

        AppInsightsClient.createByType(AppInsightsClient.EventType.Dialog,
                                       this.getClass().getSimpleName(),
                                       action,
                                       properties);
        EventUtil.logEvent(EventType.info, TelemetryConstants.DIALOG, action, properties);
    }

    @Override
    protected void doOKAction() {
        // send telemetry when OK button pressed.
        // In case subclass overrides doOKAction(), it should call super.doOKAction() explicitly
        // Otherwise the telemetry is omitted.
        this.sendTelemetry(OK_EXIT_CODE);
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        // send telemetry when Cancel button pressed.
        // In case subclass overrides doCancelAction(), it should call super.doCancelAction() explicitly
        // Otherwise the telemetry is omitted.
        this.sendTelemetry(CANCEL_EXIT_CODE);
        super.doCancelAction();
    }

    @Override
    protected void doHelpAction() {
        this.sendTelemetry(HELP_CODE);
        super.doHelpAction();
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();

        if (this.getSubscription() != null) {
            if (this.getSubscription().getName() != null) {
                properties.put("SubscriptionName",
                               this.getSubscription()
                                   .getName());
            }
            if (this.getSubscription().getId() != null) {
                properties.put("SubscriptionId",
                               this.getSubscription()
                                   .getId());
            }
        }

        return properties;
    }

    protected ValidationInfo validateAzureSubs(JComponent component) {
        try {
            AzureLoginHelper.ensureAzureSubsAvailable();
            return null;
        } catch (AzureExecutionException e) {
            return new ValidationInfo(e.getMessage(), component);
        }
    }
}
