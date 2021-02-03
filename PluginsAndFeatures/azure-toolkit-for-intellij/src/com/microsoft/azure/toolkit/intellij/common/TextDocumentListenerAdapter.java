/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public interface TextDocumentListenerAdapter extends DocumentListener {
    default void insertUpdate(final DocumentEvent e) {
        this.onDocumentChanged();
    }

    default void removeUpdate(final DocumentEvent e) {
        this.onDocumentChanged();
    }

    default void changedUpdate(final DocumentEvent e) {
        this.onDocumentChanged();
    }

    default void onDocumentChanged() {
    }
}
