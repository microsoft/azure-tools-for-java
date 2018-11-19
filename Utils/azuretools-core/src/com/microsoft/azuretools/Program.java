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

package com.microsoft.azuretools;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.ResourceGroups;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.adauth.IDeviceLoginUI;
import com.microsoft.azuretools.adauth.IWebUi;
import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.interact.AuthMethod;
import com.microsoft.azuretools.authmanage.interact.INotification;
import com.microsoft.azuretools.authmanage.interact.IUIFactory;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.authmanage.srvpri.SrvPriManager;
import com.microsoft.azuretools.authmanage.srvpri.report.FileListener;
import com.microsoft.azuretools.authmanage.srvpri.report.IListener;
import com.microsoft.azuretools.authmanage.srvpri.report.Reporter;
import com.microsoft.azuretools.authmanage.srvpri.rest.GraphRestHelper;
import com.microsoft.azuretools.authmanage.srvpri.step.Status;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.IProgressTaskImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;

public class Program {

    final static String sid = "9657ab5d-4a4a-4fd2-ae7a-4cd9fbd030ef";
    static Set<String> sidList = null;

    @SuppressWarnings("unused")
	public static void main(String[] args) {

        LogManager.getLogManager().reset();

        try {

            try {
                if (CommonSettings.getUiFactory() == null)
                    CommonSettings.setUiFactory(new UIFactory());
                String wd = "AuthManageWorkingDir";
                Path dirPath = Paths.get(System.getProperty("user.home"), wd);
                if (!Files.exists(dirPath)) {
                    Files.createDirectory(dirPath);
                }
                CommonSettings.setUpEnvironment(dirPath.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }



            System.out.println("--- Service Principals section ---");
            // create with sp
            //final File credFile = new File(System.getProperty("user.home") + "/ServicePrincipals/srv-pri.azureauth-778");
            String path = "C:\\Users\\shch\\_ServicePrincipalsTest\\sp5\\sp-9657ab5d-4a4a-4fd2-ae7a-4cd9fbd030ef_20161202161437.azureauth";
            final File credFile = new File("C:\\Users\\shch\\_ServicePrincipalsTest\\sp5\\sp-9657ab5d-4a4a-4fd2-ae7a-4cd9fbd030ef_20161202161437.azureauth");

            AuthMethodDetails amd = new AuthMethodDetails();
            amd.setAuthMethod(AuthMethod.SP);
            amd.setCredFilePath(path);

            AuthMethodManager amm = AuthMethodManager.getInstance();
            amm.setAuthMethodDetails(amd);

            AzureManager am = amm.getAzureManager();
            SubscriptionManager subscriptionManager = am.getSubscriptionManager();
            List<SubscriptionDetail> sdl = subscriptionManager.getSubscriptionDetails();





            //AzureManager spManager = new ServicePrincipalAzureManager(credFile);
            //printResourceGroups(spManager);
            //printSubscriptions(spManager);



            // create with token
            //AzureManager atManager = new AccessTokenAzureManager(Constants.tenant);
            //AzureManager atManager = new AccessTokenAzureManager();


            //SubscriptionManager.getInstance().showSubscriptionDialog();

//            AdAuthManager adAuthManager = AdAuthManager.getInstance();

            System.out.println("=== Access token section ---");

//            AuthenticationResult res = AdAuthManager.getInstance().signIn();
//            UserInfo ui = res.getUserInfo();

            // Setup working dir
            String wd = "AuthManageWorkingDir";
            Path dirPath = Paths.get(System.getProperty("user.home"), wd);
            if (!Files.exists(dirPath)) {
                Files.createDirectory(dirPath);
            }
            CommonSettings.setUpEnvironment(dirPath.toString());

            // Setup uiFactory
            if (CommonSettings.getUiFactory() == null)
                CommonSettings.setUiFactory(new UIFactory());



            //testReporter(dirPath.toString());
            //graphApiAction();
            //createSp();
//            AzureManager am = AuthMethodManager.getInstance().getAzureManager(AuthMethod.AD);
//            if (am == null) {
//                return;
//            }
//
//            System.out.println("-- Subscription list ---");
//            printSubscriptions(am);

//           sidList = AdAuthManager.getInstance().getAccountSidList();
//            for (String sid : AdAuthManager.getInstance().getAccountSidList()) {
//                System.out.println("  - " + sid);
//            }

            //printResourceGroups(am);
            //printTenants(atManager);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.exit(1);
        }
    }

    @SuppressWarnings("unused")
	private static void createStorageAccount() {

    }

    @SuppressWarnings("unused")
	private static void testReporter(String wd) throws Exception {
        Reporter<String> reporter = new Reporter<String>();
        reporter.addListener(new FileListener("testReport.txt", wd));
        reporter.addConsoleLister();

        for (int i=1; i<100;++i) {
            reporter.report("Hello SHch! " + i);
        }
    }

    @SuppressWarnings("unused")
	private static void createSp() {
        try {
            LogManager.getLogManager().reset();
            boolean doFirst = false;

            AdAuthManager adAuthManager = AdAuthManager.getInstance();
            if (doFirst) {
                if (adAuthManager.isSignedIn()) {
                    adAuthManager.signOut();
                    //AuthMethodManager.getInstance().clean(AuthMethod.AD);
                }
                adAuthManager.signIn();
            } else {
                if (!adAuthManager.isSignedIn())
                    adAuthManager.signIn();
            }

            Map<String, List<String>> tsm = adAuthManager.getAccountTenantsAndSubscriptions();
            for (String tid : tsm.keySet()) {
                List<String> sids = tsm.get(tid);

                if (!sids.isEmpty()) {
                    try {
                        SrvPriManager.createSp(tid, sids, "808", new IListener<Status>() {
                            @Override
                            public void listen(Status message) {
                                System.out.format(">> Status report: %s\t:\t%s\t:\t%s\n", message.getAction(), message.getResult(),message.getDetails());
                            }
                        }, null);
                    } catch (Exception t1) {
                        System.out.println("CreateServicePrincipalsAction ex: " + t1.getMessage());
                        t1.printStackTrace();
                    }
                }
            }
        } catch (Exception t) {
            t.printStackTrace();
        }
    }


    @SuppressWarnings("unused")
	private static void graphApiAction() throws Exception {


        AdAuthManager adAuthManager = AdAuthManager.getInstance();
        if (!adAuthManager.isSignedIn()) {
            adAuthManager.signIn();
        }

        String request = "tenantDetails";
        List<com.microsoft.azure.management.resources.Tenant> tl = AccessTokenAzureManager.getTenants("common");
        for (com.microsoft.azure.management.resources.Tenant t : tl) {
            String tid = t.tenantId();
            System.out.println("==> tenant ID: " + tid);
            String tenantId = tid;
            try {
                GraphRestHelper grh = new GraphRestHelper(tenantId);
                String resp = grh.doGet(request, null);
                System.out.println("Graph response:" + resp);

            } catch (Exception ex) {
                System.out.println("GraphApiAction ex: " + ex.getMessage());
            }
        }
    }

    @SuppressWarnings("unused")
	private static void printResourceGroups(AzureManager manager) throws Exception {

        Set<String> sidList = manager.getSubscriptionManager().getAccountSidList();
        for (String sid: sidList) {
            Azure azure = manager.getAzure(sid);
            System.out.println("==> Resource groups / " + sid);
            ResourceGroups rgs = azure.resourceGroups();
            for (ResourceGroup rg : rgs.list()) {
                System.out.println("    " + rg.name());
            }
        }
    }

    @SuppressWarnings("unused")
	private static void printSubscriptions(AzureManager manager) throws Exception {

        for ( Subscription s : manager.getSubscriptions()) {
            System.out.println("    " + s.displayName());
        }
    }

    static class UIFactory implements IUIFactory {
//        @Override
//        public ISelectAuthMethod getAuthMethodDialog() {
//            return new ISelectAuthMethod() {
//                AuthMethodDetails details;
//                @Override
//                public void init(AuthMethodDetails details) {
//                    this.details = details;
//                }
//
//                @Override
//                public AuthMethodDetails update() {
//                    return this.details;
//                }
//            };
//        }

        @Override
        public INotification getNotificationWindow() {
            return new INotification() {
                @Override
                public void deliver(String subject, String message) {
                    System.out.println(subject + ": " + message);
                }
            };
        }

        @Override
        public IWebUi getWebUi() {
            return null;
        }

        @Override
        public IDeviceLoginUI getDeviceLoginUI() {
            return null;
        }

        @Override
        public IProgressTaskImpl getProgressTaskImpl() {
            return null;
        }
    }

//    private static void printTenants(AzureManager manager) throws Exception {
//
//        for ( Tenant t : manager.getTenants()) {
//            String tid = t.tenantId();
//            System.out.println("    " + tid);
//
//        }
//    }
}

