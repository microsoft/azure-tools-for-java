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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WAEclipseHelperMethods {

    /**
     * To delete directory having contents within it.
     *
     * @param dir
     */
    public static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            // directory is empty, then delete it
            if (dir.list().length == 0) {
                dir.delete();
            } else {
                // list all the directory contents
                String[] subFiles = dir.list();
                for (int i = 0; i < subFiles.length; i++) {
                    // construct the file structure
                    File fileDelete = new File(dir, subFiles[i]);
                    // recursive delete
                    deleteDirectory(fileDelete);
                }
                // check the directory again, if empty then delete it
                if (dir.list().length == 0) {
                    dir.delete();
                }
            }
        } else {
            dir.delete();
        }
    }

    /**
     * Converts Windows path into regex including wildcards.
     *
     * @param windowsPath
     * @return
     */
    private static String windowsPathToRegex(String windowsPath) {
        if (windowsPath == null) {
            return null;
        }

        // Escape special characters
        String regex = windowsPath.replaceAll(
                "([\\\"\\+\\(\\)\\^\\$\\.\\{\\}\\[\\]\\|\\\\])", "\\\\$1");

        // Replace wildcards
        return regex.replace("*", ".*").replace("?", ".");
    }

    /**
     * Looks for a pattern in a text file
     *
     * @param file
     * @param patternText
     * @return True if a pattern is found, else false
     * @throws FileNotFoundException
     */
    private static boolean isPatternInFile(File file, String patternText) {
        Scanner fileScanner = null;

        if (file.isDirectory()) {
            return false;
        }
        try {
            fileScanner = new Scanner(file);
            Pattern pattern = Pattern.compile(patternText,
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = null;
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            if (fileScanner != null) {
                fileScanner.close();
            }
        }
    }

    /**
     * Returns default JDK path.
     *
     * @param currentlySelectedDir
     * @return
     */
    public static String jdkDefaultDirectory(String currentlySelectedDir) {
        File file;

        // Try currently selected JDK path
        String path = currentlySelectedDir;
        if (path != null && !path.isEmpty()) {
            file = new File(path);
            if (file.isDirectory() && file.exists()) {
                return path;
            }
        }

        // Try JAVA_HOME
        path = System.getenv("JAVA_HOME");
        if (path != null && !path.isEmpty()) {
            file = new File(path);
            if (file.exists() && file.isDirectory()) {
                // Verify presence of javac.exe
                File javacFile = new File(file, "bin" + File.separator
                        + "javac.exe");
                if (javacFile.exists()) {
                    return path;
                }
            }
        }

        // Try under %ProgramFiles%\Java
        path = String.format("%s%s%s", System.getenv("ProgramFiles"),
                File.separator, "Java", File.separator);
        file = new File(path);
        if (!file.exists() || !file.isDirectory()) {
            return "";
        }

        // Find the first entry under Java that contains jdk
        File[] jdkDirs = file.listFiles();
        Arrays.sort(jdkDirs);

        TreeSet<File> sortedDirs = new TreeSet<File>(Arrays.asList(jdkDirs));
        for (Iterator<File> iterator = sortedDirs.descendingIterator(); iterator
                .hasNext();) {
            File latestSdkDir = iterator.next();
            if (latestSdkDir.isDirectory()
                    && latestSdkDir.getName().contains("jdk")) {
                return latestSdkDir.getAbsolutePath();
            }
        }

        return "";
    }

    /**
     * This API compares if two files content is identical. It ignores extra
     * spaces and new lines while comparing
     *
     * @param sourceFile
     * @param destFile
     * @return
     * @throws Exception
     */
    public static boolean isFilesIdentical(File sourceFile, File destFile)
            throws Exception {
        try {
            Scanner sourceFileScanner = new Scanner(sourceFile);
            Scanner destFileScanner = new Scanner(destFile);

            while (sourceFileScanner.hasNext()) {
                /*
                 * If source file is having next token then destination file
                 * also should have next token, else they are not identical.
                 */
                if (!destFileScanner.hasNext()) {
                    destFileScanner.close();
                    sourceFileScanner.close();
                    return false;
                }
                if (!sourceFileScanner.next().equals(destFileScanner.next())) {
                    sourceFileScanner.close();
                    destFileScanner.close();
                    return false;
                }
            }
            /*
             * Handling the case where source file is empty and destination file
             * is having text
             */
            if (destFileScanner.hasNext()) {
                destFileScanner.close();
                sourceFileScanner.close();
                return false;
            } else {
                destFileScanner.close();
                sourceFileScanner.close();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Method checks if text contains alphanumeric characters, underscore only
     * and is starting with alphanumeric or underscore. \p{L} any kind of letter
     * from any language. \p{Nd} a digit zero through nine in any script except
     * ideographic scripts.
     *
     * @param text
     * @return Boolean
     */
    public static Boolean isAlphaNumericUnderscore(String text) {
        Pattern alphaNumUndscor = Pattern
                .compile("^[\\p{L}_]+[\\p{L}\\p{Nd}_]*$");
        Matcher m = alphaNumUndscor.matcher(text);
        return m.matches();
    }

    public static Boolean isAlphaNumericHyphen(String text) {
        Pattern alphaNumUndscor = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$");
        Matcher m = alphaNumUndscor.matcher(text);
        return m.matches();
    }

    /**
     * Method checks if text contains alphanumeric lower case characters and
     * integers only.
     *
     * @param text
     * @return
     */
    public static Boolean isLowerCaseAndInteger(String text) {
        Pattern lowerCaseInteger = Pattern.compile("^[a-z0-9]+$");
        Matcher m = lowerCaseInteger.matcher(text);
        return m.matches();
    }

    /**
     * Copy file from source to destination.
     *
     * @param source
     * @param destination
     * @throws Exception
     */
    public static void copyFile(String source, String destination)
            throws Exception {
        File f1 = new File(source);
        File f2 = new File(destination);
        copyFile(f1, f2);
    }

    /**
     * Copy file from source to destination.
     *
     * @param f1 source
     * @param f2 destination
     * @throws Exception
     */
    public static void copyFile(File f1, File f2)
            throws Exception {
        try {
            InputStream in = new FileInputStream(f1);
            OutputStream out = new FileOutputStream(f2);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * Method checks whether URL is blob URL or not. It should satisfy pattern
     * http[s]://<storage-account-name> -->(only lower case letters and numbers
     * allowed) . -->(exactly one dot is required) <blob-service-endpoint>
     * -->(only lower case letters, numbers and period allowed) / -->(exactly
     * one forward slash is required) <container-name> --> (only lower case
     * letters, numbers and '-' allowed) must start with letter or number, no
     * consecutive dashes allowed must be of 3 through 63 characters long /
     * -->(exactly one forward slash is required) <blob-name> -->(may contain
     * upper lower case characters, numbers and punctuation marks) must be of 1
     * through 1024 characters long
     *
     * @param text
     * @return
     */
    public static Boolean isBlobStorageUrl(String text) {
        Pattern blob = Pattern
                .compile("^https?://[a-z0-9]+\\.{1}[a-z0-9.]+/{1}([a-z]|\\d){1}([a-z]|-|\\d){1,61}([a-z]|\\d){1}/{1}[\\w\\p{Punct}]+$");
        Matcher m = blob.matcher(text);
        return m.matches();
    }

    /**
     * Method checks whether text is valid application insights instrumentation key or not.
     * Key is of the format "65e3ff09-1pq7-4e0e-9a36-3282d6c5d700"
     * 8-4-4-4-12 --> eight lower case letters or numbers followed by single dash '-' and so on.
     * @param text
     * @return
     */
    public static Boolean isValidInstrumentationKey(String text) {
        Pattern key = Pattern.compile("([a-z0-9]){8}-{1}([a-z0-9]){4}-{1}([a-z0-9]){4}-{1}([a-z0-9]){4}-{1}([a-z0-9]){12}");
        Matcher m = key.matcher(text);
        return m.matches();
    }

    /**
     * Method checks whether text is valid storage account access key or not.
     * "SK3d/vC2dIl3eaVGRs8W61FW43bO1ubDOAHD3s9TysDsq3qSBF4grpz0K2mg0pUKPx87wJwS5A8oaaXpR1VhVg=="
     * may contain upper lower case characters, numbers and punctuation marks and 88 characters long.
     * @param text
     * @return
     */
    public static Boolean isValidStorageAccAccessKey(String text) {
        Pattern key = Pattern.compile("[\\w\\p{Punct}]{88}");
        Matcher m = key.matcher(text);
        return m.matches();
    }

    public static Boolean isContainsBlobEndpointUrl(String text) {
        Pattern blob = Pattern
                .compile("^https?://[a-z0-9]+\\.{1}[a-z0-9.]+/{1}[\\w\\p{Punct}]*$");
        Matcher m = blob.matcher(text);
        return m.matches();
    }

    /**
     * API to find the hostname. This API first checks OS type. If Windows then
     * tries to get hostname from computername environment variable else uses
     * environment variable hostname.
     *
     * In case if hostname is not found from environment variable then uses java
     * networking apis
     *
     */
    public static String getHostName() {
        String hostOS = System.getProperty("os.name");
        String hostName = null;

        // Check host Operating System and get value of hostname.
        if (hostOS != null && hostOS.indexOf("Win") >= 0) {
            hostName = System.getenv("COMPUTERNAME");
        } else { // non-windows platforms
            hostName = System.getenv("HOSTNAME");
        }

        // If hostname is still null , use java network apis
        try {
            if (hostName == null || hostName.isEmpty()) {
                hostName = InetAddress.getLocalHost().getHostName();
            }
        } catch (Exception ex) { // catches UnknownHostException
            // just ignore this exception
        }

        if (hostName == null || hostName.isEmpty()) { // most probabily this
            // case won't happen
            hostName = "localhost";
        }

        return hostName;
    }

    /*
    public static List<String> prepareListToDisplay(Map<WebSite, WebSiteConfiguration> webSiteConfigMap, List<WebSite> webSiteList) {
        // prepare list to display
        List<String> listToDisplay = new ArrayList<String>();
        for (WebSite webSite : webSiteList) {
            WebSiteConfiguration webSiteConfiguration = webSiteConfigMap.get(webSite);
            StringBuilder builder = new StringBuilder(webSite.getName());
            if (!webSiteConfiguration.getJavaVersion().isEmpty()) {
                builder.append(" (JRE ");
                builder.append(webSiteConfiguration.getJavaVersion());
                if (!webSiteConfiguration.getJavaContainer().isEmpty()) {
                    builder.append("; ");
                    builder.append(webSiteConfiguration.getJavaContainer());
                    builder.append(" ");
                    builder.append(webSiteConfiguration.getJavaContainerVersion());
                }
                builder.append(")");
            } else {
                builder.append(" (.NET ");
                builder.append(webSiteConfiguration.getNetFrameworkVersion());
                if (!webSiteConfiguration.getPhpVersion().isEmpty()) {
                    builder.append("; PHP ");
                    builder.append(webSiteConfiguration.getPhpVersion());
                }
                builder.append(")");
            }
            listToDisplay.add(builder.toString());
        }
        return listToDisplay;
    }*/
}
