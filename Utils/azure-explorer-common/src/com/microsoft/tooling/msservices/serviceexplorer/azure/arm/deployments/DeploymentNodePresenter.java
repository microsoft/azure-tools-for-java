/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments;

import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import rx.Observable;
import rx.exceptions.Exceptions;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.ARM;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.EXPORT_TEMPALTE_FILE;

public class DeploymentNodePresenter<V extends DeploymentNodeView> extends MvpPresenter<V> {

    public void onGetExportTemplateRes(String template, File file) {
        Operation operation = TelemetryManager.createOperation(ARM, EXPORT_TEMPALTE_FILE);
        Observable.fromCallable(() -> {
            operation.start();
            IOUtils.write(template, new FileOutputStream(file), Charset.defaultCharset());
            return true;
        }).subscribe(res -> DefaultLoader.getIdeHelper().invokeLater(() -> {
            operation.complete();
            if (!isViewDetached()) {
                getMvpView().showExportTemplateResult(true, null);
            }
        }), ex -> {
            operation.complete();
            EventUtil.logError(operation, ErrorType.systemError, Exceptions.propagate(ex), null, null);
            if (!isViewDetached()) {
                getMvpView().showExportTemplateResult(false, ex);
            }
        });
    }
}
