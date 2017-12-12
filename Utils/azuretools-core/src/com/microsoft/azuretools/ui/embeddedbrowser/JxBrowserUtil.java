/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.ui.embeddedbrowser;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.utils.StorageAccoutUtils;
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
    public static int MAX_WAIT_TIME_IN_SECONDS = 300;

    /** Mapping OS to package name */
    private static final Map<String, String> platformToJxBrowserJar = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("win32", "jxbrowser-win32-6.16.jar");
                put("win64", "jxbrowser-6.16.jar");
                put("mac", "jxbrowser-mac-6.16.jar");
                put("linux64", "jxbrowser-linux64-6.16.jar");
                put("linux32", "jxbrowser-linux64-6.16.jar");
            }}
        );

    private static final String JXBROWSER_CLASS_BROWSERVIEW = "com.teamdev.jxbrowser.chromium.swing.BrowserView";
    private static final String JXBROWSER_CLASS_BROWSER = "com.teamdev.jxbrowser.chromium.Browser";
    private static final String AZURE_ACCOUNT_NAME = "ltiantest";
    private static final String AZURE_ACCOUNT_KEY = "UBCWXXk1ha9ktwa5WTFN0MNSQOGMnyFg5KQvruM+AX4nQHxIQ7LjpTTIQtAM7ZsRbkhayUqy1AT80V+ZPssQFA==";
    private static final String JXBROWSER_LICENSE_FILE = "license.jar";

    /**
     * Async version of createBrowserViewAndLoadURL, return FutureTask<JComponent>
     * @param url
     * @return
     */
    public static FutureTask<JComponent> createBrowserViewAndLoadURLAsync(@NotNull final String url, final String targetPath) {
        FutureTask<JComponent> createAndLoadTask = new FutureTask<> (new Callable<JComponent>() {
            @Override
            public JComponent call() throws JxBrowserException {
                return createBrowserViewAndLoadURL(url, targetPath);
            }
        });

        Thread thread = new Thread(createAndLoadTask);
        thread.setDaemon(true);
        thread.start();

        return createAndLoadTask;
    }

    /**
     * Create Swing JComponent, load the URL
     * @param url
     * @return JComponent (JxBrowser BrowserView) that can be added to Swing UI
     * @throws Exception Re-throw exception like ClassNotFound, etc.
     */
    private static JComponent createBrowserViewAndLoadURL(@NotNull String url, String targetPath) throws JxBrowserException {
        Object browserView = null;
        try {
            Path jxBrowserJarPath = Paths.get(targetPath, getJxBrowserJarFileName());
            Path licenseJarPath = Paths.get(targetPath, JXBROWSER_LICENSE_FILE);
            File jxBrowserJarFile = jxBrowserJarPath.toFile();
            File licenseJarFile = licenseJarPath.toFile();
            if (!licenseJarFile.exists() || !licenseJarFile.isFile() || !jxBrowserJarFile.exists() || !jxBrowserJarFile.isFile()) {
                FileUtils.deleteQuietly(jxBrowserJarFile);
                FileUtils.deleteQuietly(licenseJarFile);
                downloadJxBrowserJarsFromAzure(jxBrowserJarPath, licenseJarPath);
            }

            URL[] urls = new URL[] {jxBrowserJarFile.toURI().toURL(), licenseJarFile.toURI().toURL()};

            ClassLoader classLoader = new URLClassLoader(urls, JxBrowserUtil.class.getClassLoader());

            Class<?> browserClass = Class.forName(JXBROWSER_CLASS_BROWSER, true, classLoader);
            Object browser = browserClass.newInstance();
            Class<?> browserViewClass = Class.forName(JXBROWSER_CLASS_BROWSERVIEW, true, classLoader);
            Constructor<?> browserViewConstructor = browserViewClass.getConstructor(browserClass);
            browserView = browserViewConstructor.newInstance(browser);

            if (browserView != null) {
                Method loadURLMethod = browserClass.getMethod("loadURL", String.class);
                loadURLMethod.invoke(browser, url);
            }
        } catch (Exception e) {
            throw new JxBrowserException("Fail to load JxBrowser or load URL: " + e.getMessage());
        }

        return (JComponent)browserView;
    }

    /**
     * Download JxBrowser jars (i.e. jxbrowser-win32-6.16.jar and license.jar) and check digest (with retries) into given targetPath
     * @param jxBrowserJarPath
     * @param licenseJarPath
     * @return
     */
    private static boolean downloadJxBrowserJarsFromAzure(@NotNull Path jxBrowserJarPath, @NotNull Path licenseJarPath) {
        boolean result = true;
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        try {
            downloadAndCheckDigest(threadPool, jxBrowserJarPath);
            downloadAndCheckDigest(threadPool, licenseJarPath);

            threadPool.shutdown();
            threadPool.awaitTermination(MAX_WAIT_TIME_IN_SECONDS * MAX_RETRY_TIMES, TimeUnit.SECONDS);
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
            log.warning(filePathName + "doesn't exist");
            return false;
        }

        File digestFile = digestFilePathName.toFile();
        if (!digestFile.exists() || !digestFile.isFile()) {
            log.warning(digestFilePathName + "doesn't exist");
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
        } catch (IOException e) {
            log.warning(String.format("Fail to read file %s or digest %s", filePathName, digestFilePathName));
            result = false;
        } finally {
            IOUtils.closeQuietly(targetFileStream);
        }

        return result == true && digestFileContent.equals(digest);
    }

    /**
     * Download file from Azure and save to target path specified. Return true if download succeeds
     * @param filePathName
     * @return
     */
    private static Boolean downloadFromAzure(@NotNull Path filePathName) {
        Boolean result = true;
        try {
            CloudStorageAccount account = CloudStorageAccount.parse(StorageAccoutUtils.getConnectionString(AZURE_ACCOUNT_NAME, AZURE_ACCOUNT_KEY));
            CloudBlobClient serviceClient = account.createCloudBlobClient();
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
     * @param filePathName
     * @param currentRetry
     * @return
     */
    private static boolean downloadAndCheckDigest(@NotNull ExecutorService threadPool, @NotNull final Path filePathName, int currentRetry) {
        final Path digestPathName = Paths.get(FilenameUtils.removeExtension(filePathName.toString()) + ".md5");

        boolean result = true;
        try {
            if (checkFileDigest(filePathName, digestPathName) == true) {
                return true;
            }

            if (currentRetry > MAX_RETRY_TIMES) {
                log.warning("Maximum retry time reached. Fail to download" + filePathName);
                return false;
            }

            FileUtils.deleteQuietly(filePathName.toFile());
            FileUtils.deleteQuietly(digestPathName.toFile());

            List<Callable<Boolean>> downloadThreads = new ArrayList<>();
            downloadThreads.add(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return downloadFromAzure(filePathName);
                }
            });

            downloadThreads.add(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return downloadFromAzure(digestPathName);
                }
            });

            threadPool.invokeAll(downloadThreads, MAX_WAIT_TIME_IN_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ex) {
            result = false;
            log.warning(String.format("Download fileName %s fail with exception %s", filePathName, ex.getMessage()));
        } finally {
            if (!result) {
                result = downloadAndCheckDigest(threadPool, filePathName, currentRetry + 1);
            }
        }

        return result;
    }

    private static boolean downloadAndCheckDigest(@NotNull ExecutorService threadPool, @NotNull Path filePathName) {
        return downloadAndCheckDigest(threadPool, filePathName, 0);
    }

    private static String getJxBrowserJarFileName() throws JxBrowserException {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        log.info("osName is : " + osName + ", osArch is: " + osArch);

        if (osName.contains("win")) {
            if (osArch.contains("64")) {
                return platformToJxBrowserJar.get("win64");
            } else {
                return platformToJxBrowserJar.get("win32");
            }
        } else if (osName.contains("mac")) {
            return platformToJxBrowserJar.get("mac");
        } else if (osName.contains("nix") || osName.contains("nux")) {
            if (osArch.contains("64")) {
                return platformToJxBrowserJar.get("linux64");
            } else {
                throw new JxBrowserException("JxBrowser does not provide jar file for Linux 32bit.");
            }
        } else {
            throw new JxBrowserException("Cannot recognize the operation system.");
        }
    }
}