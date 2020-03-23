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
