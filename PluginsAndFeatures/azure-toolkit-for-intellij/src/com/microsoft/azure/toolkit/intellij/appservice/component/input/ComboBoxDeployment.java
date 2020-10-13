package com.microsoft.azure.toolkit.intellij.appservice.component.input;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ComboBoxDeployment extends AzureComboBox<Path> {

    public static final String FILE_CHOOSER_TITLE = "Select Artifact to Deploy";

    @NotNull
    @Override
    protected List<? extends Path> loadItems() throws Exception {
        final Path[] paths = {
                Paths.get("C:\\Users\\wangmi\\workspace\\azure-tools-for-java"),
                Paths.get("C:\\Users\\wangmi\\workspace\\azure-maven-plugins")
        };
        return Arrays.asList(paths);
    }

    @Nullable
    @Override
    protected Icon getItemIcon(Object o) {
        if (o instanceof VirtualFile) {
            return AllIcons.FileTypes.Archive;
        } else {
            return super.getItemIcon(o);
        }
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.OpenDisk, "Open file", this::onSelectFile);
    }


    private void onSelectFile() {
        // Todo: enable customize file descriptor
        final FileChooserDescriptor fileDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        fileDescriptor.setTitle(FILE_CHOOSER_TITLE);
        final VirtualFile file = FileChooser.chooseFile(fileDescriptor, null, null);
        if (Objects.nonNull(file) && file.exists()) {
            addOrSelectExistingVirtualFile(file);
        }
    }

    private void addOrSelectExistingVirtualFile(VirtualFile virtualFile) {
        for (int i = 0; i < ComboBoxDeployment.this.getItemCount(); i++) {
            final Object object = ComboBoxDeployment.this.getItemAt(i);
            if (object instanceof VirtualFile && StringUtils.equals(virtualFile.getPath(),
                                                                    ((VirtualFile) object).getPath())) {
                ComboBoxDeployment.this.setSelectedItem(virtualFile);
                return;
            }
        }
        ComboBoxDeployment.this.addItem(Paths.get(virtualFile.getPath()));
        ComboBoxDeployment.this.setSelectedItem(virtualFile);
    }

}
