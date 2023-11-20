/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nonnull;
import java.util.Objects;

public class EditorUtils {
    public static void focusContentInCurrentEditor(@Nonnull final Project project, @Nonnull final VirtualFile file, @Nonnull final String targetContent) {
        final FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (selectedEditor instanceof TextEditor &&  Objects.equals(selectedEditor.getFile(), file)) {
            final Editor editor = ((TextEditor) selectedEditor).getEditor();
            final CaretModel caretModel = editor.getCaretModel();
            final String text = editor.getDocument().getText();
            final int index = text.indexOf(targetContent);
            if (index >= 0) {
                caretModel.moveToOffset(index);
            }
            // caretModel.getCurrentCaret().setSelection(index, index + targetContent.length());
        }
    }
}
