/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.Operation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.Type.SUCCESS;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.*;

@Slf4j
public abstract class AzureDialog<T> extends DialogWrapper {
    public static final String SYSTEM = "system";
    public static final String CANCEL = "cancel_dialog";
    public static final String CANCEL_DIALOG = "user/system.cancel_dialog";
    public static final String DIALOG_TITLE = "DialogTitle";
    @Setter
    protected OkActionListener<T> okActionListener;
    @Setter
    protected Action<T> okAction;
    @Setter
    protected Action<T> closeAction;
    protected String helpId;

    public AzureDialog(Project project) {
        super(project, true);
        setTitle(this.getDialogTitle());
        setModal(true);
    }

    public AzureDialog() {
        this(null);
    }

    @Override
    protected void doOKAction() {
        try {
            if (Objects.nonNull(this.okAction)) {
                final T data = this.getForm().getValue();
                final DataContext context = DataManager.getInstance().getDataContext(this.getContentPanel());
                final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), null, getName(), context);
                this.okAction.handle(data, event);
                super.doOKAction();
            } else if (Objects.nonNull(this.okActionListener)) {
                final T data = this.getForm().getValue();
                this.okActionListener.onOk(data);
            } else {
                super.doOKAction();
            }
        } catch (final Exception e) {
            AzureMessager.getMessager().error(e);
        }
    }

    public void close() {
        this.doCancelAction();
    }

    @Override
    public void doCancelAction() {
        try {
            if (Objects.nonNull(this.closeAction)) {
                final DataContext context = DataManager.getInstance().getDataContext(this.getContentPanel());
                final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), null, getName(), context);
                this.closeAction.handle(null, event);
                super.doCancelAction();
            } else {
                AzureTelemeter.log(AzureTelemetry.Type.OP_END, getCancelProperties());
                super.doCancelAction();
            }
        } catch (final Exception e) {
            AzureMessager.getMessager().error(e);
        }
    }

    private Map<String, String> getCancelProperties() {
        final Map<String, String> properties = new HashMap<>();
        final Action.Id<T> id = Optional.ofNullable(okAction).map(Action::getId).orElse(null);
        properties.put(SERVICE_NAME, Optional.ofNullable(id).map(
            Action.Id::getService).orElse(SYSTEM));
        properties.put(OPERATION_NAME, Optional.ofNullable(id).map(Action.Id::getOperation).map(op -> op + "_" + CANCEL).orElse(SYSTEM));
        properties.put(OP_TYPE, Operation.Type.USER);
        properties.put(OP_NAME, Optional.ofNullable(okAction).map(Action::getId).map(Action.Id::getId).orElse(CANCEL_DIALOG));
        properties.put(DIALOG_TITLE, this.getDialogTitle());
        return properties;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.getForm().getInputs().stream()
            .filter(i -> i instanceof AzureFormInputComponent)
            .map(i -> ((AzureFormInputComponent<?>) i).getInputComponent())
            .findFirst().orElseGet(super::getPreferredFocusedComponent);
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        final List<AzureValidationInfo> infos = this.getForm().getAllValidationInfos(true);
        // this.setOKActionEnabled(infos.stream().allMatch(AzureValidationInfo::isValid));
        return infos.stream()
            .filter(i -> i.getType() != SUCCESS)
            .map(AzureFormInputComponent::toIntellijValidationInfo)
            .collect(Collectors.toList());
    }

    public abstract AzureForm<T> getForm();

    @Nonnull
    protected abstract String getDialogTitle();

    @Nonnull
    protected String getName() {
        return this.getDialogTitle().replaceAll("\\s+", ".").toLowerCase();
    }

    @FunctionalInterface
    public interface OkActionListener<T> {
        void onOk(T data);
    }

    @Override
    protected @NonNls @Nullable String getHelpId() {
        return Optional.ofNullable(helpId).orElseGet(super::getHelpId);
    }
}
