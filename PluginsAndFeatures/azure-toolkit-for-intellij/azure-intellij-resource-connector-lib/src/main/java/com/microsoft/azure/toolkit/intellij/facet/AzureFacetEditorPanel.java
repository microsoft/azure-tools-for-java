package com.microsoft.azure.toolkit.intellij.facet;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Optional;

public class AzureFacetEditorPanel {
    @Nonnull
    private final Module module;
    @Getter
    private JPanel contentPanel;
    private TextFieldWithBrowseButton dotAzureDirInput;

    public AzureFacetEditorPanel(@Nonnull final Module module) {
        this.module = module;
        $$$setupUI$$$();
    }

    public String getValue() {
        return this.dotAzureDirInput.getText();
    }

    public void setValue(final String dotAzurePath) {
        this.dotAzureDirInput.setText(dotAzurePath);
    }

    public JComponent[] getInputs() {
        return new JComponent[]{this.dotAzureDirInput};
    }

    private void createUIComponents() {
        this.dotAzureDirInput = new TextFieldWithBrowseButton(new JTextField());
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false)
            .withFileFilter(file -> file.isDirectory() && file.findChild("profiles.xml") != null);
        Optional.ofNullable(ProjectUtil.guessModuleDir(this.module)).ifPresent(descriptor::setRoots);
        this.dotAzureDirInput.setEditable(false);
        this.dotAzureDirInput.setEnabled(false);
        // noinspection DialogTitleCapitalization
        final String title = "Select path to Azure resource connection configuration files";
        this.dotAzureDirInput.addBrowseFolderListener(this.module.getProject(), descriptor.withTitle(title));
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    public void $$$setupUI$$$() {
    }
}
