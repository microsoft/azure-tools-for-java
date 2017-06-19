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

package com.microsoft.azuretools.utils;


import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.Constants;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CancellationException;

/**
 * Created by vlashch on 1/19/17.
 */
public class WebAppUtils {

    private static final String ftpRootPath = "/site/wwwroot/";
    private static final String ftpWebAppsPath = ftpRootPath + "webapps/";
    private static String jdkFolderName = "jdk";
    private static final String ftpJdkPath = ftpRootPath + jdkFolderName;
    private static String aspScriptName = "getjdk.aspx";
    private static String webConfigFilename = "web.config";
    private static String reportFilename = "report.txt";
    private static String statusFilename = "status.txt";

    public static FTPClient getFtpConnection(PublishingProfile pp) throws IOException {

        FTPClient ftp = new FTPClient();

        System.out.println("\t\t" + pp.ftpUrl());
        System.out.println("\t\t" + pp.ftpUsername());
        System.out.println("\t\t" + pp.ftpPassword());

        URI uri = URI.create("ftp://" + pp.ftpUrl());
        ftp.connect(uri.getHost(), 21);
        final int replyCode = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(replyCode)) {
            ftp.disconnect();
            throw new ConnectException("Unable to connect to FTP server");
        }

        if (!ftp.login(pp.ftpUsername(), pp.ftpPassword())) {
            throw new ConnectException("Unable to login to FTP server");
        }

        ftp.setControlKeepAliveTimeout(Constants.connection_read_timeout_ms);
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        ftp.enterLocalPassiveMode();//Switch to passive mode

