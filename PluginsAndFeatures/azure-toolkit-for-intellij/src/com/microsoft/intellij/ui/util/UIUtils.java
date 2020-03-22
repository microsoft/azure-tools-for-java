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
package com.microsoft.intellij.ui.util;

import com.intellij.diagnostic.ThreadDumper;
import com.intellij.ide.ui.LafManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Consumer;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
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

    public static ActionListener createFileChooserListenerWithTextPath(final TextFieldWithBrowseButton parent, final @Nullable Project project,
                                                                       final FileChooserDescriptor descriptor) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final VirtualFile[] files = FileChooser.chooseFiles(descriptor, parent, project,
                        StringUtils.isNotEmpty(parent.getText()) ? LocalFileSystem.getInstance().findFileByPath(parent.getText()) : null);
                if (files.length > 0) {
                    final StringBuilder builder = new StringBuilder();
                    for (final VirtualFile file : files) {
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

    public static boolean showYesNoDialog(String title, String prompt) {
        return Messages.showYesNoDialog(null, prompt, title, "Yes", "No", null) == 0;
    }

    public static boolean isUnderIntelliJTheme() {
        UIManager.LookAndFeelInfo theme = LafManager.getInstance().getCurrentLookAndFeel();
        return theme.getName().equalsIgnoreCase("intellij");
    }

    public static void setPanelBackGroundColor(JPanel panel, Color color) {
        panel.setBackground(color);
        for (Component child : panel.getComponents()) {
            if (child instanceof JPanel) {
                setPanelBackGroundColor((JPanel) child, color);
            }
        }
    }

    public static void assertInDispatchThread() {
        ApplicationManager.getApplication().assertIsDispatchThread();
    }

    public static void assertInPooledThread() {
        Application app = ApplicationManager.getApplication();

        if (!app.isDispatchThread()) {
            return;
        }

        if (ShutDownTracker.isShutdownHookRunning()) {
            return;
        }

        throw new RuntimeExceptionWithAttachments(
                "Accessing IO or performing other time consuming operations from event dispatch thread will block UI.",
                "EventQueue.isDispatchThread()=" + EventQueue.isDispatchThread() +
                        " Toolkit.getEventQueue()=" + Toolkit.getDefaultToolkit().getSystemEventQueue() +
                        "\nCurrent thread: " + describe(Thread.currentThread()),
                new Attachment("threadDump.txt", ThreadDumper.dumpThreadsToString()));
    }

    // copy from IntelliJ ApplicationImpl
    private static String describe(Thread o) {
        return o == null ? "null" : o + " " + System.identityHashCode(o);
    }
}
