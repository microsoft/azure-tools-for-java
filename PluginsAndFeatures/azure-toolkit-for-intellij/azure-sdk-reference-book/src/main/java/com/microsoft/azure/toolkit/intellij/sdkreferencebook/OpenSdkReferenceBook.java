package com.microsoft.azure.toolkit.intellij.sdkreferencebook;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.AzureSdkLibraryService;

import javax.annotation.Nonnull;

public class OpenSdkReferenceBook extends AnAction {
    @Override
    public void actionPerformed(@Nonnull final AnActionEvent anActionEvent) {
        System.out.println(OpenSdkReferenceBook.class.getName());
        // todo: remove test codes
        AzureSdkLibraryService.getInstance().getArtifacts()
            .forEach(azureSDKArtifactEntity -> System.out.println(azureSDKArtifactEntity.getDisplayName()));
        AzureSdkLibraryService.getInstance().getServices()
            .forEach(azureSdkServiceEntity -> {
                System.out.println(azureSdkServiceEntity.getName());
                azureSdkServiceEntity.getFeatures().forEach(feature -> {
                    final String description = String.format("\t %s : %d %d",
                        feature.getName(),
                        feature.getClientPackages().size(),
                        feature.getManagementPackages().size());
                    System.out.println(description);
                });
            });
    }
}
