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
