package com.microsoft.azure.hdinsight.sdk.cluster;

import cucumber.api.DataTable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IClusterDetailMock{
    static public IClusterDetail factoryFromMap(Map<String, String> clusterConfig) {
        IClusterDetail clusterDetailMock = mock(IClusterDetail.class);

        when(clusterDetailMock.isEmulator()).thenReturn(clusterConfig.getOrDefault("<IsEmulator>", "false").toLowerCase().equals("true"));
        when(clusterDetailMock.getName()).thenReturn(clusterConfig.get("<Name>"));
        when(clusterDetailMock.getConnectionUrl()).thenReturn(clusterConfig.get("<ConnectionUrl>"));
        when(clusterDetailMock.getCreateDate()).thenReturn(clusterConfig.get("<CreateDate>"));
        when(clusterDetailMock.getDataNodes()).thenReturn(new Integer(clusterConfig.getOrDefault("<DataNodes>", "0")));
        try {
            when(clusterDetailMock.getHttpPassword()).thenReturn(clusterConfig.get("<HttpPassword>"));
            when(clusterDetailMock.getHttpUserName()).thenReturn(clusterConfig.get("<HttpUserName>"));
        } catch (Exception ignore) {}
        when(clusterDetailMock.getLocation()).thenReturn(clusterConfig.get("<Location>"));
        when(clusterDetailMock.getOSType()).thenReturn(clusterConfig.get("<OSType>"));
        when(clusterDetailMock.getResourceGroup()).thenReturn(clusterConfig.get("<ResourceGroup>"));
        when(clusterDetailMock.getSparkVersion()).thenReturn(clusterConfig.get("<SparkVersion>"));
        when(clusterDetailMock.getState()).thenReturn(clusterConfig.get("<State>"));
        when(clusterDetailMock.getVersion()).thenReturn(clusterConfig.get("<Version>"));
        when(clusterDetailMock.getType()).thenReturn(ClusterType.valueOf(clusterConfig.getOrDefault("<Type>", ClusterType.unkown.toString())));

        return clusterDetailMock;
    }

    /**
     * Generate IClusterDetail list from Data Table like this:
     *  | <Name>   | <ConnectionUrl> | <CreateDate> | <DataNodes> | <HttpUserName> | <HttpPassword> | <Location> | <OSType> | <ResourceGroup> | <SparkVersion> | <State> | <Version> | <Type> |
     *  | nameMock | urlMock         | 2017-01-01   | 5           | userMock       | passwordMock   | localMock  |  osMock   | rmMock          | 1.6              | idle  | 3.4       | spark  |
     *
     *  The <Type> value depends on ClusterType enum, which can be one of the following items:
     *      unkown, hadoop, hbase, storm, kafka, interactivehive, spark
     *     *
     * @param clustersTableConfig The data table from cucumber feature file
     * @return mocked IClusterDetail list
     * @throws Throwable any issues
     */
    static public List<IClusterDetail> listFactoryFromTable(DataTable clustersTableConfig) throws Throwable {
        List<IClusterDetail> clusterDetailsMock = clustersTableConfig.asMaps(String.class, String.class).stream()
                .map(IClusterDetailMock::factoryFromMap)
                .collect(Collectors.toList());

        return clusterDetailsMock;
    }
}
