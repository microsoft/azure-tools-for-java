/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.ui.libraries;

public class AzureLibrary {
    public static AzureLibrary SQL_JDBC = new AzureLibrary("Microsoft JDBC Driver 6.1 for SQL Server UI",
            null, new String[]{"mssql-jdbc"});
    public static AzureLibrary AZURE_LIBRARIES = new AzureLibrary("Package for Microsoft Azure Libraries for Java (by Microsoft)",
            null,
            new String[]{
                    "azure",
                    "azure-client-authentication",
                    "azure-client-runtime",
                    "client-runtime",
                    "guava",
                    "retrofit",
                    "okhttp",
                    "okio",
                    "logging-interceptor",
                    "okhttp-urlconnection",
                    "converter-jackson",
                    "jackson-databind",
                    "jackson-datatype-joda",
                    "jackson-annotations",
                    "jackson-core",
                    "joda-time",
                    "commons-lang3",
                    "adapter-rxjava",
                    "adal4j",
                    "oauth2-oidc-sdk",
                    "mail",
                    "activation",
                    "jcip-annotations",
                    "json-smart",
                    "lang-tag",
                    "nimbus-jose-jwt",
                    "bcprov-jdk15on",
                    "gson",
                    "commons-codec",
                    "azure-mgmt-resources",
                    "rxjava",
                    "azure-annotations",
                    "azure-mgmt-storage",
                    "azure-mgmt-network",
                    "azure-mgmt-compute",
                    "azure-mgmt-graph-rbac",
                    "azure-mgmt-keyvault",
                    "azure-mgmt-batch",
                    "azure-mgmt-trafficmanager",
                    "azure-mgmt-dns",
                    "azure-mgmt-redis",
                    "azure-mgmt-appservice",
                    "api-annotations",
                    "azure-mgmt-cdn",
                    "azure-mgmt-sql"
            });
    public static AzureLibrary APP_INSIGHTS = new AzureLibrary("Application Insights for Java",
            null,
            new String[]{
                    "applicationinsights-core",
                    "applicationinsights-web",
                    "guava",
                    "httpcore",
                    "httpclient",
                    "commons-io",
                    "commons-codec",
                    "commons-logging",
                    "commons-lang3",
                    "annotation-detector"
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
