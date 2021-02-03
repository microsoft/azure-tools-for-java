/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.hdinsight.jobs;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.lang.reflect.Constructor;

public class JobViewFxUtil {
    public static Object startFx(Object composite, final String webUrl, Object jobUtils) {
        try {
            Class compositeClass = Class.forName("org.eclipse.swt.widgets.Composite");
            Class[] paramTypes = {compositeClass, int.class};
            Constructor con = FXCanvas.class.getConstructor(paramTypes);
            Object[] parames = {composite, 1 << 16};
            final FXCanvas canvas = (FXCanvas) con.newInstance(parames);
            Platform.setImplicitExit(false);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    final WebView webView = new WebView();
                    Scene scene = new Scene(webView);
                    canvas.setScene(scene);
                    WebEngine webEngine = webView.getEngine();
                    webEngine.load(webUrl);

                    webEngine.getLoadWorker().stateProperty().addListener(
                            new ChangeListener<Worker.State>() {
                                @Override
                                public void changed(ObservableValue<? extends Worker.State> ov,
                                                    Worker.State oldState, Worker.State newState) {
                                    if (newState == Worker.State.SUCCEEDED) {
                                        JSObject win = (JSObject) webEngine.executeScript("window");
                                        win.setMember("JobUtils", new JobUtilsForEclipse());
                                    }
                                }
                            }
                    );
                }
            });
            return canvas;
        } catch (Exception e) {
            return e;
        }
    }
}
