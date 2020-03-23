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

package com.microsoft.azuretools.azurecommons.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Utils {

    private static final Integer RES_GRP_MAX_NAME_LENGTH = 90;
    private static final String RES_GRP_REGEX = "^[A-Za-z0-9().\\-_]+(?<!\\.)$";

    public static String replaceLastSubString(String location, String find, String replaceWith) {
        if (location == null || location.isEmpty())
            return location;

        int lastIndex = location.lastIndexOf(find);

        if (lastIndex < 0)
            return location;

        String end = location.substring(lastIndex).replaceFirst(find, replaceWith);
        return location.substring(0, lastIndex) + end;
    }

    public static String getDefaultCNName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date now = new Date();
        return "Self Signed Certificate " + dateFormat.format(now);
    }

    /**
     * This method is used for invoking native commands.
     *
     * @param command
     *            :- command to invoke.
     * @param ignoreErrorStream
     *            : Boolean which controls whether to throw exception or not based on error stream.
     * @return result :- depending on the method invocation.
     * @throws Exception
     * @throws IOException
     */
    public static String cmdInvocation(String[] command, boolean ignoreErrorStream) throws Exception, IOException {
        String result = "";
        String error = "";
        InputStream inputStream = null;
        InputStream errorStream = null;
        BufferedReader br = null;
        BufferedReader ebr = null;
        try {
            Process process = new ProcessBuilder(command).start();
            ;
            inputStream = process.getInputStream();
            errorStream = process.getErrorStream();
            br = new BufferedReader(new InputStreamReader(inputStream));
            result = br.readLine();
            process.waitFor();
            ebr = new BufferedReader(new InputStreamReader(errorStream));
            error = ebr.readLine();
            if (error != null && (!error.equals(""))) {
                // To do - Log error message

                if (!ignoreErrorStream) {
                    throw new Exception(error, null);
                }
            }
        } catch (Exception e) {
            throw new Exception("Exception occurred while invoking command", e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (errorStream != null) {
                errorStream.close();
            }
            if (br != null) {
                br.close();
            }
            if (ebr != null) {
                ebr.close();
            }
        }
        return result;
    }

    public static boolean isPortAvailable(int port) throws Exception {
        Socket s = null;
        try {
            s = new Socket("localhost", port);
            // something is using the port and has responded
            // port not available
            return false;
        } catch (IOException e) {
            return true;
        } finally {
            if (s != null) {
                try {
                    s.close();
                    Thread.sleep(3000);
                } catch (Exception e) {
                    throw new Exception(e.getMessage());
                }
            }
        }
    }

    public static List<String> getJarEntries(String jarName, String entryName) throws IOException {
        List<String> files = new ArrayList<String>();
        System.out.println("entryName = " + entryName);
        JarURLConnection urlConnection = (JarURLConnection) new URL("jar:file:" + jarName + "!/" + entryName)
                .openConnection();
        // URLConnection urlConnection = originUrl.openConnection();
        JarFile jarFile = urlConnection.getJarFile();
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith(urlConnection.getEntryName())) {
                if (!entry.isDirectory()) {
                    files.add(entry.getName());
                }
            }
        }
        return files;
    }

    /*
     * when there is version upgrade, if the existed version >= 3.x, then respect the recorded telemetry preference;
     * Otherwise, overwrite it to "true";
     */
    public static boolean whetherUpdateTelemetryPref(String recordedVersion) {
        if (recordedVersion == null || recordedVersion.isEmpty()) {
            return true;
        }

        if (compareVersion(recordedVersion, "3.0") >= 0) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * return negative: v1 < v2 return 0; v1 = v2 return positive: v1 > v2
     */
    public static int compareVersion(String version1, String version2) {
        if ((version1 == null || version1.isEmpty()) && (version2 == null || version2.isEmpty())) {
            return 0;
        }

        if (version1 == null || version1.isEmpty()) {
            return -1;
        }

        if (version2 == null || version2.isEmpty()) {
            return 1;
        }

        // neither is null
        String[] version1Seg = version1.split("\\.");
        String[] version2Seg = version2.split("\\.");

        int i = 0;
        while (i < version1Seg.length && i < version2Seg.length && version1Seg[i].equalsIgnoreCase(version2Seg[i])) {
            i++;
        }

        if (i < version1Seg.length && i < version2Seg.length) {
            return Integer.valueOf(version1Seg[i]).compareTo(Integer.valueOf(version2Seg[i]));
        }

        return version1Seg.length - version2Seg.length;
    }

    public static boolean isEmptyString(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static void copyToSystemClipboard(String key) throws Exception {
        StringSelection stringSelection = new StringSelection(key);
        Toolkit toolKit = Toolkit.getDefaultToolkit();
        if (toolKit == null) {
            throw new Exception("Cannot copy to system clipboard.");
        }
        Clipboard clipboard = toolKit.getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    public static boolean isResGrpNameValid(String name) {
        if (null == name || name.length() > RES_GRP_MAX_NAME_LENGTH || !name.matches(RES_GRP_REGEX)) {
            return false;
        }
        return true;
    }

    public static String getPrettyJson(String jsonString) {
        try {
            Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
            JsonParser parser = new JsonParser();
            JsonElement je = parser.parse(jsonString);
            return gson.toJson(je);
        } catch (Exception ignore) {
            return jsonString;
        }
    }

    public static String getOS() {
        return System.getProperty("os.name");
    }

    public static boolean isMac() {
        return StringUtils.containsIgnoreCase(getOS(), "mac");
    }

    public static boolean isWindows() {
        return StringUtils.containsIgnoreCase(getOS(), "windows");
    }
}
