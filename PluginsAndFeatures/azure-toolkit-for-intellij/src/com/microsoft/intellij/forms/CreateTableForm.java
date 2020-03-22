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
package com.microsoft.intellij.forms;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CreateTableForm extends AzureDialogWrapper {
    private JPanel contentPane;
    private JTextField nameTextField;
    private JLabel namingGuidelinesLink;
    private Project project;
    private ClientStorageAccount storageAccount;
    private Runnable onCreate;

    public CreateTableForm(Project project) {

        super(project, true);
        this.project = project;

        setModal(true);
        setTitle("Create Table");
        namingGuidelinesLink.addMouseListener(new LinkListener("http://go.microsoft.com/fwlink/?LinkId=267429"));

        nameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                changedName();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                changedName();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                changedName();
            }
        });

        init();
    }

    private void changedName() {
        setOKActionEnabled(nameTextField.getText().length() > 0);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (!nameTextField.getText().matches("^[A-Za-z][A-Za-z0-9]{2,62}$")) {
            return new ValidationInfo("Table names must start with a letter, and can contain only letters and numbers.\n" +
                    "Queue names must be from 3 through 63 characters long.", nameTextField);
        }

        return null;
    }

    @Override
    protected void doOKAction() {

        final String name = nameTextField.getText();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Creating table...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                /*try {
                    progressIndicator.setIndeterminate(true);

                    for (Table table : StorageClientSDKManager.getManager().getTables(storageAccount)) {
                        if (table.getName().equals(name)) {
                            DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    JOptionPane.showMessageDialog(null, "A table with the specified name already exists.", "Azure Explorer", JOptionPane.ERROR_MESSAGE);
                                }
                            });

                            return;
                        }
                    }

                    Table table = new Table(name, "");
                    StorageClientSDKManager.getManager().createTable(storageAccount, table);

                    if (onCreate != null) {
                        DefaultLoader.getIdeHelper().invokeLater(onCreate);
                    }
                } catch (AzureCmdException e) {
                    String msg = "An error occurred while attempting to create table." + "\n" + String.format(message("webappExpMsg"), e.getMessage());
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, e);
                }*/
            }
        });

        close(DialogWrapper.OK_EXIT_CODE, true);
    }

    public void setStorageAccount(ClientStorageAccount storageAccount) {
        this.storageAccount = storageAccount;
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}