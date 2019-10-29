package com.microsoft.azuretools.ijidea.ui;

import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

public class HintTextField extends JBTextField {
    public HintTextField(@NotNull String hint) {
        super();
        getEmptyText().setText(hint);
    }
}