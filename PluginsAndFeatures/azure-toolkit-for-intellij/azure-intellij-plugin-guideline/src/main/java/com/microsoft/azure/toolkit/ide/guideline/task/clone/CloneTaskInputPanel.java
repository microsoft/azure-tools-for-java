package com.microsoft.azure.toolkit.ide.guideline.task.clone;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.microsoft.azure.toolkit.ide.guideline.Context;
import com.microsoft.azure.toolkit.ide.guideline.InputComponent;
import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.File;

public class CloneTaskInputPanel implements InputComponent {
    public static final String SELECT_PATH_TO_SAVE_THE_PROJECT = "Select path to save the project";
    public static final String PATH_TO_SAVE_THE_DEMO_PROJECT = "Please select the target path to save the demo project";
    private JPanel pnlRoot;
    private AzureFileInput fileInput;

    private Process process;

    public CloneTaskInputPanel(Process process) {
        this.process = process;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        fileInput.setValue(new File(System.getProperty("user.home"), process.getName()).getAbsolutePath());
        fileInput.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>(SELECT_PATH_TO_SAVE_THE_PROJECT, PATH_TO_SAVE_THE_DEMO_PROJECT, fileInput,
                null, FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
    }

    @Override
    public JComponent getComponent() {
        return pnlRoot;
    }

    @Override
    public Context apply(@Nonnull Context context) {
        context.setProperty("directory", fileInput.getValue());
        return context;
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
