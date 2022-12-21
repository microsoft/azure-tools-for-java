/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.bicep.activities;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.CommonConst;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.wso2.lsp4intellij.IntellijLanguageClient;
import org.wso2.lsp4intellij.client.languageserver.serverdefinition.ProcessBuilderServerDefinition;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Objects;
import java.util.Optional;

public class BicepStartupActivity implements StartupActivity {
    protected static final Logger LOG = Logger.getInstance(BicepStartupActivity.class);
    public static final String BICEP_LANGSERVER = "bicep-langserver";
    public static final String BICEP_LANG_SERVER_DLL = "Bicep.LangServer.dll";
    public static final String DOTNET = "dotnet";
    public static final String STDIO = "--stdio";
    public static final String BICEP = "bicep";

    @Override
    @ExceptionNotification
    @AzureOperation(name = "platform/bicep.startup_language_server")
    public void runActivity(@Nonnull Project project) {
        final File bicep = FileUtils.getFile(CommonConst.PLUGIN_PATH, "bicep", BICEP_LANGSERVER, BICEP_LANG_SERVER_DLL);
        if (Objects.isNull(bicep) || !bicep.exists()) {
            AzureMessager.getMessager().warning("Bicep Language Server was not found, disable related features.");
            return;
        }
        final String dotnetRuntimePath = Azure.az().config().getDotnetRuntimePath();
        final ProcessBuilder process = new ProcessBuilder(DOTNET, bicep.getAbsolutePath(), STDIO);
        Optional.ofNullable(dotnetRuntimePath)
            .filter(StringUtils::isNotEmpty).map(File::new)
            .filter(File::exists).ifPresent(process::directory);
        IntellijLanguageClient.addServerDefinition(new ProcessBuilderServerDefinition(BICEP, process));
    }
}
