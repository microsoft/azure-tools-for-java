/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.microsoft.azuretools.authmanage.CommonSettings;

import java.net.URI;

public class CommonConst {
    public static final String ProductIconName = "Product";
    public static final String RefreshIConPath = "/icons/Refresh.png";
    public static final String REFRESH_DARK_ICON_PATH = "icons/RefreshDark_16.png";
    public static final String BlobFileIConPath = "/icons/BlobFile.png";
    public static final String HDExplorerIconName = "HdExplorer";
    public static final String HDExplorerIcon_13x_Path = "HdExplorer.png";
    public static final String HDInsightIConPath = "HdInsight.png";
    public static final String HDInsightGrayIconPath = "HdInsight_gray.png";
    public static final String ClusterIConPath = "Cluster.png";
    public static final String StorageAccountIConPath = "StorageAccount_16.png";
    public static final String ADLS_STORAGE_ACCOUNT_ICON_PATH = "adls_storageaccount.png";
    public static final String StorageAccountFoldIConPath = "StorageAccountFolder.png";
    public static final String BlobContainerIConPath = "BlobFile_16.png";
    public static final String JavaProjectIconPath = "/icons/Spark-Java.png";
    public static final String ScalaProjectIconPath = "/icons/Spark-Scala.png";
    public static final String StopIconPath = "/icons/Stop.png";
    public static final String StopDisableIconPath = "/icons/Stop-Disable.png";
    public static final String OpenSparkUIIconName = "OpenSparkUI";
    public static final String OpenSparkUIIconPath = "/icons/OpenSparkUI.png";
    public static final String OpenSparkUIDisableIconPath = "/icons/OpenSparkUI-Disable.png";
    public static final String SPARK_JOBVIEW_ICONPATH = "/icons/JobViewTitle.png";
    public static final String EmualtorPath = "/emulator/";
    public static final String EmulatorArchieveFileName = "service.zip";
    public static final String EmulatorSetupScriptFileName = "setup.sh";
    public static final String SparkFailureTaskDebugIcon_13x_Path = "/icons/ToolWindowSparkJobDebugger_13x.png";
    public static final String SparkFailureTaskDebugIcon_16x_Path = "/icons/ToolWindowSparkJobDebug.png";
    public static final String ToolWindowSparkJobRunIcon_13x_Path = "/icons/ToolWindowSparkJobRun_13x.png";
    public static final String ToolWindowSparkJobRunIcon_16x_Path = "/icons/ToolWindowSparkJobRun.png";
    public static final String ToolWindowSparkJobDebugIcon_13x_Path = "/icons/ToolWindowSparkJobDebugger_13x.png";
    public static final String ToolWindowSparkJobDebugIcon_16x_Path = "/icons/ToolWindowSparkJobDebug.png";

    public static final String ENABLE_HDINSIGHT_NEW_SDK = "Enable.HDInsight.New.SDK";
    public static final String HDINSIGHT_ADDITIONAL_CLUSTERS = "com.microsoft.azure.hdinsight.AdditionalClusters";
    public static final String HDINSIGHT_ADDITIONAL_MFA_CLUSTERS = "com.microsoft.azure.hdinsight.AdditionalMfaClusters";
    public static final String HDINSIGHT_LIVY_LINK_CLUSTERS = "com.microsoft.azure.hdinsight.LivyLinkClusters";
    public static final String SQL_BIG_DATA_LIVY_LINK_CLUSTERS = "com.microsoft.azure.sqlbigdata.SqlBigDataLivyLinkClusters";
    public static final String EMULATOR_CLUSTERS = "com.microsoft.azure.hdinsight.EmulatorClusters";
    public static final String CACHED_SPARK_SDK_PATHS = "com.microsoft.azure.hdinsight.cachedSparkSDKpath";
    public static final String SPARK_FAILURE_TASK_CONTEXT_EXTENSION = "ftd";

    public static final String AZURE_SERVERLESS_SPARK_ROOT_ICON_PATH = "AzureServerlessSparkRoot.png";
    public static final String AZURE_SERVERLESS_SPARK_ACCOUNT_ICON_PATH = "AzureServerlessSparkAccount.png";

    public static final String SQL_BIG_DATA_CLUSTER_MODULE_ICON_PATH = "SqlBigDataClusterRoot_16x.png";
    public static final String ARCADIA_WORKSPACE_MODULE_ICON_PATH = "ArcadiaRoot_16x.png";
    public static final String ARCADIA_WORKSPACE_NODE_ICON_PATH = "Workspace_13x.png";
    public static final String ARCADIA_OPEN_UI_NAME = "OpenArcadiaUI";
    public static final String DISABLE_SSL_CERTIFICATE_VALIDATION = "false";

    public static final String CosmosServerlessToolWindowIconName= "SparkSubmissionToolWindow";

    public static final String[] AZURE_LOGIN_HOSTS = new String[] {
            "login.windows.net",
            URI.create(CommonSettings.getAdEnvironment().getActiveDirectoryEndpoint()).getHost()
    };
}
