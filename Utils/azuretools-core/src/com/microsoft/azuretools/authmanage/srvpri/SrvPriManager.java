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

package com.microsoft.azuretools.authmanage.srvpri;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.srvpri.entities.AuthenticationError;
import com.microsoft.azuretools.authmanage.srvpri.report.FileListener;
import com.microsoft.azuretools.authmanage.srvpri.report.IListener;
import com.microsoft.azuretools.authmanage.srvpri.report.Reporter;
import com.microsoft.azuretools.authmanage.srvpri.step.ApplicationStep;
import com.microsoft.azuretools.authmanage.srvpri.step.CommonParams;
import com.microsoft.azuretools.authmanage.srvpri.step.RoleAssignmentStep;
import com.microsoft.azuretools.authmanage.srvpri.step.ServicePrincipalStep;
import com.microsoft.azuretools.authmanage.srvpri.step.Status;
import com.microsoft.azuretools.authmanage.srvpri.step.StepManager;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;

/**
 * Created by vlashch on 8/16/16.
 */


public class SrvPriManager {
    // relationship;
    // app - sp (1-1)
    // sp - role (1-many)

    private final static Logger LOGGER = Logger.getLogger(SrvPriManager.class.getName());
    public static String createSp(AccessTokenAzureManager preAccessTokenAzureManager,
                                  String tenantId,
                                  List<String> subscriptionIds,
                                  String suffix,
                                  IListener<Status> statusListener,
                                  String destinationFolder)
            throws IOException {

        System.out.print(tenantId +": [");
        for (String sid : subscriptionIds) {
            System.out.print(sid + ", ");
        }
        System.out.println("]");

        Path spDirPath = Paths.get(destinationFolder);
        if (spDirPath == null) {
            String baseDir = System.getProperty("user.home");
            String dirName = "MSAzureAuhtFiles";
            spDirPath = Paths.get(baseDir, dirName);
            if (!Files.exists(spDirPath)) {
                Files.createDirectory(spDirPath);
            }
        }

        Reporter<Status> statusReporter = new Reporter<Status>();
        statusReporter.addListener(statusListener);
        CommonParams.setStatusReporter(statusReporter);

        // generate a password
        String password = "AZURE" + UUID.randomUUID().toString();

        CommonParams.setSubscriptionIdList(subscriptionIds);
        CommonParams.setResultSubscriptionIdList(new LinkedList<>());
        CommonParams.setTenantId(tenantId);
        String filename = tenantId + "_" + suffix;
        //String filename = tenantId + "_" + subscriptionIds.get(0) + "_" + suffix;
        //String filename = subscriptionIds.get(0) + "_" + suffix;
        String spFilename = "sp-" + filename + ".azureauth";
        String reportFilename = "report-" + filename + ".txt";

        Reporter<String> fileReporter = new Reporter<String>();
        fileReporter.addListener(new FileListener(reportFilename, spDirPath.toString()));
        fileReporter.addConsoleLister();
        CommonParams.setReporter(fileReporter);

        StepManager sm = new StepManager();
        sm.getParamMap().put("displayName", "AzureTools4j-" + suffix);
        sm.getParamMap().put("homePage", "https://github.com/Microsoft/azure-tools-for-java");
        // MUST be unique
        sm.getParamMap().put("identifierUri", "file://" + spFilename);
        sm.getParamMap().put("password", password);
        sm.getParamMap().put("status", "standby");

        sm.add(new ApplicationStep(preAccessTokenAzureManager));
        sm.add(new ServicePrincipalStep(preAccessTokenAzureManager));
        sm.add(new RoleAssignmentStep(preAccessTokenAzureManager));

        fileReporter.report(String.format("== Starting for tenantId: '%s'", tenantId));

        sm.execute();

        String overallStatusText = "=== Overall status";
        // create a file artifact
        if(sm.getParamMap().get("status").toString().equals("done")) {
            Path filePath = Paths.get(spDirPath.toString(), spFilename);;

            createArtifact(
                    filePath.toString(),
                    (UUID) sm.getParamMap().get("appId"),
                    password
            );

            statusReporter.report(new Status("Waiting for service principal activation to complete...", null, null));
            final int SLEEP_SEC_TO_PROPAGATE = 40;
            try {
                Thread.sleep(SLEEP_SEC_TO_PROPAGATE * 1000);
            } catch (InterruptedException e) {
                fileReporter.report("Interrupted sleep: " + e.getMessage());
            }

            statusReporter.report(new Status("Checking auth file...", null, null));
            checkArtifact(fileReporter, filePath);

            String successSidsResult = String.format("Succeeded for %d of %d subscriptions. ",
                    CommonParams.getResultSubscriptionIdList().size(),
                    CommonParams.getSubscriptionIdList().size());

            statusReporter.report(new Status(
                    overallStatusText,
                    Status.Result.SUCCESSFUL,
                    successSidsResult
            ));

            fileReporter.report(String.format("Authentication file created, path: %s", filePath.toString()));
            return filePath.toString();

        } else {
            statusReporter.report(new Status(
                    overallStatusText,
                    Status.Result.FAILED,
                    "Can't create a service principal."
            ));
        }

        return null;
    }

