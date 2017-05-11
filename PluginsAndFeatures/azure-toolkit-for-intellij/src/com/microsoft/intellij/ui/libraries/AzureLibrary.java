/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.ui.libraries;

public class AzureLibrary {
    public static AzureLibrary SQL_JDBC = new AzureLibrary("Microsoft JDBC Driver 6.0 for SQL Server UI",
            "com-microsoft-sqljdbc", new String[]{});
    public static AzureLibrary AZURE_LIBRARIES = new AzureLibrary("Package for Microsoft Azure Libraries for Java (by Microsoft)",
            null,
            new String[]{
                    "azure-1.0.0.jar",
                    "azure-client-authentication-1.0.2.jar",
                    "azure-client-runtime-1.0.2.jar",
                    "client-runtime-1.0.0.jar",
                    "guava-20.0.jar",
                    "retrofit-2.1.0.jar",
                    "okhttp-3.3.1.jar",
                    "okio-1.8.0.jar",
                    "logging-interceptor-3.3.1.jar",
                    "okhttp-urlconnection-3.3.1.jar",
                    "converter-jackson-2.1.0.jar",
                    "jackson-databind-2.7.2.jar",
                    "jackson-datatype-joda-2.7.2.jar",
                    "jackson-annotations-2.7.0.jar",
                    "jackson-core-2.7.2.jar",
                    "joda-time-2.4.jar",
                    "commons-lang3-3.4.jar",
                    "adapter-rxjava-2.1.0.jar",
                    "adal4j-1.1.2.jar",
                    "oauth2-oidc-sdk-4.5.jar",
                    "mail-1.4.7.jar",
                    "activation-1.1.jar",
                    "jcip-annotations-1.0.jar",
                    "json-smart-1.1.1.jar",
                    "lang-tag-1.4.jar",
                    "nimbus-jose-jwt-3.1.2.jar",
                    "bcprov-jdk15on-1.51.jar",
                    "gson-2.7.jar",
                    "commons-codec-1.10.jar",
                    "azure-mgmt-resources-1.0.0.jar",
                    "rxjava-1.2.4.jar",
                    "azure-annotations-1.0.0.jar",
                    "azure-mgmt-storage-1.0.0.jar",
                    "azure-mgmt-network-1.0.0.jar",
                    "azure-mgmt-compute-1.0.0.jar",
                    "azure-mgmt-graph-rbac-1.0.0.jar",
                    "azure-mgmt-keyvault-1.0.0.jar",
                    "azure-mgmt-batch-1.0.0.jar",
                    "azure-mgmt-trafficmanager-1.0.0.jar",
                    "azure-mgmt-dns-1.0.0.jar",
                    "azure-mgmt-redis-1.0.0.jar",
                    "azure-mgmt-appservice-1.0.0.jar",
                    "api-annotations-0.0.1.jar",
                    "azure-mgmt-cdn-1.0.0.jar",
                    "azure-mgmt-sql-1.0.0.jar"
            });
    public static AzureLibrary APP_INSIGHTS = new AzureLibrary("Application Insights for Java",
            null,
            new String[]{
                    "applicationinsights-core-1.0.3.jar",
                    "applicationinsights-management-1.0.3.jar",
                    "applicationinsights-web-1.0.3.jar",
                    "guava-20.0.jar",
                    "httpcore-4.3.3.jar",
                    "httpclient-4.3.6.jar",
                    "commons-io-2.5.jar",
                    "commons-codec-1.10.jar",
                    "commons-logging-1.1.3.jar",
                    "commons-lang3-3.4.jar",
                    "annotation-detector-3.0.4.jar"
            });
    public static AzureLibrary[] LIBRARIES = new AzureLibrary[]{SQL_JDBC, AZURE_LIBRARIES};

    private String name;
    private String location;
    private String[] files;

    public AzureLibrary(String name, String location, String[] files) {
        this.name = name;
        this.location = location;
        this.files = files;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String[] getFiles() {
        return files;
    }
}
