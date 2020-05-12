/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.helpers;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.intellij.plugins.markdown.ui.preview.MarkdownSplitEditor;
import org.intellij.plugins.markdown.ui.split.SplitFileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.intellij.helpers.WhatsNewViewProvider.AZURE_TOOLKIT_WHATS_NEW_VIEW_TYPE;

public enum WhatsNewHelper {
    INSTANCE;

    private static final String AZURE_TOOLKIT_FOR_JAVA = "Azure Toolkit for Java";
    private static final String AZURE_TOOLKIT_WHATS_NEW = "AzureToolkit.WhatsNew";
    private static final String VERSION_PATTERN = "<!-- Version: (.*) -->";
    private static final String WHAT_S_NEW_CONSTANT = "WHAT_S_NEW";
    private static final String WHAT_S_NEW_CONTENT_PATH = "/whatsnew/whatsnew.md";
    private static final Key<String> WHAT_S_NEW_ID = new Key<>(WHAT_S_NEW_CONSTANT);

    public synchronized void showWhatsNew(@NotNull boolean force, @NotNull Project project) throws IOException {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final VirtualFile existingWhatsNewFile = searchExistingFile(fileEditorManager);
        if (existingWhatsNewFile != null) {
            fileEditorManager.openFile(existingWhatsNewFile, true, true);
        } else {
            // Get whats new file
            final String content = getWhatsNewContent();
            // Get whats new version
            final DefaultArtifactVersion whatsNewVersion = getWhatsNewVersion(content);
            final DefaultArtifactVersion shownVersion = getShownVersion();
            if (force || !isDocumentShownBefore(whatsNewVersion, shownVersion)) {
                saveShownVersion(whatsNewVersion);
                createAndShowWhatsNew(project, fileEditorManager, content);
            }
        }
    }

    private void createAndShowWhatsNew(Project project, FileEditorManager fileEditorManager, String content)
            throws IOException {
        final LightVirtualFile virtualFile = new LightVirtualFile(AZURE_TOOLKIT_FOR_JAVA);
        virtualFile.setFileType(new FileType() {
            @NotNull
            @Override
            public String getName() {
                return AZURE_TOOLKIT_WHATS_NEW_VIEW_TYPE;
            }

            @NotNull
            @Override
            public String getDescription() {
                return AZURE_TOOLKIT_WHATS_NEW_VIEW_TYPE;
            }

            @NotNull
            @Override
            public String getDefaultExtension() {
                return AZURE_TOOLKIT_WHATS_NEW_VIEW_TYPE;
            }

            @Nullable
            @Override
            public Icon getIcon() {
                return UIHelperImpl.loadIcon("azure.png");
            }

            @Override
            public boolean isBinary() {
                return false;
            }

            @Override
            public boolean isReadOnly() {
                return true;
            }

            @Nullable
            @Override
            public String getCharset(@NotNull final VirtualFile virtualFile, @NotNull final byte[] bytes) {
                return "UTF-8";
            }
        });
        virtualFile.setBinaryContent(content.getBytes());
        virtualFile.putUserData(WHAT_S_NEW_ID, WHAT_S_NEW_CONSTANT);
        final FileEditor[] fileEditors = fileEditorManager.openFile(virtualFile, true, true);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof MarkdownSplitEditor) {
                // Switch to markdown preview panel
                ((MarkdownSplitEditor) fileEditor).triggerLayoutChange(SplitFileEditor.SplitEditorLayout.SECOND,
                                                                       true);
            }
        }
    }

    private String getWhatsNewContent() throws IOException {
        final File contentFile = StreamUtil.getResourceFile(WHAT_S_NEW_CONTENT_PATH);
        return FileUtils.readFileToString(contentFile, StandardCharsets.UTF_8);
    }

    private boolean isDocumentShownBefore(DefaultArtifactVersion documentVersion, DefaultArtifactVersion shownVersion) {
        // Regard whats new document has been shown if we can't get the version in case shown it every time
        return documentVersion == null || (shownVersion != null && shownVersion.compareTo(documentVersion) >= 0);
    }

    private DefaultArtifactVersion getShownVersion() {
        final String shownVersionValue = PropertiesComponent.getInstance().getValue(AZURE_TOOLKIT_WHATS_NEW);
        return StringUtils.isEmpty(shownVersionValue) ? null : new DefaultArtifactVersion(shownVersionValue);
    }

    private void saveShownVersion(DefaultArtifactVersion version) {
        PropertiesComponent.getInstance().setValue(AZURE_TOOLKIT_WHATS_NEW, version.toString());
    }

    private VirtualFile searchExistingFile(FileEditorManager fileEditorManager) {
        return Arrays.stream(fileEditorManager.getOpenFiles())
                     .filter(file -> StringUtils.equals(file.getUserData(WHAT_S_NEW_ID), WHAT_S_NEW_CONSTANT))
                     .findFirst().orElse(null);
    }

    private DefaultArtifactVersion getWhatsNewVersion(String content) {
        try (Scanner scanner = new Scanner(content)) {
            // Read the first comment line to get the whats new version
            String versionLine = scanner.nextLine();
            final Matcher matcher = Pattern.compile(VERSION_PATTERN).matcher(versionLine);
            return matcher.matches() ? new DefaultArtifactVersion(matcher.group(1)) : null;
        }
    }
}
