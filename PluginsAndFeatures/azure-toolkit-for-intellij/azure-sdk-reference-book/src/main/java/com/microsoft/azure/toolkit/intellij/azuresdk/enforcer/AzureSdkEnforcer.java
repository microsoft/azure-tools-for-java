/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.enforcer;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.OpenReferenceBookAction;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.AzureSdkLibraryService;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.ProjectLibraryService;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.ProjectLibraryService.ProjectLibEntity;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijActionMessageAction;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijOpenInBrowserMessageAction;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code AzureSdkEnforcer} detects deprecated Azure SDK libs in project and warn
 */
public class AzureSdkEnforcer {

    public static void enforce(Project project) {
        final Map<String, AzureJavaSdkEntity> allDeprecatedAzureLibs = AzureSdkLibraryService.getDeprecatedAzureSDKEntities().stream()
                .collect(Collectors.toMap(AzureJavaSdkEntity::getPackageName, e -> e));
        final Set<String> allDeprecatedAzureLibNames = allDeprecatedAzureLibs.keySet();
        final Set<String> projectLibPackageNames = ProjectLibraryService.getProjectLibraries(project).stream()
                .map(ProjectLibEntity::getPackageName).collect(Collectors.toSet());
        final SetUtils.SetView<String> deprecatedProjectLibNames = SetUtils.intersection(projectLibPackageNames, allDeprecatedAzureLibNames);
        if (CollectionUtils.isNotEmpty(deprecatedProjectLibNames)) {
            final List<AzureJavaSdkEntity> libs = deprecatedProjectLibNames.stream().map(allDeprecatedAzureLibs::get).collect(Collectors.toList());
            final String message = buildMessage(libs);
            final IAzureMessage.Action referenceBook = new IntellijActionMessageAction(OpenReferenceBookAction.ID);
            final IAzureMessage.Action sdkReleases = new IntellijOpenInBrowserMessageAction("Azure SDK Releases", "https://azure.github.io/azure-sdk/releases/latest/java.html");
            AzureMessager.getMessager().warning(message, "Deprecated Azure SDK libraries Detected", referenceBook, sdkReleases);
        }
    }

    private static String buildMessage(@Nonnull List<? extends AzureJavaSdkEntity> libs) {
        final String liPackages = libs.stream().map(l -> {
            if (StringUtils.isNotBlank(l.getReplace())) {
                return String.format("<li>%s" +
                        "   <ul style='margin-top:0;margin-bottom:0;padding:0'>" +
                        "       <li>Replaced by: <a href='%s'>%s</a></li>" +
                        "   </ul>" +
                        "</li>", l.getPackageName(), l.getMavenArtifactUrl(), l.getReplace().trim());
            } else {
                return String.format("<li>%s</li>", l.getPackageName());
            }
        }).collect(Collectors.joining(""));
        return "<html>" +
                "Deprecated Azure SDK libraries are detected in your project, " +
                "refer <a href='https://azure.github.io/azure-sdk/releases/latest/java.html'>Azure SDK Releases</a> for the latest releases." +
                "<ul style='margin-top:2px'>" + liPackages + "</ul>" +
                "</html>";
    }
}
