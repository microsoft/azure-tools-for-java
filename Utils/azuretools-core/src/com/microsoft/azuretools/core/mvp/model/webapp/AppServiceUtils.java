/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.management.appservice.CsmPublishingProfileOptions;
import com.microsoft.azure.management.appservice.PublishingProfileFormat;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.java.Log;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Paths;

@Log
public class AppServiceUtils {
    @AzureOperation(
        value = "get publishing profile of function app[%s] with secret",
        params = {"$webAppBase.name()"},
        type = AzureOperation.Type.TASK
    )
    public static boolean getPublishingProfileXmlWithSecrets(WebAppBase webAppBase, String filePath) {
        final File file = new File(Paths.get(filePath, String.format("%s_%s.PublishSettings",
                                                                     webAppBase.name(), System.currentTimeMillis()))
                                        .toString());
        try {
            file.createNewFile();
        } catch (final IOException e) {
            log.warning("failed to create publishing profile xml file");
            return false;
        }
        try (final InputStream inputStream = webAppBase.manager().inner().webApps()
                                                       .listPublishingProfileXmlWithSecrets(webAppBase.resourceGroupName(),
                                                                                      webAppBase.name(),
                                                                                      new CsmPublishingProfileOptions().withFormat(
                                                                                          PublishingProfileFormat.FTP));
             final OutputStream outputStream = new FileOutputStream(file)
        ) {
            IOUtils.copy(inputStream, outputStream);
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
