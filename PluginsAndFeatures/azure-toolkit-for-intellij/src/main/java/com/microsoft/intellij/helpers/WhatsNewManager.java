/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.intellij.plugins.markdown.ui.preview.MarkdownSplitEditor;
import org.intellij.plugins.markdown.ui.split.SplitFileEditor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum WhatsNewManager {
    INSTANCE;

    private static final String AZURE_TOOLKIT_FOR_JAVA = "Azure Toolkit for Java";
    private static final String AZURE_TOOLKIT_WHATS_NEW = "AzureToolkit.WhatsNew";
    private static final String VERSION_PATTERN = "<!-- Version: (.*) -->";
    private static final String WHAT_S_NEW_CONSTANT = "WHAT_S_NEW";
    private static final String WHAT_S_NEW_CONTENT_PATH = "/whatsnew/whatsnew.md";
    private static final Key<String> WHAT_S_NEW_ID = new Key<>(WHAT_S_NEW_CONSTANT);

    public synchronized void showWhatsNew(boolean force, @NotNull Project project) throws IOException {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final VirtualFile existingWhatsNewFile = getExistingWhatsNewFile(fileEditorManager);
        if (existingWhatsNewFile != null) {
            ApplicationManager.getApplication().invokeLater(
                () -> fileEditorManager.openFile(existingWhatsNewFile, true, true), ModalityState.NON_MODAL);
        } else {
            // Get whats new file
            final String content = getWhatsNewContent();
            // Get whats new version
            final DefaultArtifactVersion whatsNewVersion = getCurrentWhatsNewVersion(content);
            final DefaultArtifactVersion shownVersion = getLastWhatsNewVersion();
            if (force || !isDocumentShownBefore(whatsNewVersion, shownVersion)) {
                saveWhatsNewVersion(whatsNewVersion);
                createAndShowWhatsNew(fileEditorManager, content);
            }
        }
    }

    private void createAndShowWhatsNew(FileEditorManager fileEditorManager, String content) {
        final LightVirtualFile virtualFile = new LightVirtualFile(AZURE_TOOLKIT_FOR_JAVA);
        virtualFile.setLanguage(Language.findLanguageByID("Markdown"));
        virtualFile.setContent(null, content, true);
        virtualFile.putUserData(WHAT_S_NEW_ID, WHAT_S_NEW_CONSTANT);
        virtualFile.setWritable(false);
        AzureTaskManager.getInstance().runAndWait(() -> {
            if (fileEditorManager.getProject().isDisposed()) {
                return;
            }
            final FileEditor[] fileEditors = fileEditorManager.openFile(virtualFile, true, true);
            for (FileEditor fileEditor : fileEditors) {
                if (fileEditor instanceof MarkdownSplitEditor) {
                    // Switch to markdown preview panel
                    ((MarkdownSplitEditor) fileEditor).triggerLayoutChange(SplitFileEditor.SplitEditorLayout.SECOND, true);
                }
            }
        });
    }

    private String getWhatsNewContent() throws IOException {
        final File contentFile = StreamUtil.getResourceFile(WHAT_S_NEW_CONTENT_PATH);
        return FileUtils.readFileToString(contentFile, StandardCharsets.UTF_8);
    }

    private boolean isDocumentShownBefore(DefaultArtifactVersion documentVersion, DefaultArtifactVersion shownVersion) {
        // Regard whats new document has been shown if we can't get the version in case shown it every time
        return documentVersion == null || (shownVersion != null && shownVersion.compareTo(documentVersion) >= 0);
    }

    private DefaultArtifactVersion getLastWhatsNewVersion() {
        final String shownVersionValue = PropertiesComponent.getInstance().getValue(AZURE_TOOLKIT_WHATS_NEW);
        return StringUtils.isEmpty(shownVersionValue) ? null : new DefaultArtifactVersion(shownVersionValue);
    }

    private void saveWhatsNewVersion(DefaultArtifactVersion version) {
        PropertiesComponent.getInstance().setValue(AZURE_TOOLKIT_WHATS_NEW, version.toString());
    }

    private VirtualFile getExistingWhatsNewFile(FileEditorManager fileEditorManager) {
        return Arrays.stream(fileEditorManager.getOpenFiles())
                     .filter(file -> StringUtils.equals(file.getUserData(WHAT_S_NEW_ID), WHAT_S_NEW_CONSTANT))
                     .findFirst().orElse(null);
    }

    private DefaultArtifactVersion getCurrentWhatsNewVersion(String content) {
        try (Scanner scanner = new Scanner(content)) {
            // Read the first comment line to get the whats new version
            String versionLine = scanner.nextLine();
            final Matcher matcher = Pattern.compile(VERSION_PATTERN).matcher(versionLine);
            return matcher.matches() ? new DefaultArtifactVersion(matcher.group(1)) : null;
        }
    }
}
