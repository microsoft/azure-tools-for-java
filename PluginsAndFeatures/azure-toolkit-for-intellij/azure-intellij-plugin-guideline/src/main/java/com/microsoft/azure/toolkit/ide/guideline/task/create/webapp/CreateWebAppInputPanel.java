package com.microsoft.azure.toolkit.ide.guideline.task.create.webapp;

import com.microsoft.azure.toolkit.ide.guideline.Context;
import com.microsoft.azure.toolkit.ide.guideline.InputComponent;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;

import javax.annotation.Nonnull;
import javax.swing.*;

import static com.microsoft.azure.toolkit.ide.guideline.task.create.webapp.CreateWebAppTask.WEBAPP_NAME;

public class CreateWebAppInputPanel implements InputComponent {

    private JTextField txtName;
    private JPanel pnlRoot;

    public CreateWebAppInputPanel() {
        $$$setupUI$$$();
        init();
    }

    private void init() {
        txtName.setText(String.format("webapp-%s", Utils.getTimestamp()));
    }

    @Override
    public JComponent getComponent() {
        return pnlRoot;
    }

    @Override
    public Context apply(@Nonnull Context context) {
        context.setProperty(WEBAPP_NAME, txtName.getText());
        return context;
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
