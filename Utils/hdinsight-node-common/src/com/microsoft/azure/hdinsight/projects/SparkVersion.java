/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.projects;

import java.util.Comparator;

public enum SparkVersion {
    SPARK_3_0_1("3.0.1", "2.12.12","2.12"),
    SPARK_2_4_0("2.4.0", "2.11.12","2.11"),
    SPARK_2_3_2("2.3.2", "2.11.8", "2.11"),
    SPARK_2_3_0("2.3.0", "2.11.8", "2.11"),
    SPARK_2_2_0("2.2.0", "2.11.8", "2.11"),
    SPARK_2_1_0("2.1.0", "2.11.8", "2.11"),
    SPARK_2_0_2("2.0.2", "2.11.8", "2.11"),
    SPARK_1_6_3("1.6.3", "2.10.5", "2.10"),
    SPARK_1_6_2("1.6.2", "2.10.5", "2.10"),
    SPARK_1_5_2("1.5.2", "2.10.4", "2.10");

    private final String sparkVersion;
    private final String scalaVersion;
    private final String scalaVer;

    SparkVersion(String sparkVersion, String scalaVersion, String scalaVer) {
        this.sparkVersion = sparkVersion;
        this.scalaVersion = scalaVersion;
        this.scalaVer  = scalaVer;
    }

    @Override
    public String toString() {
        return String.format("Spark %s (Scala %s)", this.sparkVersion, this.scalaVersion);
    }

    public static SparkVersion parseString(String strSparkVersion) {
        String[] tokens = strSparkVersion.split(" ");
        for (SparkVersion sparkVersion : SparkVersion.class.getEnumConstants()) {
            if (sparkVersion.getSparkVersion().equalsIgnoreCase(tokens[1])) {
                if (tokens[3].contains(sparkVersion.getScalaVersion())) {
                    return sparkVersion;
                }
            }
        }

        return SparkVersion.class.getEnumConstants()[0];
    }

    public String getSparkVersion() {
        return sparkVersion;
    }

    public String getScalaVersion() {
        return scalaVersion;
    }

    public String getScalaVer() {
        return scalaVer;
    }

    public String getSparkVersioninDashFormat() {
        return sparkVersion.replace(".", "_") + "_";
    }

    public static Comparator<SparkVersion> sparkVersionComparator = (v1, v2) -> {
        String[] v1Vers = v1.getSparkVersion().split("\\.");
        String[] v2Vers = v2.getSparkVersion().split("\\.");

        int majorVerResult = Integer.parseInt(v1Vers[0]) - Integer.parseInt(v2Vers[0]);
        int minorVerResult = Integer.parseInt(v1Vers[1]) - Integer.parseInt(v2Vers[1]);
        int patchVerResult = Integer.parseInt(v1Vers[2]) - Integer.parseInt(v2Vers[2]);

        return majorVerResult != 0 ? majorVerResult :
                                    (minorVerResult != 0 ? minorVerResult : patchVerResult);
    };
}
