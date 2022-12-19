/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.bicep.highlight;

import com.intellij.openapi.application.PluginPathManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.CommonConst;
import org.jetbrains.plugins.textmate.TextMateService;
import org.jetbrains.plugins.textmate.configuration.BundleConfigBean;
import org.jetbrains.plugins.textmate.configuration.TextMateSettings;
import org.jetbrains.plugins.textmate.configuration.TextMateSettings.TextMateSettingsState;
import org.jetbrains.plugins.textmate.language.TextMateLanguageDescriptor;
import org.jetbrains.plugins.textmate.language.syntax.highlighting.TextMateEditorHighlighterProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class BicepEditorHighlighterProvider extends TextMateEditorHighlighterProvider {
    public static final String LIB_ROOT = Path.of(PluginPathManager.getPluginHomePath(CommonConst.PLUGIN_NAME), "lib").toString();

    @Override
    @AzureOperation("platform/bicep.get_editor_highlighter")
    public EditorHighlighter getEditorHighlighter(@Nullable Project project, @Nonnull FileType fileType, @Nullable VirtualFile virtualFile, @Nonnull EditorColorsScheme colors) {
        final TextMateLanguageDescriptor descriptor = TextMateService.getInstance().getLanguageDescriptorByExtension("bicep");
        final TextMateSettingsState state = TextMateSettings.getInstance().getState();
        if (Objects.isNull(descriptor) && Objects.nonNull(state)) { // register textmate if not registered
            registerBicepTextMateBundleAndReload(state);
        }
        return super.getEditorHighlighter(project, fileType, virtualFile, colors);
    }

    @AzureOperation("boundary/bicep.register_textmate_bundles")
    private static void registerBicepTextMateBundleAndReload(TextMateSettingsState state) {
        final Collection<BundleConfigBean> bundles = state.getBundles();
        final ArrayList<BundleConfigBean> newBundles = new ArrayList<>(bundles);
        newBundles.add(new BundleConfigBean("bicep", Path.of(LIB_ROOT, "textmate", "bicep").toString(), true));
        newBundles.add(new BundleConfigBean("bicepparam", Path.of(LIB_ROOT, "textmate", "bicepparam").toString(), true));
        state.setBundles(newBundles);
        TextMateService.getInstance().reloadEnabledBundles();
    }
}
