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

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.HideableDecorator;
import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import static com.intellij.openapi.ui.Messages.wrapToScrollPaneIfNeeded;

public class ToolkitErrorDialog extends AzureDialogWrapper {
    private JPanel pnlMain;
    private JPanel pnlHolder;
    private JPanel pnlDetails;
    private JPanel pnlRoot;
    private JLabel lblIcon;
    private JPanel pnlMessage;

    private HideableDecorator slotDecorator;

    private String title;
    private String message;
    private String details;
    private Throwable throwable;
    private Action[] actions;
    private Project project;

    public ToolkitErrorDialog(final Project project, String title, String message, String details,
                              AzureExceptionHandler.AzureExceptionAction[] actions, Throwable throwable) {
        super(project);
        this.project = project;
        this.title = title;
        this.message = message;
        this.details = details;
        this.actions = getExceptionAction(actions);
        this.throwable = throwable;
        setTitle(title);
        slotDecorator = new HideableDecorator(pnlHolder, "Details", true);
        slotDecorator.setContentComponent(pnlDetails);
        slotDecorator.setOn(false);
        if (StringUtils.isEmpty(details)) {
            slotDecorator.setEnabled(false);
            pnlHolder.setVisible(false);
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
        pnlDetails.add( wrapToScrollPaneIfNeeded(detailsPane, 60, 10), BorderLayout.CENTER);
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
}
