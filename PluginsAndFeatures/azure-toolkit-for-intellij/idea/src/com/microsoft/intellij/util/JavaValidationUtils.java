/*
 * Copyright (c) 2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import org.apache.commons.lang.StringUtils;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class JavaValidationUtils extends ValidationUtils {
    //refer: https://dev.azure.com/msazure/AzureDMSS/_git/AzureDMSS-PortalExtension?path=%2Fsrc%2FSpringCloudPortalExt%2FClient%2FCreateApplication%2FCreateApplicationBlade.ts&version=GBdev&line=463&lineEnd=463&lineStartColumn=25&lineEndColumn=55&lineStyle=plain&_a=contents
    private static final String SPRING_CLOUD_APP_NAME_PATTERN = "^[a-z][a-z0-9-]{2,30}[a-z0-9]$";

    public static void validateSpringCloudAppName(final String name, final SpringCloudCluster cluster) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException(message("springCloud.app.name.validate.empty"));
        } else if (!name.matches(SPRING_CLOUD_APP_NAME_PATTERN)) {
            throw new IllegalArgumentException(message("springCloud.app.name.validate.invalid"));
        } else {
            if (cluster.app(name).exists()) {
                throw new IllegalArgumentException(message("springCloud.app.name.validate.exist", name));
            }
        }
    }
}
