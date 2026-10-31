package com.microsoft.azure.toolkit.ide.guidance.input;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import lombok.Getter;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class FileChooserInputPanel implements AzureFormJPanel<String> {
    public static final String SELECT_PATH_TO_SAVE_THE_PROJECT = "Select path to save the project";
    public static final String PATH_TO_SAVE_THE_DEMO_PROJECT = "Please select the target path to save the demo project";
    @Getter
    private JPanel rootPanel;
    private AzureFileInput fileInput;

    public FileChooserInputPanel() {
        $$$setupUI$$$();
        init();
    }

    private void init() {
        fileInput.addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle(SELECT_PATH_TO_SAVE_THE_PROJECT)
            .withDescription(PATH_TO_SAVE_THE_DEMO_PROJECT));
        fileInput.setRequired(true);
    }

    @Override
    public String getValue() {
        return fileInput.getValue();
    }

    @Override
    public void setValue(final String value) {
        this.fileInput.setValue(value);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(fileInput);
    }

    @Override
    public JPanel getContentPanel() {
        return rootPanel;
    }
}
