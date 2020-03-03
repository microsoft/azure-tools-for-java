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

package com.microsoft.azure.hdinsight.projects;

import java.util.Comparator;

public enum SparkVersion {
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
