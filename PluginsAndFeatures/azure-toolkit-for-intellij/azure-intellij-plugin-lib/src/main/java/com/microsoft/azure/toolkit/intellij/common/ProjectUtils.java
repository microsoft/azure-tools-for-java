/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.IdeFrameImpl;
import com.intellij.openapi.wm.impl.ProjectFrameHelper;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;

public class ProjectUtils {

    @SuppressWarnings("UnstableApiUsage")
    public static Window getWindow(@Nullable Project project) {
        final WindowManagerEx manager = WindowManagerEx.getInstanceEx();
        Window window = manager.suggestParentWindow(project);
        if (window == null) {
            window = manager.getMostRecentFocusedWindow();
        }
        if (window == null) {
            for (final ProjectFrameHelper frameHelper : manager.getProjectFrameHelpers()) {
                final IdeFrameImpl frame = frameHelper.getFrame();
                if (frame != null && frame.isActive()) {
                    window = frameHelper.getFrame();
                    break;
                }
            }
        }
        if (window == null) {
            window = JOptionPane.getRootFrame();
        }
        return window;
    }

    @Nonnull
    @SuppressWarnings("UnstableApiUsage")
    public static Project getProject() {
        Project project = null;
        final WindowManagerEx manager = WindowManagerEx.getInstanceEx();
        for (final ProjectFrameHelper frameHelper : manager.getProjectFrameHelpers()) {
            final IdeFrameImpl frame = frameHelper.getFrame();
            if (frame != null && frame.isActive()) {
                project = frameHelper.getProject();
            }
        }
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        return project;
    }

    public static Project getProject(@Nullable Component component) {
        return Optional.ofNullable(component).map(c -> DataManager.getInstance().getDataContext(c))
            .or(() -> Optional.of(DataManager.getInstance().getDataContext()))
            .map(c -> c.getData(CommonDataKeys.PROJECT))
            .orElseGet(ProjectUtils::getProject);
    }

    public static boolean isSameModule(@Nullable final Module first, @Nullable final Module second) {
        return Objects.equals(first, second) || StringUtils.equals(getFormalModuleId(first), getFormalModuleId(second));
    }

    private static String getFormalModuleId(@Nullable Module module) {
        return Optional.ofNullable(module).map(Module::getName)
            .map(name -> StringUtils.removeEnd(name, ".main"))
            .map(name -> StringUtils.removeEnd(name, ".test")).orElse(null);
    }
}
