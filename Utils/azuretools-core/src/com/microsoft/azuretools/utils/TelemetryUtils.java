package com.microsoft.azuretools.utils;

import java.io.File;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.util.GetHashMac;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;

public class TelemetryUtils {
    @NotNull
    public static String getMachieId(String dataFile, String prefVal, String instId) {
        String ret = "";
        if (new File(dataFile).exists()) {
            String prefValue = DataOperations.getProperty(dataFile, prefVal);
            if (prefValue != null && prefValue.equalsIgnoreCase("false")) {
                return ret;
            }
            ret = DataOperations.getProperty(dataFile, instId);
            if (ret == null || ret.isEmpty() || !GetHashMac.IsValidHashMacFormat(ret)) {
                ret = GetHashMac.GetHashMac();
            }
        } else {
            ret = GetHashMac.GetHashMac();
        }

        return ret;
    }
}
