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

package com.microsoft.azuretools.azureexplorer.hdinsight;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class FxClassLoader {

    private static final String JOB_VIEW_FX_UTILS_NAME = "com.microsoft.hdinsight.jobs.JobViewFxUtil";
    private static final String JOB_VIEW_FX_JAR_PATH = PluginUtil.pluginFolder + "/com.microsoft.azuretools.hdinsight" + "/hdinsight-job-view.jar";

    private static boolean isJavaFxLoaded = false;

    private static ClassLoader myClassLoader = null;
    private static Class jobViewFxUtilslCLass = null;
    private static Method method = null;

    private static String efxclipsePluginSymbolicName = "org.eclipse.fx.ide.css.jfx8";
    private static String efxclipseMarketplaceURL = "http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=335225";
    // FWLink for http://www.eclipse.org/efxclipse/install.html
    private static String efxclipseManualInstallURL = "https://go.microsoft.com/fwlink/?linkid=861126";

    public static void loadJavaFxForJobView(Composite composite, String url) {
        if(!isJavaFxLoaded) {
            if (!PluginUtil.isJavaVersionHigherThanTarget(1.7f)) {
                PluginUtil.forceInstallPluginUsingMarketPlaceAsync(efxclipsePluginSymbolicName, efxclipseMarketplaceURL, efxclipseManualInstallURL);
            }

            tryLoadJavaFx();
        }

        if(isJavaFxLoaded) {
            try {
                // new JobUtils() case crash on Eclipse under Mac&Ubuntu, so we just pass empty string to JavaFx.
                method.invoke(null, composite, url, "");
            } catch (Exception e) {
                Activator.getDefault().log("HDInsight: load JavaFx error", e);
            }
        }
    }

    private static URLClassLoader createSWTFXClassLoader() throws Exception {
        ClassLoader parent = FxClassLoader.class.getClassLoader();
        File javaHome = null;
        try {
            javaHome = new File(System.getProperty("java.home")).getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get java home", e);
        }
        if (!javaHome.exists()) {
            throw new IllegalStateException("Java home \"" + javaHome.getAbsolutePath() + "\" doesn't exits");
        }

        File swtFxFile = new File(javaHome.getAbsolutePath() + "/lib/jfxswt.jar");
        File swtFxRtFile = new File(javaHome.getAbsolutePath() + "/lib/ext/jfxrt.jar");
        File jobViewFile = new File(JOB_VIEW_FX_JAR_PATH);
        if (swtFxFile.exists() && jobViewFile.exists() && swtFxRtFile.exists()) {
            return new URLClassLoader(new URL[] { swtFxFile.getCanonicalFile().toURI().toURL(),
                    jobViewFile.getCanonicalFile().toURI().toURL(), swtFxRtFile.getCanonicalFile().toURI().toURL() }, parent);
        }
        return null;
    }



    private static void tryLoadJavaFx() {
        try {
            loadJavaFxCompont();
            isJavaFxLoaded = true;
        }catch(Exception e) {
            Activator.getDefault().log("HDInsight: load JavaFx error", e);
        }
    }

    private static void loadJavaFxCompont() throws Exception {
        myClassLoader = createSWTFXClassLoader();
        jobViewFxUtilslCLass = Class.forName(JOB_VIEW_FX_UTILS_NAME, true, myClassLoader);
        method = jobViewFxUtilslCLass.getMethod("startFx", Object.class, String.class, Object.class);
    }
}
