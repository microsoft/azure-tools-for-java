package com.microsoft.azure.toolkit.ide.guideline;

import javax.annotation.Nonnull;
import javax.swing.*;

public interface InputComponent {
    JComponent getComponent();

    Context apply(@Nonnull Context context);
}
