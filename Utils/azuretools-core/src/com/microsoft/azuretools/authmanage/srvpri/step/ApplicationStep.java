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
package com.microsoft.azuretools.authmanage.srvpri.step;

import com.microsoft.azuretools.authmanage.srvpri.entities.Application;
import com.microsoft.azuretools.authmanage.srvpri.entities.ApplicationGet;
import com.microsoft.azuretools.authmanage.srvpri.entities.ApplicationRet;
import com.microsoft.azuretools.authmanage.srvpri.entities.PasswordCredentials;
import com.microsoft.azuretools.authmanage.srvpri.report.Reporter;
import com.microsoft.azuretools.authmanage.srvpri.rest.GraphRestHelper;
import com.microsoft.azuretools.sdkmanage.AccessTokenAzureManager;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Created by shch on 8/21/2016.
 */

public class ApplicationStep implements IStep {
    private GraphRestHelper graphRestHelper;
    private Reporter<String> reporter;
    private final AccessTokenAzureManager preAccessTokenAzureManager;

    public ApplicationStep(AccessTokenAzureManager preAccessTokenAzureManager) {
        this.preAccessTokenAzureManager = preAccessTokenAzureManager;
    }

    @Override
    public void execute(Map<String, Object> params) throws IOException, InterruptedException {

        //System.out.println("ApplicationStep execute...");

        String displayName = params.get("displayName").toString();
        String homePage = params.get("homePage").toString();
        String identifierUri = params.get("identifierUri").toString();
        String password = params.get("password").toString();
        //UUID tenantId = (UUID) params.get("tenantId");
        String tenantId = CommonParams.getTenantId();
        graphRestHelper = new GraphRestHelper(preAccessTokenAzureManager, tenantId);

        reporter = CommonParams.getReporter();
        reporter.report("displayName: " + displayName);
        reporter.report("homePage: " + homePage);
        reporter.report("identifierUri: " + identifierUri);
        reporter.report("password: " + password);

        reporter.report("Creating ADD Application...");
        ApplicationRet app = createAadApplication(
                displayName,
                homePage,
                new String[]{identifierUri},
                password);
        reporter.report("Done.");
        reporter.report(String.format("Checking ADD Application availability..."));
        final int RETRY_QNTY = 5;
        final int SLEEP_SEC = 10;
        int retry_count = 0;
        while (retry_count < RETRY_QNTY) {
            ApplicationGet applicationGet = getAadApplication( app.appId);
            if (applicationGet.value.size() > 0) {
                reporter.report("Done.");
                break;
            }
            retry_count++;
            reporter.report(String.format("Not available. Will retry in %d sec...", SLEEP_SEC));
            Thread.sleep(SLEEP_SEC * 1000);
        }

        if ((retry_count >= RETRY_QNTY)) {
            String errorDetails = String.format("The AD Application (appId: %s) is not available after %s retries", app.appId, RETRY_QNTY);
            CommonParams.getStatusReporter().report(new Status(
                    getName(),
                    Status.Result.FAILED,
                    errorDetails
            ));
            throw new IOException(errorDetails);
        }

        params.put("appId", app.appId);
        params.put("appObjectId", app.objectId);
        params.put("status", "app");
        CommonParams.getStatusReporter().report(new Status(
                getName(),
                Status.Result.SUCCESSFUL,
                String.format("appId: %s; appObjectId: %s", app.appId, app.objectId)
        ));
    }

    @Override
    public void rollback(Map<String, Object> params) throws IOException {
        //System.out.println("ApplicationStep rollback...");
        Object appObjectId= params.get("appObjectId");
        if(appObjectId != null)
            destroyAadApplication((UUID)appObjectId);
    }

    @Override
    public String getName() {
        return "Creating Azure Active Directory application";
    }

    // helpers

    // Create AAD Application
    private ApplicationRet createAadApplication (
            String displayName,
            String homePage,
            String[] identifierUris,
            String password ) throws IOException {

        /*
            POST https://graph.windows.net/72f988bf-86f1-41af-91ab-2d7cd011db47/applications?api-version=1.6

            {
              "availableToOtherTenants": false,
              "displayName": "ShchAadApp",
              "homepage": "http://github.com/Microsoft/azure-tools-for-java",
              "identifierUris": [
                "http://github.com/Microsoft/azure-tools-for-java"
              ],
              "passwordCredentials": [
                {
                  "startDate": "2016-08-16T04:55:00.3381704Z",
                  "endDate": "2017-08-16T04:55:00.3381704Z",
                  "keyId": "61d97a80-147a-44d2-bccc-13ffda967071",
                  "value": "Zxcv1234"
                }
              ]
            }

        */

        Application app = new Application();
        app.displayName = displayName;
        app.homepage = homePage;
        app.identifierUris.addAll(Arrays.asList(identifierUris));
        PasswordCredentials pc = new PasswordCredentials();
        LocalDateTime dt = LocalDateTime.now( ZoneOffset.UTC);
        //pc.startDate = dt.format( DateTimeFormatter.ISO_DATE_TIME );
        pc.endDate = dt.plusYears(1).format( DateTimeFormatter.ISO_DATE_TIME );;
        pc.keyId = UUID.randomUUID();
        pc.value = password;
        app.passwordCredentials.add(pc);

        ObjectMapper mapper = new ObjectMapper();
        String applicationJson = mapper.writeValueAsString(app);

        String resp = graphRestHelper.doPost("applications", null, applicationJson);
        ApplicationRet applicationRet = mapper.readValue(resp, ApplicationRet.class);
        return applicationRet;
    }

    private void destroyAadApplication (UUID appObjectId) throws IOException {
        // DELETE https://graph.windows.net/72f988bf-86f1-41af-91ab-2d7cd011db47/applications/8a30c28a-dd22-456d-b377-99e6a775f552?api-version=1.6-internal
        String resp = graphRestHelper.doDelete("applications/" + appObjectId.toString(), null, null);
        System.out.println("destroyAadApplication responce: " + resp);
    }

    private ApplicationGet getAadApplication(UUID appId) throws IOException {
        // GET /72f988bf-86f1-41af-91ab-2d7cd011db47/applications?$filter=appId%20eq%20'd43b8e8a-3ab5-436a-b8ab-bef2e3cef533'&api-version=1.6 HTTP/1.1

        String resp = graphRestHelper.doGet("applications", "$filter=appId%20eq%20'" + appId.toString() + "'");
        System.out.println("getAadApplication responce: " + resp);
        ObjectMapper mapper = new ObjectMapper();
        ApplicationGet applicationGet = mapper.readValue(resp, ApplicationGet.class);
        return applicationGet;
    }

}
