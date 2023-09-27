/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.enforcer;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.OpenReferenceBookAction;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.AzureSdkLibraryService;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.ProjectLibraryService;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.ProjectLibraryService.ProjectLibEntity;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessage;
import com.microsoft.azure.toolkit.intellij.common.settings.IntellijStore;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        final String neverShowGainActionId = "user/common.never_show_again";
        if (Boolean.TRUE.equals(IntellijStore.getInstance().getState().getSuppressedActions().get(neverShowGainActionId))) {
            return;
        }
        if (CollectionUtils.isNotEmpty(deprecatedProjectLibNames)) {
            final List<AzureJavaSdkEntity> libs = deprecatedProjectLibNames.stream().map(allDeprecatedAzureLibs::get).collect(Collectors.toList());
            AzureSdkEnforcer.warnDeprecatedLibs(libs, project);
        }
    }

    @AzureOperation(name = "ui/sdk.warn_deprecated_libs")
    private static void warnDeprecatedLibs(@AzureTelemetry.Property List<? extends AzureJavaSdkEntity> deprecatedLibs, Project project) {
        final String message = buildMessage(deprecatedLibs);
        final AzureActionManager am = AzureActionManager.getInstance();
        final Action.Id<Object> actionId = Action.Id.of(OpenReferenceBookAction.ID);
        final Action<?> referenceBook = Optional.ofNullable(am).map(m -> m.getAction(actionId)).orElse(null);
        final Action<?> neverShowAgain = Optional.ofNullable(am).map(m -> m.getAction(ResourceCommonActionsContributor.SUPPRESS_ACTION).bind(actionId)).orElse(null);
        final Action<?>[] actions = Stream.of(referenceBook, neverShowAgain).filter(Objects::nonNull).toArray(Action[]::new);
        final IntellijAzureMessage msg = (IntellijAzureMessage) AzureMessager.getMessager().buildWarningMessage(message, "Deprecated Azure SDK libraries Detected", (Object[]) actions);
        msg.setProject(project).show();
    }

    private static String buildMessage(@Nonnull List<? extends AzureJavaSdkEntity> libs) {
        final String liPackages = libs.stream().map(l -> {
            if (StringUtils.isNotBlank(l.getReplace())) {
                final String[] replacePackages = l.getReplace().trim().split(",");
                final String replaces = Arrays.stream(replacePackages)
                    .map(p -> String.format("<a href='%s'>%s</a>", getMavenArtifactUrl(p.trim()), p.trim()))
                    .collect(Collectors.joining(", "));
                return String.format("<li>%s" +
                    "   <ul style='margin-top:0;margin-bottom:0;padding:0'>" +
                    "       <li>Replaced by: %s</li>" +
                    "   </ul>" +
                    "</li>", l.getPackageName(), replaces);
            } else {
                return String.format("<li>%s</li>", l.getPackageName());
            }
        }).collect(Collectors.joining(""));
        return "<html>" +
            "Deprecated Azure SDK libraries are detected in your project, " +
            "refer to <a href='https://azure.github.io/azure-sdk/releases/latest/java.html?_ijop_=sdk.open_latest_releases'>Azure SDK Releases</a> for the latest releases." +
            "<ul style='margin-top:2px'>" + liPackages + "</ul>" +
            "</html>";
    }

    public static String getMavenArtifactUrl(String pkgName) {
        return String.format("https://search.maven.org/artifact/%s/?_ijop_=sdk.open_maven_url", pkgName);
    }
}
