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

package com.microsoft.intellij.runner.webapp.webappconfig;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.runner.AzureProcessHandler;
import org.apache.commons.net.ftp.FTPClient;
import org.jetbrains.idea.maven.model.MavenConstants;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.FileInputStream;
import java.io.IOException;

public class WebDeployUtil {

    private static final String PROCESS_TERMINATED = "The process has been terminated";
    private static final String GETTING_DEPLOYMENT_CREDENTIAL = "Getting Deployment Credential...";
    private static final String CONNECTING_FTP = "Connecting to FTP server...";
    private static final String UPLOADING_WAR = "Uploading war file...";
    private static final String UPLOADING_SUCCESSFUL = "Uploading successfully...";
    private static final String LOGGING_OUT = "Logging out of FTP server...";
    private static final String DEPLOY_SUCCESSFUL = "Deploy successfully...";

    private static final String BASE_PATH = "/site/wwwroot/webapps/";
    private static final String ROOT_PATH = BASE_PATH + "ROOT";
    private static final String ROOT_FILE_PATH = ROOT_PATH + "." + MavenConstants.TYPE_WAR;

    public static void deployWebApp(WebAppSettingModel webAppSettingModel, AzureProcessHandler handler) {
        Observable.fromCallable(() -> {
            if (webAppSettingModel.isCreatingNew()) {
                //todo: creating
            }
            println(handler,GETTING_DEPLOYMENT_CREDENTIAL, ProcessOutputTypes.STDOUT);
            WebApp webApp = AzureWebAppMvpModel.getInstance().getWebAppById(webAppSettingModel.getSubscriptionId(), webAppSettingModel.getWebAppId());
            FTPClient ftp;
            FileInputStream input;
            println(handler,CONNECTING_FTP, ProcessOutputTypes.STDOUT);
            ftp = WebAppUtils.getFtpConnection(webApp.getPublishingProfile());
            println(handler,UPLOADING_WAR, ProcessOutputTypes.STDOUT);
            input = new FileInputStream(webAppSettingModel.getTargetPath());
            boolean isSuccess;
            if (webAppSettingModel.isDeployToRoot()) {
                // Deploy to Root
                WebAppUtils.removeFtpDirectory(ftp, ROOT_PATH, null);
                isSuccess = ftp.storeFile(ROOT_FILE_PATH, input);
            } else {
                //Deploy according to war file name
                WebAppUtils.removeFtpDirectory(ftp, BASE_PATH + webAppSettingModel.getTargetName(), null);
                isSuccess = ftp.storeFile(BASE_PATH + webAppSettingModel.getTargetName(), input);
            }
            if (!isSuccess) {
                int rc = ftp.getReplyCode();
                throw new IOException("FTP client can't store the artifact, reply code: " + rc);
            }
            println(handler,UPLOADING_SUCCESSFUL, ProcessOutputTypes.STDOUT);
            println(handler,LOGGING_OUT, ProcessOutputTypes.STDOUT);
            ftp.logout();
            input.close();
            if (ftp.isConnected()) {
                ftp.disconnect();
            }
            return null;
        })
        .subscribeOn(Schedulers.io())
        .subscribe(number -> {
            println(handler, DEPLOY_SUCCESSFUL, ProcessOutputTypes.STDOUT);
            print(handler, "URL: ", ProcessOutputTypes.STDOUT);
            String url = webAppSettingModel.getWebAppUrl();
            if (!webAppSettingModel.isDeployToRoot()) {
                url += "/" + webAppSettingModel.getTargetName()
                        .substring(0, webAppSettingModel.getTargetName().indexOf("." + MavenConstants.TYPE_WAR));
            }
            println(handler, url, ProcessOutputTypes.STDOUT);
            handler.notifyProcessTerminated(0 /*exitCode*/);
        }, err -> {
            handler.notifyTextAvailable(err.getMessage(), ProcessOutputTypes.STDERR);
            handler.notifyProcessTerminated(0 /*exitCode*/);
        });
    }

    static private void print(AzureProcessHandler processHandler, String message, Key type) {
        if (!processHandler.isProcessTerminating() && !processHandler.isProcessTerminated()) {
            processHandler.notifyTextAvailable(message, type);
        } else {
            throw new Error(PROCESS_TERMINATED);
        }
    }

    static private void println(AzureProcessHandler processHandler, String message, Key type) {
        if (!processHandler.isProcessTerminating() && !processHandler.isProcessTerminated()) {
            processHandler.notifyTextAvailable(message + "\n", type);
        } else {
            throw new Error(PROCESS_TERMINATED);
        }
    }
}
