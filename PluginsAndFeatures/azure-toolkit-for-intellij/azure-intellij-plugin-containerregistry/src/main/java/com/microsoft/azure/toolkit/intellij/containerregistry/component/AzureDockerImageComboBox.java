/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListItemDescriptorAdapter;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.popup.list.GroupedItemsListRenderer;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.ProjectUtils;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AzureDockerImageComboBox extends AzureComboBox<DockerImage> {
    private DockerHost dockerHost;
    private final List<DockerImage> draftImages = new ArrayList<>();
    @Getter
    private final Project project;

    public AzureDockerImageComboBox(Project project) {
        super(false);
        this.project = project;
        this.setRenderer(new GroupedItemsListRenderer<>(new DockerImageDescriptor()) {
            @Override
            protected boolean hasSeparator(DockerImage value, int index) {
                return index >= 0 && super.hasSeparator(value, index);
            }
        });
        this.setUsePreferredSizeAsMinimum(false);
    }

    public void setDockerHost(@Nullable final DockerHost dockerHost) {
        if (Objects.equals(dockerHost, this.dockerHost)) {
            return;
        }
        this.dockerHost = dockerHost;
        if (dockerHost == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public void setValue(DockerImage value) {
        final Boolean isDraftImage = Optional.ofNullable(value).map(DockerImage::isDraft).orElse(false);
        if (!(getItems().contains(value) || draftImages.contains(value)) && isDraftImage) {
            this.draftImages.removeIf(image -> Objects.equals(image.getDockerFile(), value.getDockerFile()));
            this.draftImages.add(0, value);
            this.reloadItems();
        }
        super.setValue(value);
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof DockerImage) {
            return ((DockerImage) item).isDraft() ? Optional.ofNullable(((DockerImage) item).getDockerFile()).orElse("Unknown Docker File") : ((DockerImage) item).getImageName();
        }
        return super.getItemText(item);
    }

    @Nullable
    @Override
    protected Icon getItemIcon(Object item) {
        if (item instanceof DockerImage) {
            return IntelliJAzureIcons.getIcon(((DockerImage) item).isDraft() ? "/icons/DockerFile.svg" : "/icons/Docker.svg");
        }
        return super.getItemIcon(item);
    }

    @Nonnull
    @Override
    protected List<? extends DockerImage> loadItems() throws Exception {
        final List<DockerImage> localImages = getLocalDockerImages();
        final List<DockerImage> dockerImages = this.loadDockerFiles().stream()
                .filter(draft -> ListUtils.indexOf(localImages, image -> Objects.equals(image.getDockerFile(), draft.getDockerFile())) < 0)
                .collect(Collectors.toList());
        return Stream.of(localImages, draftImages, dockerImages).flatMap(List::stream).distinct().collect(Collectors.toList());
    }

    private List<DockerImage> getLocalDockerImages() {
        try {
            return Optional.ofNullable(this.dockerHost)
                .map(AzureDockerClient::from)
                .map(AzureDockerClient::listLocalImages)
                .map(l -> l.stream().flatMap(image -> DockerImage.fromImage(image).stream())
                        .sorted(Comparator.comparing(DockerImage::getImageName)).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        } catch (final RuntimeException e) {
            return Collections.emptyList();
        }
    }

    private List<DockerImage> loadDockerFiles() {
        final Project project = Optional.ofNullable(this.project).orElseGet(ProjectUtils::getProject);
        return ReadAction.compute(() -> FilenameIndex.getVirtualFilesByName("Dockerfile", GlobalSearchScope.projectScope(project))).stream()
                .map(DockerImage::new).collect(Collectors.toList());
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        final List<ExtendableTextComponent.Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("%s (%s)", "Select Docker File", KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension addEx = ExtendableTextComponent.Extension.create(AllIcons.General.OpenDisk, tooltip, this::selectDockerFile);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    public void addDraftValue(@Nonnull final DockerImage value) {
        if (value.isDraft()) {
            draftImages.add(value);
        }
    }

    private void selectDockerFile() {
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        descriptor.withTitle("Select Dockerfile");
        final VirtualFile toSelect = Optional.ofNullable(this.getValue()).map(DockerImage::getDockerFile).map(file -> LocalFileSystem.getInstance().findFileByIoFile(new File(file))).orElse(null);
        final VirtualFile[] files = FileChooser.chooseFiles(descriptor, project, toSelect);
        if (ArrayUtils.isNotEmpty(files)) {
            final VirtualFile file = files[0];
            final DockerImage image = new DockerImage(file);
            this.draftImages.add(image);
            this.reloadItems();
            this.setValue(image);
        }
    }

    class DockerImageDescriptor extends ListItemDescriptorAdapter<DockerImage> {
        @Override
        public String getTextFor(DockerImage image) {
            return image.isDraft() ? Optional.ofNullable(image.getDockerFile()).orElse("Unknown Docker File") : image.getImageName();
        }

        @Override
        public Icon getIconFor(DockerImage value) {
            return IntelliJAzureIcons.getIcon(value.isDraft() ? "/icons/DockerFile.svg" : "/icons/Docker.svg");
        }

        @Override
        public String getCaptionAboveOf(DockerImage image) {
            return image.isDraft() ? "Dockerfile" : "Docker Image";
        }

        @Override
        public boolean hasSeparatorAboveOf(DockerImage image) {
            final List<DockerImage> items = getItems();
            final int index = items.indexOf(image);
            if (index <= 0) {
                return index == 0;
            }
            final String currentCaption = getCaptionAboveOf(image);
            final String lastCaption = getCaptionAboveOf(items.get(index - 1));
            return !Objects.equals(currentCaption, lastCaption);
        }
    }
}
