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

package com.microsoft.azuretools.ui.embeddedbrowser;

import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
/**
 * Use JxBrowser to load URL for Swing, AWT or JavaFX UIs
 */
public class JxBrowserUtil {
    public static final Logger log = Logger.getLogger(JxBrowserUtil.class.getName());
    public static int MAX_RETRY_TIMES = 3;
    public static int MAX_WAIT_TIME_IN_SECONDS = 120;

    public static final LoadedClassesAndMethods loadedClasses = new LoadedClassesAndMethods();

    /** Mapping OS to package name */
    private static final Map<String, String> platformToJxBrowserJar = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("win32", "jxbrowser-win32-6.16.jar");
                put("win64", "jxbrowser-win32-6.16.jar");
                put("mac", "jxbrowser-mac-6.16.jar");
                put("linux64", "jxbrowser-linux64-6.16.jar");
                put("linux32", "jxbrowser-linux64-6.16.jar");
            }}
        );

    private static final String JXBROWSER_CLASS_BROWSERVIEW = "com.teamdev.jxbrowser.chromium.swing.BrowserView";
    private static final String JXBROWSER_CLASS_BROWSER = "com.teamdev.jxbrowser.chromium.Browser";
    private static final String AZURE_BLOB_URI = "https://hdinsighttool.azureedge.net";
    private static final String JXBROWSER_LICENSE_FILE = "license.jar";
    private static final String JXBROWSER_COMMON_JAR = "jxbrowser-6.16.jar";
    private static final int RETRY_BACKOFF_FACTOR = 3;

    /**
     * Return JComponent to be used in Swing
     * @param url
     * @return JComponent (JxBrowser BrowserView) that can be added to Swing UI
     */
    public static CompletableFuture<JComponent> createBrowserViewAndLoadURL(@NotNull final String url, final String targetPath) {
        log.fine("Start to download, create browser view and load URL at time " + System.currentTimeMillis());
        CompletableFuture<JComponent> downloadAndLoadClassesTask = CompletableFuture.supplyAsync(() -> {
            synchronized (JxBrowserUtil.class) {
                if (!loadedClasses.isLoaded()) {
                    try {
                        downloadAndLoadClasses(targetPath);
                    } catch (JxBrowserException e) {
                        throw new CompletionException(e);
                    }
                };
            }

            if (!loadedClasses.isLoaded()) {
                log.severe("Class JxBrowser is not loaded at time " + System.currentTimeMillis());
                throw new CompletionException(new JxBrowserException("Class JxBrowser is not loaded"));
            }

            try {
                Object browser = loadedClasses.browserClass.newInstance();

                Object browserView = loadedClasses.browserViewConstructor.newInstance(browser);

                if (browserView != null) {
                    loadedClasses.loadURLMethod.invoke(browser, url);
                }

                return (JComponent)browserView;
            } catch (Exception e) {
                log.severe("Class JxBrowser is not loaded at time " +  + System.currentTimeMillis());
                throw new CompletionException(e);
            }
        });

        log.fine("Finished downloading, creating browser view and loading URL at " + System.currentTimeMillis());

        return downloadAndLoadClassesTask;
    }

    /**
     * Download and load classes
     * @param targetPath
     * @throws JxBrowserException
     */
    private static void downloadAndLoadClasses(String targetPath) throws JxBrowserException {
        try {
            Path jxBrowserJarPath = Paths.get(targetPath, getJxBrowserJarFileName());
            Path licenseJarPath = Paths.get(targetPath, JXBROWSER_LICENSE_FILE);
            Path commonJarPath = Paths.get(targetPath, JXBROWSER_COMMON_JAR);
            File jxBrowserJarFile = jxBrowserJarPath.toFile();
            File licenseJarFile = licenseJarPath.toFile();
            File commonJarFile = commonJarPath.toFile();

            if (!licenseJarFile.exists() || !licenseJarFile.isFile()
                    || !jxBrowserJarFile.exists() || !jxBrowserJarFile.isFile()
                    || !commonJarFile.exists() || !commonJarFile.isFile()) {
                FileUtils.deleteQuietly(jxBrowserJarFile);
                FileUtils.deleteQuietly(licenseJarFile);
                FileUtils.deleteQuietly(commonJarFile);
                downloadFilesFromAzure(Arrays.asList(jxBrowserJarPath, commonJarPath, licenseJarPath));
            }

            URL[] urls = new URL[] {commonJarFile.toURI().toURL(), jxBrowserJarFile.toURI().toURL(), licenseJarFile.toURI().toURL()};

            ClassLoader classLoader = new URLClassLoader(urls, JxBrowserUtil.class.getClassLoader());

            loadedClasses.browserClass = Class.forName(JXBROWSER_CLASS_BROWSER, true, classLoader);
            loadedClasses.browserViewClass = Class.forName(JXBROWSER_CLASS_BROWSERVIEW, true, classLoader);
            loadedClasses.loadURLMethod = loadedClasses.browserClass.getMethod("loadURL", String.class);
            loadedClasses.browserViewConstructor = loadedClasses.browserViewClass.getConstructor(loadedClasses.browserClass);
        } catch (Exception e) {
            throw new JxBrowserException("Fail to load JxBrowser or load URL: " + e.getMessage());
        }
    }

    /**
     * Download JxBrowser jars (i.e. jxbrowser-win32-6.16.jar and license.jar) and check digest (with retries) into given targetPath
     * @param filePathNames
     * @return
     */
    private static boolean downloadFilesFromAzure(@NotNull List<Path> filePathNames) {
        boolean result = true;
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        try {
            downloadAndCheckDigest(threadPool, filePathNames);

            threadPool.shutdown();
            threadPool.awaitTermination((long) (MAX_WAIT_TIME_IN_SECONDS * Math.pow(RETRY_BACKOFF_FACTOR, MAX_RETRY_TIMES)), TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.severe("Download JxBrowser jars failed with exception " + ex.getMessage());
            result = false;
        } finally {
            threadPool.shutdownNow();
        }

        return result;
    }

    /**
     * Check the file correctness by comparing the MD5 digest with the digest file. Return true if both file and digest exist and match.
     * @param filePathName
     * @param digestFilePathName
     * @return
     */
    private static boolean checkFileDigest(@NotNull Path filePathName, @NotNull Path digestFilePathName) {
        File targetFile = filePathName.toFile();
        if (!targetFile.exists() || !targetFile.isFile()) {
            log.fine(filePathName + " doesn't exist");
            return false;
        }

        File digestFile = digestFilePathName.toFile();
        if (!digestFile.exists() || !digestFile.isFile()) {
            log.fine(digestFilePathName + " doesn't exist");
            return false;
        }

        FileInputStream targetFileStream = null;
        String digestFileContent = "content";
        String digest = "digest";
        boolean result = true;
        try {
            targetFileStream = new FileInputStream(targetFile);
            digest = DigestUtils.md5Hex(targetFileStream).toUpperCase();
            digestFileContent = FileUtils.readFileToString(digestFile, "UTF-8");
            targetFileStream.close();
        } catch (IOException e) {
            log.warning(String.format("Fail to read file %s or digest %s", filePathName, digestFilePathName));
            result = false;
        } finally {
            IOUtils.closeQuietly(targetFileStream);
        }

        if (digestFileContent.equals(digest) == false) {
            log.warning(String.format("Check sum error for file %s and digest file %s", filePathName, digestFilePathName));
            return false;
        }

        return result;
    }

    /**
     * Download file from Azure and save to target path specified. Return true if download succeeds
     * @param filePathName
     * @return
     */
    private static Boolean downloadFromAzure(@NotNull Path filePathName) {
        boolean result = true;
        try {
            URI cloudBlobUri = new URI(AZURE_BLOB_URI);
            CloudBlobClient serviceClient = new CloudBlobClient(cloudBlobUri);
            CloudBlobContainer container = serviceClient.getContainerReference("libcontainer1");
            CloudBlockBlob blob = container.getBlockBlobReference(filePathName.getFileName().toString());
            File downloadingFile = filePathName.toFile();
            blob.downloadToFile(downloadingFile.getAbsolutePath());
        } catch (Exception e) {
            result = false;
            log.warning("Fail to download file from Azure: " + e.getMessage());
        }

        return result;
    }

    /**
     * Download file and check digest with retry. Return false if all retry fails
     * @param threadPool
     * @param filePathNameList
     * @param retryLeft
     * @param waitTime
     * @return
     */
    private static boolean downloadAndCheckDigest(@NotNull ExecutorService threadPool, @NotNull List<Path> filePathNameList, int retryLeft, int waitTime) {
        List<Path> filePathNames = new ArrayList<>(filePathNameList);
        List<Path> digestPathNames = new ArrayList<>();
        for (int i = 0; i < filePathNames.size(); i++) {
            digestPathNames.add(Paths.get(FilenameUtils.removeExtension(filePathNames.get(i).toString()) + ".md5"));
        }

        boolean result = true;
        try {
            int i = filePathNames.size() - 1;
            for (; i >= 0; i--) {
                if (checkFileDigest(filePathNames.get(i), digestPathNames.get(i)))  {
                    filePathNames.remove(i);
                    digestPathNames.remove(i);
                }
            }

            if (filePathNames.isEmpty()) {
                return true;
            }

            if (retryLeft < 0) {
                log.warning("Maximum retry time reached. Fail to download" + filePathNames.get(0));
                return false;
            }

            for (i = 0; i < filePathNames.size(); i++) {
                FileUtils.deleteQuietly(filePathNames.get(i).toFile());
                FileUtils.deleteQuietly(digestPathNames.get(i).toFile());
            }

            List<Callable<Boolean>> downloadThreads = new ArrayList<>();
            for (i = 0; i < filePathNames.size(); i++) {
                final Path filePathName = filePathNames.get(i);
                final Path digestPathName = digestPathNames.get(i);
                downloadThreads.add(() -> downloadFromAzure(filePathName));
                downloadThreads.add(() -> downloadFromAzure(digestPathName));
            };

            List<Future<Boolean>> results = threadPool.invokeAll(downloadThreads, waitTime, TimeUnit.SECONDS);

            for (i = 0; i < results.size(); i++) {
                if (!results.get(i).get()) {
                    result = false;
                }
            }
        } catch (Exception ex) {
            result = false;
            log.warning(String.format("Download fileName %s fail with exception %s", (filePathNames.size() > 0 ? filePathNames.get(0) : ""), ex.getMessage()));
        } finally {
            if (!result) {
                log.fine("Start to download again with retry count " + retryLeft + " and wait time " + waitTime);
                result = downloadAndCheckDigest(threadPool, filePathNames, retryLeft - 1, waitTime * RETRY_BACKOFF_FACTOR);
            }
        }

        return result;
    }

    private static boolean downloadAndCheckDigest(@NotNull ExecutorService threadPool, @NotNull List<Path> filePathNames) {
        return downloadAndCheckDigest(threadPool, filePathNames, MAX_RETRY_TIMES, MAX_WAIT_TIME_IN_SECONDS);
    }

    private static String getJxBrowserJarFileName() throws JxBrowserException {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        log.fine("osName is : " + osName + ", osArch is: " + osArch);

        String result = "";
        if (osName.contains("mac") || osName.contains("darwin")) {
            result = platformToJxBrowserJar.get("mac");
        } else if (osName.contains("win")) {
            if (osArch.contains("64")) {
                result = platformToJxBrowserJar.get("win64");
            } else {
                result = platformToJxBrowserJar.get("win32");
            }
        } else if (osName.contains("nix") || osName.contains("nux")) {
            if (osArch.contains("64")) {
                result = platformToJxBrowserJar.get("linux64");
            } else {
                result = platformToJxBrowserJar.get("linux32");
            }
        } else {
            throw new JxBrowserException("Cannot recognize the operation system." + osName);
        }


        return result;
    }

    private static class LoadedClassesAndMethods {
        volatile static Class<?> browserClass = null;
        volatile static Class<?> browserViewClass = null;
        volatile static Constructor<?> browserViewConstructor = null;
        volatile static Method loadURLMethod = null;


        public static boolean isLoaded() {
            return browserClass != null && browserViewClass != null && browserViewConstructor != null && loadURLMethod != null;
        }
    }
}
