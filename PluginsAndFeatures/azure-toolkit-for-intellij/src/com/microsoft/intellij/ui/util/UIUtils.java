/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.ui.util;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

public class UIUtils {

    public static ActionListener createFileChooserListener(final TextFieldWithBrowseButton parent, final @Nullable Project project,
                                                           final FileChooserDescriptor descriptor) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//                final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createAllButJarContentsDescriptor();
//                DataContext dataContext = DataManager.getInstance().getDataContextFromFocus().getResult();
                final VirtualFile[] files = FileChooser.chooseFiles(descriptor, parent, project,
                        (project == null) && !parent.getText().isEmpty() ? LocalFileSystem.getInstance().findFileByPath(parent.getText()) : null);
                if (files.length > 0) {
                    final StringBuilder builder = new StringBuilder();
                    for (VirtualFile file : files) {
                        if (builder.length() > 0) {
                            builder.append(File.pathSeparator);
                        }
                        builder.append(FileUtil.toSystemDependentName(file.getPath()));
                    }
                    parent.setText(builder.toString());
                }
            }
        };
    }

    public static ActionListener createFileChooserListener(final JTextField parent, final @Nullable Project project,
                                                           final FileChooserDescriptor descriptor) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final VirtualFile[] files = FileChooser.chooseFiles(descriptor, parent, project,
                        (project == null) && !parent.getText().isEmpty() ? LocalFileSystem.getInstance().findFileByPath(parent.getText()) : null);
                if (files.length > 0) {
                    final StringBuilder builder = new StringBuilder();
                    for (VirtualFile file : files) {
                        if (builder.length() > 0) {
                            builder.append(File.pathSeparator);
                        }
                        builder.append(FileUtil.toSystemDependentName(file.getPath()));
                    }
                    parent.setText(builder.toString());
                }
            }
        };
    }

    public static ActionListener createFileChooserListener(final TextFieldWithBrowseButton parent, final @Nullable Project project,
                                                           final FileChooserDescriptor descriptor, final Consumer<List<VirtualFile>> consumer) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileChooser.chooseFiles(descriptor, project, parent,
                        parent.getText().isEmpty() ? null : LocalFileSystem.getInstance().findFileByPath(parent.getText()), consumer);
            }
        };
    }

    /**
     * Select item from combo box as per item name.
     * By finding out selection index as per name.
     *
     * @param combo
     * @param name
     * @return
     */
    public static JComboBox selectByText(JComboBox combo, String name) {
        if (combo.getItemCount() > 0 && name != null && !name.isEmpty()) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                String itemText = ((ElementWrapper) combo.getItemAt(i)).getKey();
                if (name.equals(itemText)) {
                    combo.setSelectedIndex(i);
                    return combo;
                }
            }
        }
        combo.setSelectedIndex(0);
        return combo;
    }

    public static class ElementWrapper<T> {
        private String key;
        private T value;

        public ElementWrapper(String key, T value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return key;
        }
    }

    /**
     * Show a balloon styled notification at the bottom of the IDE.
     */
    public static void showNotification(@NotNull StatusBar statusBar, String message, MessageType type) {
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, type, null /*hyperlinkListener*/)
                .setFadeoutTime(10 * 1000) // fade out after 10 seconds
                .createBalloon()
                .showInCenterOf(statusBar.getComponent());
    }

    public static void showNotification(@NotNull Project project, String message, MessageType type) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        showNotification(statusBar, message, type);
    }
}
