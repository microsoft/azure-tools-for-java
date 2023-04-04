/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.utils;

import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkProvider;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.lang.JavaVersion;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CustomLog
public class JdkUtils {

    @Nullable
    public static Sdk getJdk(@Nonnull Module module) {
        final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk != null && sdk.getSdkType() instanceof JavaSdkType) return sdk;
        return null;
    }

    /**
     * Get project jdk from project or module.
     * reference: com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil#getProjectJdk
     *
     * @param project project
     * @return project jdk
     */
    @Nonnull
    @SuppressWarnings("UnstableApiUsage")
    public static Sdk getJdk(@Nullable Project project) {
        if (project != null) {
            final Sdk res = ProjectRootManager.getInstance(project).getProjectSdk();
            if (res != null) return res;

            final Module[] modules = ModuleManager.getInstance(project).getModules();
            for (final Module module : modules) {
                final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
                if (sdk != null && sdk.getSdkType() instanceof JavaSdkType) return sdk;
            }
        }

        // Workaround for projects without project Jdk
        final SdkType jdkType = ExternalSystemJdkProvider.getInstance().getJavaSdkType();
        return ProjectJdkTable.getInstance()
            .getSdksOfType(jdkType).stream()
            .filter(ExternalSystemJdkUtil::isValidJdk)
            .max(jdkType.versionComparator())
            .orElseGet(() -> ExternalSystemJdkProvider.getInstance().getInternalJdk());
    }

    @Nullable
    public static Integer getJdkLanguageLevel(@Nonnull Module module) {
        Sdk jdk = JdkUtils.getJdk(module);
        if (jdk == null) {
            jdk = getJdk(module.getProject());
        }
        return getJdkLanguageLevel(jdk);
    }

    @Nullable
    public static Integer getJdkLanguageLevel(@Nonnull Project project) {
        final Sdk jdk = JdkUtils.getJdk(project);
        return getJdkLanguageLevel(jdk);
    }

    @Nullable
    public static Integer getJdkLanguageLevel(@Nonnull Sdk jdk) {
        final JavaSdkVersion version = JavaSdk.getInstance().getVersion(jdk);
        if (Objects.nonNull(version)) {
            final LanguageLevel level = version.getMaxLanguageLevel();
            return level.toJavaVersion().feature;
        }
        final String str = jdk.getVersionString();
        if (StringUtils.isNotBlank(str)) {
            final Matcher matcher = Pattern.compile("\\d+(\\.\\d+)*").matcher(str);
            if (matcher.find()) {
                final String versionStr = matcher.group(0);
                return JavaVersion.parse(versionStr).feature;
            }
        }
        return null;
    }

    @Nullable
    public static Integer getTargetBytecodeLanguageLevel(@Nonnull Module module) {
        final CompilerConfiguration config = CompilerConfiguration.getInstance(module.getProject());
        final String level = config.getBytecodeTargetLevel(module);
        if (StringUtils.isNotBlank(level)) {
            return JavaVersion.parse(level).feature;
        }
        return null;
    }

    @Nullable
    public static Integer getBytecodeLanguageLevel(@Nonnull File artifact) {
        try {
            return Utils.getArtifactCompileVersion(artifact);
        } catch (final Exception e) {
            return null;
        }
    }
}