    private static void createArtifact(String filepath, UUID appId, String appPassword) throws IOException {

        Properties prop = new Properties();
        Writer writer = null;

        try {
            // to ignore date comment
            String lineSeparator = System.getProperty("line.separator");
            writer = new BufferedWriter(new FileWriter(filepath)) {
                private boolean skipLineSeparator = false;
                @Override
                public void write(String str) throws IOException {
                    System.out.println(str);
                    if (str.startsWith("#")) {
                        skipLineSeparator = true;
                        return;
                    }
                    if (str.startsWith(lineSeparator) && skipLineSeparator) {
                        skipLineSeparator = false;
                        return;
                    }
                    super.write(str);
                }
            };

            // set the properties value
            prop.setProperty("tenant", CommonParams.getTenantId());
            int i = 0;
            for (String subscriptionId : CommonParams.getResultSubscriptionIdList()) {
                if (i==0) {
                    prop.setProperty("subscription", subscriptionId);
                } else {
                    prop.setProperty("subscription"+i, subscriptionId);
                }
                i++;
            }
            prop.setProperty("client", appId.toString());
            prop.setProperty("key", appPassword);

            AzureEnvironment azureEnv = CommonSettings.getAdEnvironment();
            prop.setProperty("managementURI", azureEnv.managementEndpoint());
            prop.setProperty("baseURL", azureEnv.resourceManagerEndpoint());
            prop.setProperty("authURL", azureEnv.activeDirectoryEndpoint());
            prop.setProperty("graphURL", azureEnv.graphEndpoint());

            prop.store(writer, null);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static void checkArtifact(Reporter<String> fileReporter, Path filePath) throws IOException {
        // here we try to use the file to check it's ok with retry logic
        fileReporter.report("Checking cred file...");
        final int RETRY_QNTY = 5;
        final int SLEEP_SEC = 10;
        int retry_count = 0;
        File authFiel = new File(filePath.toString());
        while (retry_count < RETRY_QNTY) {
            try {
                fileReporter.report("Checking: Azure.authenticate(authFile)...");
                Azure.Authenticated azureAuthenticated = Azure.authenticate(authFiel);
                fileReporter.report("Checking: azureAuthenticated.subscriptions().list()...");
                azureAuthenticated.subscriptions().list();
                fileReporter.report("Checking: azureAuthenticated.withDefaultSubscription()...");
                Azure azure = azureAuthenticated.withDefaultSubscription();
                fileReporter.report("Checking: resourceGroups().list()...");
                azure.resourceGroups().list();
                fileReporter.report("Done.");
                break;
            } catch (Throwable e) {
                LOGGER.info("=== checkArtifact@SrvPriManager exception: " + e.getMessage());
                //e.printStackTrace();
                if (needToRetry(e)) {
                    retry_count++;
                    if ((retry_count >= RETRY_QNTY)) {
                        fileReporter.report(String.format("Failed to check cred file -retry limit %s has reached, error: %s", RETRY_QNTY, e.getMessage()));
                        throw e;
                    }
                    LOGGER.info(String.format("Failed %d/%d, will retry in %s seconds, error: %s", retry_count, RETRY_QNTY, SLEEP_SEC, e.getMessage()));
                    try {
                        Thread.sleep(SLEEP_SEC * 1000);
                    } catch (InterruptedException e1) {
                        fileReporter.report("Interrupted sleep: " + e.getMessage());
                    }
                } else {
                    fileReporter.report(String.format("Failed to check cred file after %s retries, error", retry_count, e.getMessage()));
                    throw e;
                }
            }
        }
    }

    private static boolean needToRetry(Throwable e) throws IOException {
        final String ERROR_LABEL = "\"error\":";
        final String ERROR_TEXT = "unauthorized_client";
        if (e instanceof com.microsoft.aad.adal4j.AuthenticationException) {
            LOGGER.info("=== needToRetry@SrvPriManager: AuthenticationException info: " + e.getMessage());
            ObjectMapper om = new ObjectMapper();
            AuthenticationError ae = om.readValue(e.getMessage(), AuthenticationError.class);
            if (ae.error.equals(ERROR_TEXT)) {
                return true;
            }
        } else {
            LOGGER.info("=== needToRetry@SrvPriManager: Exception info: " + e.getMessage());
            // if we can't catch the exception by type - the one we are catching may be on a deep level of cause.
            // we are looking for an error text;
            String mes = e.getMessage();
            int i1 = mes.indexOf(ERROR_LABEL);
            if (i1 >=0) {
                String error = mes.substring(i1 + ERROR_LABEL.length());
                if (error.contains(ERROR_TEXT)) {
                    return true;
                }
            }
        }

        return false;
    }

//    SrvPriData collectSrvPriData(UUID appId) {
//        return new SrvPriData();
//    }

// ======== Private helpers ===============================


//    private static void printSrvPri(String id) throws Throwable {
//        String resp = GraphRestHelper.get( "servicePrincipals/" + id, null);
//        if(resp != null) {
//            ObjectMapper mapper = new ObjectMapper();
//            ServicePrincipalRet sp = mapper.readValue(resp, ServicePrincipalRet.class);
//        }
//    }
}