        return ftp;
    }

    public static void deployArtifact(String artifactName, String artifactPath, PublishingProfile pp, boolean toRoot, IProgressIndicator indicator) throws IOException {
        FTPClient ftp = null;
        InputStream input = null;
        try {
            if (indicator != null) indicator.setText("Connecting to FTP server...");

            ftp = getFtpConnection(pp);

            if (indicator != null) indicator.setText("Uploading the application...");
            input = new FileInputStream(artifactPath);
            if (toRoot) {
                WebAppUtils.removeFtpDirectory(ftp, ftpWebAppsPath + "ROOT", indicator);
                ftp.deleteFile(ftpWebAppsPath + "ROOT.war");
                ftp.storeFile(ftpWebAppsPath + "ROOT.war", input);
            } else {
                WebAppUtils.removeFtpDirectory(ftp, ftpWebAppsPath + artifactName, indicator);
                ftp.deleteFile(artifactName + ".war");
                boolean success = ftp.storeFile(ftpWebAppsPath + artifactName + ".war", input);
                if (!success) {
                    int rc = ftp.getReplyCode();
                    throw new IOException("FTP client can't store the artifact, reply code: " + rc);
                }
            }
            if (indicator != null) indicator.setText("Logging out of FTP server...");
            ftp.logout();
        } finally {
            if (input != null)
                input.close();
            if (ftp != null && ftp.isConnected()) {
                ftp.disconnect();
            }
        }
    }

    public static void removeFtpDirectory(FTPClient ftpClient, String path, IProgressIndicator pi) throws IOException {
        String prefix = "Removing from FTP server: ";
        FTPFile[] subFiles = ftpClient.listFiles(path);
        if (subFiles.length > 0) {
            for (FTPFile ftpFile : subFiles) {
                if (pi != null && pi.isCanceled()) break;
                String currentFileName = ftpFile.getName();
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    continue; // skip
                }

                String path1 = path + "/" + currentFileName;
                if (ftpFile.isDirectory()) {
                    // remove the sub directory
                    removeFtpDirectory(ftpClient, path1, pi);
                } else {
                    // delete the file
                    if (pi != null) pi.setText2(prefix + path1);
                    ftpClient.deleteFile(path1);
                }
            }
        }

        if (pi != null) pi.setText2(prefix + path);
        ftpClient.removeDirectory(path);
        if (pi != null) pi.setText2("");
    }

    private static void uploadJdkDownloadScript(FTPClient ftp, String jdkDownloadUrl) throws IOException {

        String aspxPageName = aspScriptName;

        byte[] aspxPageData = generateAspxScriptForCustomJdkDownload(jdkDownloadUrl);
        ftp.storeFile(ftpRootPath + aspxPageName, new ByteArrayInputStream(aspxPageData));

        byte[] webConfigData = generateWebConfigForCustomJdkDownload(aspxPageName, null);
        ftp.storeFile(ftpRootPath + webConfigFilename, new ByteArrayInputStream(webConfigData));
    }

    public static boolean doesRemoteFileExist(FTPClient ftp, String path, String fileName) throws IOException {
        FTPFile[] files = ftp.listFiles(path);
        for (FTPFile file : files) {
            if (file.isFile() && file.getName().equalsIgnoreCase(fileName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean doesRemoteFolderExist(FTPClient ftp, String path, String folderName) throws IOException {
        FTPFile[] files = ftp.listFiles(path);
        for (FTPFile file : files) {
            if (file.isDirectory() && file.getName().equalsIgnoreCase(folderName)) {
                return true;
            }
        }
        return false;
    }

    public static int sendGet(String sitePath) throws IOException {
        URL url = new URL(sitePath);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setReadTimeout(Constants.connection_read_timeout_ms);
        return con.getResponseCode();
        //con.setRequestProperty("User-Agent", "AzureTools for Intellij");
    }

    public static boolean isUrlAccessible(String url) throws IOException {
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("HEAD");
        con.setReadTimeout(Constants.connection_read_timeout_ms);
        try {
            if (con.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

    private static void cleanupWorkerData(FTPClient ftp) throws IOException {
        ftp.deleteFile(ftpRootPath + aspScriptName);
        ftp.deleteFile(ftpRootPath + "jdk.zip");
    }

    public static void removeCustomJdkArtifacts(FTPClient ftp, IProgressIndicator pi) throws IOException {
        if (doesRemoteFolderExist(ftp, ftpRootPath, jdkFolderName)) {
            removeFtpDirectory(ftp, ftpJdkPath, pi);
        }
        ftp.deleteFile(ftpRootPath + webConfigFilename);
        ftp.deleteFile(ftpRootPath + reportFilename);
        ftp.deleteFile(ftpRootPath + statusFilename);
    }

    public static class WebAppException extends Exception {
        /**
         * 
         */
        private static final long serialVersionUID = 1352713295336034845L;

        WebAppException(String message) {
            super(message);
        }
    }

    public static void deployCustomJdk(WebApp webApp, String jdkDownloadUrl, WebContainer webContainer, IProgressIndicator indicator) throws IOException, InterruptedException, WebAppException {
        FTPClient ftp = null;
        String customJdkFolderName =  null;
        try {

            PublishingProfile pp = webApp.getPublishingProfile();
            ftp = getFtpConnection(pp);

            // stop and restart web app
//            if (indicator != null) indicator.setText("Stopping the service...");
//            webApp.stop();

            if (indicator != null) indicator.setText("Deleting custom jdk artifacts, if any (takes a while)...");
            removeCustomJdkArtifacts(ftp, indicator);

            if (indicator != null) indicator.setText("Uploading scripts...");
            uploadJdkDownloadScript(ftp, jdkDownloadUrl);

//            if (indicator != null) indicator.setText("Starting the service...");
//            webApp.start();

            final String siteUrl = "https://" + webApp.defaultHostName();

            // send get to activate the script
            sendGet(siteUrl);

            // Polling report.txt...
            if (indicator != null) indicator.setText("Checking the JDK gets downloaded and unpacked...");
            //int step = 0;
            while (!doesRemoteFileExist(ftp, ftpRootPath, reportFilename)) {
                if (indicator != null && indicator.isCanceled()) throw new CancellationException("Canceled by user.");
                //if (step++ > 3) checkFreeSpaceAvailability(ftp);
                Thread.sleep(5000);
                sendGet(siteUrl);
            }

            if (indicator != null) indicator.setText("Checking status...");
            OutputStream reportFileStream = new ByteArrayOutputStream();
            ftp.retrieveFile("report.txt", reportFileStream);
            String reportFileString = reportFileStream.toString();
            if (reportFileString.startsWith("FAIL")) {
                String err = reportFileString.substring(reportFileString.indexOf(":"+1));
                throw new WebAppException(err);
            }

            // get top level jdk folder name (under jdk folder)
            FTPFile[] ftpDirs = ftp.listDirectories(ftpJdkPath);
            if (ftpDirs.length != 1) {
                String err = "Bad JDK archive. Please make sure the JDK archive contains a single JDK folder. For example, 'my-jdk1.7.0_79.zip' archive should contain 'jdk1.7.0_79' folder only";
                throw new WebAppException(err);
            }

            customJdkFolderName = ftpDirs[0].getName();

            uploadWebConfigForCustomJdk(ftp, webApp, customJdkFolderName, webContainer, indicator);
        } catch (IOException | WebAppException | InterruptedException ex){
            if (doesRemoteFolderExist(ftp, ftpRootPath, jdkFolderName)) {
                indicator.setText("Error happened. Cleaning up...");
                removeFtpDirectory(ftp, ftpJdkPath, indicator);
            }
            throw ex;
        } finally {
            indicator.setText("Removing working data from server...");
            cleanupWorkerData(ftp);
            if (ftp != null && ftp.isConnected()) {
                ftp.disconnect();
            }
        }
    }

    private static void uploadWebConfigForCustomJdk(FTPClient ftp, WebApp webApp, String jdkFolderName, WebContainer webContainer, IProgressIndicator indicator) throws IOException {
        if  (jdkFolderName == null || jdkFolderName.isEmpty()) {
            throw new IllegalArgumentException("jdkFolderName is null or empty");
        }

        if(indicator != null) indicator.setText("Stopping the service...");
        webApp.stop();

        if(indicator != null) indicator.setText("Deleting "+ webConfigFilename + "...");
        ftp.deleteFile(ftpRootPath + webConfigFilename);

        if (indicator != null) indicator.setText("Turning the App Service into java based...");
        webApp.update().withJavaVersion(JavaVersion.JAVA_8_NEWEST).withWebContainer(webContainer).apply();

        if(indicator != null) indicator.setText("Generating " + webConfigFilename + "...");
        String jdkPath = "%HOME%\\site\\wwwroot\\jdk\\" + jdkFolderName;
        String webContainerPath = generateWebContainerPath(webContainer);
        byte[] webConfigData = generateWebConfigForCustomJDK(jdkPath, webContainerPath);

        if(indicator != null) indicator.setText("Uploading " + webConfigFilename + "...");
        ftp.storeFile(ftpRootPath + webConfigFilename,  new ByteArrayInputStream(webConfigData));

        if(indicator != null) indicator.setText("Starting the service...");
        webApp.start();
    }

    public enum WebContainerMod {
        Newest_Tomcat_70("Newest Tomcat 7.0", "tomcat 7.0"),
        Newest_Tomcat_80("Newest Tomcat 8.0", "tomcat 8.0"),
        Newest_Tomcat_85("Newest Tomcat 8.5", "tomcat 8.5"),
        Newest_Jetty_91("Newest Jetty 9.1", "jetty 9.1"),
        Newest_Jetty_93("Newest Jetty 9.3", "jetty 9.3");

        private String displayName;
        private String value;

        WebContainerMod(String displayName, String value ) {
            this.displayName = displayName;
            this.value = value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getValue() {
            return value;
        }

        public WebContainer toWebContainer() {
            return new WebContainer();
        }

        @Override
        public String toString() {
            return getDisplayName();
        }

    }

    public static String generateWebContainerPath(WebContainer webContainer) throws IOException {
        if (webContainer.toString().equals(WebContainerMod.Newest_Tomcat_70.getValue())) {
            return "%AZURE_TOMCAT7_HOME%";
        } else if (webContainer.toString().equals(WebContainerMod.Newest_Tomcat_80.getValue())) {
            return "%AZURE_TOMCAT8_HOME%";
        } else if (webContainer.toString().equals(WebContainerMod.Newest_Tomcat_85.getValue())) {
            return "%AZURE_TOMCAT85_HOME%";
        } else if (webContainer.toString().equals(WebContainerMod.Newest_Jetty_91.getValue())) {
            return "%AZURE_JETTY9_HOME%";
        } else if (webContainer.toString().equals(WebContainerMod.Newest_Jetty_93.getValue())) {
            return "%AZURE_JETTY93_HOME%";
        }
//        if (webContainer.equals(WebContainer.TOMCAT_7_0_NEWEST)) {
//            return "%AZURE_TOMCAT7_HOME%";
//        } else if (webContainer.equals(WebContainer.TOMCAT_8_0_NEWEST)) {
//            return "%AZURE_TOMCAT8_HOME%";
//        } else if (webContainer.equals(WebContainer.JETTY_9_1_NEWEST)) {
//            return "%AZURE_JETTY9_HOME%";
//        }
//        String binPath = "%programfiles(x86)%\\";
//        String wc = webContainer.toString();
//        int verIdx = wc.indexOf(" ") + 1;
//        String ver = wc.substring(verIdx);
//        if (wc.startsWith("tomcat")) {
//            return binPath + "apache-tomcat-" + ver;
//        } else if (wc.startsWith("jetty")) {
//            StringBuilder sbVer = new StringBuilder(ver);
//            sbVer.insert(ver.lastIndexOf('.')+1, 'v');
//            return binPath + "jetty-distribution-" + sbVer.toString();
//        }

        throw new IOException("Unknown web container: " + webContainer.toString());
    }

    public static byte[] generateWebConfigForCustomJDK(String jdkPath, String webContainerPath) {
        String javaPath = jdkPath.isEmpty() ? "%JAVA_HOME%\\bin\\java.exe" : jdkPath + "\\bin\\java.exe";
        String debugOptions = "-Djava.net.preferIPv4Stack=true -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=127.0.0.1:%HTTP_PLATFORM_DEBUG_PORT% ";

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version='1.0' encoding='UTF-8'?>\n");
        sb.append("<configuration>\n");
        sb.append("    <system.webServer>\n");

        if (!webContainerPath.toUpperCase().contains("JETTY")) {
            sb.append("        <httpPlatform>\n");
            sb.append("            <environmentVariables>\n");
            sb.append("                <environmentVariable name='JRE_HOME' value='"+ jdkPath +"'/>\n");
            sb.append("                <environmentVariable name='JAVA_OPTS' value='"+ debugOptions +"'/>\n");
            sb.append("            </environmentVariables>\n");
            sb.append("        </httpPlatform>\n");
        } else {
            String arg = debugOptions + "-Djetty.port=%HTTP_PLATFORM_PORT% -Djetty.base=\"" +
                    webContainerPath + "\" -Djetty.webapps=\"d:\\home\\site\\wwwroot\\webapps\"  -jar \"" + webContainerPath + "\\start.jar\" etc\\jetty-logging.xml";

            sb.append("        <httpPlatform processPath='"+ javaPath +"' startupTimeLimit='30' startupRetryCount='10' arguments='"+ arg +"'/>\n");
        }
        sb.append("    </system.webServer>\n");
        sb.append("</configuration>\n");

        return sb.toString().getBytes();
    }

    public static byte[] generateAspxScriptForCustomJdkDownload(String jdkDownloadUrl) throws IOException {

        StringBuilder sb = new StringBuilder();

        sb.append("<%@ Page Language=\"C#\" %>\n");
        sb.append("<%@ Import namespace=\"System.IO\" %>\n");
        sb.append("<%@ Import namespace=\"System.Net\" %>\n");
        sb.append("<%@ Import namespace=\"System.IO.Compression\" %>\n");
        sb.append("<script runat=server>\n");

        sb.append("const string baseDir = @\"d:\\home\\site\\wwwroot\";\n");
        sb.append("const string keySuccess = \"SUCCESS\";\n");
        sb.append("const string keyFail = \"FAIL\";\n");
        sb.append("const string reportPattern = \"{0}:{1}\";\n");
        sb.append("readonly static string pathReport = Path.Combine(baseDir, \"report.txt\");\n");
        sb.append("readonly static string pathStatus = Path.Combine(baseDir, \"status.txt\");\n");

        sb.append("string getTime() {\n");
        sb.append("    getJdk();\n");
        sb.append("    return DateTime.Now.ToString(\"t\");\n");
        sb.append("}\n");

        sb.append("static void getJdk() {\n");
        sb.append("    try {\n");
        sb.append("         const string downloadSrc = @\"" + jdkDownloadUrl + "\";\n");
        sb.append("         string downloadDst = Path.Combine(baseDir, \"jdk.zip\");\n");
        sb.append("         statusAdd(\"Deleting zip file, if any\");\n");
        sb.append("         if (File.Exists(downloadDst)) { File.Delete(downloadDst); }\n");
        sb.append("         statusAdd(\"Checking zip size for download\");\n");
        sb.append("         var req = WebRequest.Create(downloadSrc);\n");
        sb.append("         req.Method = \"HEAD\";\n");
        sb.append("         long contentLength;\n");
        sb.append("         using (WebResponse resp = req.GetResponse()) {\n");
        sb.append("             if (!long.TryParse(resp.Headers.Get(\"Content-Length\"), out contentLength)) {\n");
        sb.append("                 throw new Exception(\"Can't get file size\");\n");
        sb.append("             }\n");
        sb.append("         }\n");
        sb.append("         statusAdd(\"zip size is [\" + contentLength + \"] , disk size is [\" + getDiskFreeSpace() + \"]\" );\n");
        sb.append("         if (contentLength*2 > getDiskFreeSpace()) {\n");
        sb.append("             throw new Exception(\"There is not enough disk space to complete the operation.\");\n");
        sb.append("         }\n");
        sb.append("         statusAdd(\"Downloading zip\");\n");
        sb.append("         using (var client = new WebClient()) {\n");
        sb.append("             client.DownloadFile(downloadSrc, downloadDst);\n");
        sb.append("         }\n");
        sb.append("         string unpackDst = Path.Combine(baseDir, \"jdk\");\n");
        sb.append("         statusAdd(\"Deleting jdk dir, if any\");\n");
        sb.append("         if (Directory.Exists(unpackDst)) { Directory.Delete(unpackDst, true); }\n");
        sb.append("         string unpackSrc = Path.Combine(baseDir, \"jdk.zip\");\n");
        sb.append("         statusAdd(\"Checking expected upacked size\");\n");
        sb.append("         long expectedUnpackedSize;\n");
        sb.append("         using (ZipArchive archive = ZipFile.OpenRead(unpackSrc)) {\n");
        sb.append("             expectedUnpackedSize = archive.Entries.Sum(entry => entry.Length);\n");
        sb.append("         }\n");
        sb.append("         statusAdd(\"Expected upacked size is [\" + expectedUnpackedSize + \"] , disk size is [\" + getDiskFreeSpace() + \"]\");\n");
        sb.append("         if (expectedUnpackedSize*2 > getDiskFreeSpace()) {\n");
        sb.append("             throw new Exception(\"There is not enough disk space to complete the operation.\");\n");
        sb.append("         }\n");
        sb.append("         statusAdd(\"Unpacking zip\");\n");
        sb.append("         ZipFile.ExtractToDirectory(unpackSrc, unpackDst);\n");
        sb.append("         statusAdd(\"Done\");\n");
        sb.append("         reportOneLine(string.Format(reportPattern, keySuccess, string.Empty));\n");
        sb.append("     } catch (Exception e) {\n");
        sb.append("         statusAdd(\"Exception: \" + e.Message);\n");
        sb.append("         reportOneLine(string.Format(reportPattern, keyFail, e.Message));\n");
        sb.append("     }\n");
        sb.append("}\n");

        sb.append("static long getDiskFreeSpace() {\n");
        sb.append("     DriveInfo driveInfo = new DriveInfo(@\"d:\");\n");
        sb.append("     return driveInfo.AvailableFreeSpace;\n");
        sb.append("}\n");

        sb.append("static void reportOneLine(string message) {\n");
        sb.append("     if (File.Exists(pathReport)) File.Delete(pathReport);\n");
        sb.append("     using (StreamWriter sw = File.CreateText(pathReport)) {\n");
        sb.append("         sw.WriteLine(message);\n");
        sb.append("     }\n");
        sb.append("}\n");

        sb.append("static void statusAdd(string message) {\n");
        sb.append("     if (!File.Exists(pathStatus)) {\n");
        sb.append("         using (StreamWriter sw = File.CreateText(pathStatus)) {\n");
        sb.append("             sw.WriteLine(message);\n");
        sb.append("         }\n");
        sb.append("     } else {\n");
        sb.append("         using (StreamWriter sw = File.AppendText(pathStatus)) {\n");
        sb.append("             sw.WriteLine(message);\n");
        sb.append("         }\n");
        sb.append("     }\n");
        sb.append("}\n");

        sb.append("</script>\n");
        sb.append("<html>\n");
        sb.append("<body>\n");
        sb.append("<form id=\"form1\" runat=\"server\">\n");
        sb.append("Current server time is <% =getTime()%>\n");
        sb.append("</form>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString().getBytes();
    }

    public static byte[] generateWebConfigForCustomJdkDownload(String initializationPage, String[] assemblies) throws IOException {

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='no'?>\n");
        sb.append("<configuration>\n");
        sb.append("    <system.webServer>\n");
        sb.append("        <applicationInitialization remapManagedRequestsTo='/hostingstart.html'>\n");
        if(initializationPage!=null && !initializationPage.isEmpty())
            sb.append("        <add initializationPage='/" + initializationPage + "'/>\n");
        sb.append("    </applicationInitialization>\n");
        sb.append("    </system.webServer>\n");
        sb.append("    <system.web>\n");
        sb.append("        <customErrors mode='Off'/>\n");
        sb.append("        <compilation debug='true' targetFramework='4.5'>\n");
        sb.append("        <assemblies>\n");
        sb.append("            <add assembly='System.IO.Compression.FileSystem, Version=4.0.0.0, Culture=neutral, PublicKeyToken=B77A5C561934E089'/>\n");
        sb.append("            <add assembly='System.IO.Compression, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b77a5c561934e089'/>\n");

        if (assemblies != null) {
            for (String assembly : assemblies) {
                sb.append("            <add assembly='" + assembly + "'/>\n");
            }
        }

        sb.append("        </assemblies>\n");
        sb.append("        </compilation>\n");
        sb.append("        <httpRuntime targetFramework='4.5'/>\n");
        sb.append("    </system.web>\n");
        sb.append("</configuration>\n");
        return sb.toString().getBytes();
    }


    public static abstract class CreateAppServiceModel {
        public enum JdkTab {
            Default,
            ThirdParty,
            Own;
        }
        public String webAppName;
        public WebContainer webContainer;
        public SubscriptionDetail subscriptionDetail;

        public boolean isResourceGroupCreateNew;
        public ResourceGroup resourceGroup;
        public String resourceGroupNameCreateNew;

        public boolean isAppServicePlanCreateNew;
        public AppServicePlan appServicePlan;
        public String appServicePlanNameCreateNew;
        public Location appServicePlanLocationCreateNew;
        public PricingTier appServicePricingTierCreateNew;

        public String jdk3PartyUrl;
        public String jdkOwnUrl;
        public String storageAccountKey;
        public JdkTab jdkTab;
        public String jdkDownloadUrl;

        public abstract void collectData();
    }

    public static WebApp createAppService(IProgressIndicator progressIndicator, CreateAppServiceModel model) throws IOException, WebAppException, InterruptedException, AzureCmdException {

        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) { return null; }

        Azure azure = azureManager.getAzure(model.subscriptionDetail.getSubscriptionId());

        AppServicePlan appServicePlan = null;
        if (model.isAppServicePlanCreateNew) {
            AppServicePlan.DefinitionStages.WithGroup ds1 = azure.appServices().appServicePlans()
                    .define(model.appServicePlanNameCreateNew)
                    .withRegion(model.appServicePlanLocationCreateNew.name());
            AppServicePlan.DefinitionStages.WithPricingTier ds2;
            if (model.isResourceGroupCreateNew) {
                ds2 = ds1.withNewResourceGroup(model.resourceGroupNameCreateNew);
            } else {
                ds2 = ds1.withExistingResourceGroup(model.resourceGroup);
            }
            appServicePlan = ds2.withPricingTier(model.appServicePricingTierCreateNew).withOperatingSystem(OperatingSystem.WINDOWS).create();
        } else {
            appServicePlan = model.appServicePlan;
        }

        WebApp.DefinitionStages.Blank definitionStages = azure.webApps().define(model.webAppName);
        WebAppBase.DefinitionStages.WithCreate<WebApp> withCreate;

        WebApp.DefinitionStages.ExistingWindowsPlanWithGroup ds1 = definitionStages.withExistingWindowsPlan(appServicePlan);
        if (model.isResourceGroupCreateNew) {
            withCreate = ds1.withNewResourceGroup(model.resourceGroupNameCreateNew);
        } else {
            withCreate = ds1.withExistingResourceGroup(model.resourceGroup);
        }

        if (model.jdkDownloadUrl == null) { // no custom jdk
            withCreate = withCreate.withJavaVersion(JavaVersion.JAVA_8_NEWEST).withWebContainer(model.webContainer);
        }

        WebApp myWebApp = withCreate.create();

        if (model.jdkDownloadUrl != null ) {
            progressIndicator.setText("Deploying custom jdk...");
            WebAppUtils.deployCustomJdk(myWebApp, model.jdkDownloadUrl, model.webContainer, progressIndicator);
        }

        // update cache
        if (model.isResourceGroupCreateNew) {
            ResourceGroup rg = azure.resourceGroups().getByName(model.resourceGroupNameCreateNew);
            if (rg == null) {
                throw new AzureCmdException(String.format("azure.resourceGroups().getByName(%s) returned null"), model.resourceGroupNameCreateNew);
            }

            AzureModelController.addNewResourceGroup(model.subscriptionDetail, rg);
            AzureModelController.addNewWebAppToJustCreatedResourceGroup(rg, myWebApp);
            if (model.isAppServicePlanCreateNew) {
                AzureModelController.addNewAppServicePlanToJustCreatedResourceGroup(rg, appServicePlan);
            } else {
                // add empty list
                AzureModelController.addNewAppServicePlanToJustCreatedResourceGroup(rg, null);
            }
        } else {
            ResourceGroup rg = model.resourceGroup;
            AzureModelController.addNewWebAppToExistingResourceGroup(rg, myWebApp);
            if (model.isAppServicePlanCreateNew) {
                //AppServicePlan asp = azure.appServices().appServicePlans().getById(myWebApp.appServicePlanId());
                AzureModelController.addNewAppServicePlanToExistingResourceGroup(rg, appServicePlan);
            }
        }

        return myWebApp;
    }

    public static void deleteAppService(WebAppDetails webAppDetails) throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        Azure azure = azureManager.getAzure(webAppDetails.subscriptionDetail.getSubscriptionId());
        azure.webApps().deleteById(webAppDetails.webApp.id());
        // check asp still exists
        AppServicePlan asp = azure.appServices().appServicePlans().getById(webAppDetails.appServicePlan.id());
        System.out.println("asp is " + (asp == null ? "null -> removing form cache" : asp.name()));
        // update cache
        AzureModelController.removeWebAppFromResourceGroup(webAppDetails.resourceGroup, webAppDetails.webApp);
        if (asp == null) {
            AzureModelController.removeAppServicePlanFromResourceGroup(webAppDetails.appServicePlanResourceGroup, webAppDetails.appServicePlan);
        }
    }

    public static void uploadWebConfig(WebApp webApp, InputStream fileStream, IProgressIndicator indicator) throws IOException {
        FTPClient ftp = null;
        try {
            PublishingProfile pp = webApp.getPublishingProfile();
            ftp = getFtpConnection(pp);

            if(indicator != null) indicator.setText("Stopping the service...");
            webApp.stop();

            if(indicator != null) indicator.setText("Uploading " + webConfigFilename + "...");
            ftp.storeFile(ftpRootPath + webConfigFilename, fileStream);

            if(indicator != null) indicator.setText("Starting the service...");
            webApp.start();
        } finally {
            if (ftp != null && ftp.isConnected()) {
                ftp.disconnect();
            }
        }
    }

    public static class WebAppDetails {
        public SubscriptionDetail subscriptionDetail;
        public ResourceGroup resourceGroup;
        public AppServicePlan appServicePlan;
        public ResourceGroup appServicePlanResourceGroup;
        public WebApp webApp;

        public WebAppDetails() {}

        public WebAppDetails(ResourceGroup resourceGroup, WebApp webApp,
                             AppServicePlan appServicePlan, ResourceGroup appServicePlanResourceGroup,
                             SubscriptionDetail subscriptionDetail) {
            this.resourceGroup = resourceGroup;
            this.webApp = webApp;
            this.appServicePlan = appServicePlan;
            this.appServicePlanResourceGroup = appServicePlanResourceGroup;
            this.subscriptionDetail = subscriptionDetail;
        }
    }

    public static class AspDetails {
        private AppServicePlan asp;
        private ResourceGroup rg;
        public AspDetails(AppServicePlan asp, ResourceGroup rg) {
            this.asp = asp;
            this.rg = rg;
        }
        public AppServicePlan getAsp() {
            return asp;
        }
        public ResourceGroup getRg() {
            return rg;
        }
    }
}
