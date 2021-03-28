/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.HideableDecorator;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExceptionHandler;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.ui.Messages.wrapToScrollPaneIfNeeded;

public class AzureToolkitErrorDialog extends AzureDialog<Void> {
    private JPanel pnlMain;
    private JPanel pnlHolder;
    private JPanel pnlDetails;
    private JPanel pnlRoot;
    private JLabel lblIcon;
    private JPanel pnlMessage;

    private final String title;
    private final String message;
    private final String details;
    private final Throwable throwable;
    private final Action[] actions;

    public AzureToolkitErrorDialog(final Project project, String title, String message, String details,
                                   AzureExceptionHandler.AzureExceptionAction[] actions, Throwable throwable) {
        super(project);
        this.title = title;
        this.message = message;
        this.details = details;
        this.actions = getExceptionAction(actions);
        this.throwable = throwable;
        if (StringUtils.isNotEmpty(details)) {
            final HideableDecorator slotDecorator = new HideableDecorator(pnlHolder, "Details", true);
            slotDecorator.setContentComponent(pnlDetails);
            slotDecorator.setOn(false);
        } else {
            pnlDetails.setVisible(false);
        }
        init();
    }

    @Override
    protected void init() {
        super.init();
        lblIcon.setIcon(Messages.getErrorIcon());

        final JTextPane messagePane = Messages.configureMessagePaneUi(new JTextPane(), message);
        messagePane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        pnlMessage.add(wrapToScrollPaneIfNeeded(messagePane, 60, 10), BorderLayout.CENTER);

        final JTextPane detailsPane = Messages.configureMessagePaneUi(new JTextPane(), details);
        detailsPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        pnlDetails.add(wrapToScrollPaneIfNeeded(detailsPane, 60, 10), BorderLayout.CENTER);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    protected Action[] createActions() {
        return ArrayUtils.addAll(new Action[]{getOKAction()}, actions);
    }

    private Action[] getExceptionAction(AzureExceptionHandler.AzureExceptionAction[] actions) {
        return Arrays.stream(actions).map(exceptionAction -> new DialogWrapper.DialogWrapperAction(exceptionAction.name()) {
            @Override
            protected void doAction(final ActionEvent actionEvent) {
                exceptionAction.actionPerformed(throwable);
            }
        }).toArray(Action[]::new);
    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        return Collections.emptyList();
    }

    @Override
    public AzureForm<Void> getForm() {
        throw new AzureToolkitRuntimeException("no form found in " + AzureToolkitErrorDialog.class.getSimpleName());
    }

    @Override
    protected String getDialogTitle() {
        return this.title;
    }
}
