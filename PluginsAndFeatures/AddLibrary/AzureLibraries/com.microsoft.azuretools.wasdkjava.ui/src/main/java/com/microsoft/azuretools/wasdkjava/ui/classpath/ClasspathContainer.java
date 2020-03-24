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

package com.microsoft.azuretools.wasdkjava.ui.classpath;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.osgi.framework.Bundle;

import com.microsoft.azuretools.wasdkjava.ui.Activator;

/**
 * Classpath container implementation.
 *
 */
public class ClasspathContainer implements IClasspathContainer {

    private IPath containerPath;

    /**
     * This class acts as a filter to accept jar files only. Does not include sources jars.
     */
    class SDKJarsFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return (name.endsWith(".jar") && name.indexOf(Messages.src) == -1);
        }
    }

    /**
     * Constructor.
     */
    public ClasspathContainer(IPath path) {
        super();
        this.containerPath = path;
    }

    /**
     * Returns the classpath entries.
     */
    @Override
    public IClasspathEntry[] getClasspathEntries() {

        Bundle bundle = Platform.getBundle(Messages.sdkID);
        // Search the available SDKs
        Bundle[] bundles = Platform.getBundles(Messages.sdkID, null);
        List<IClasspathEntry> listEntries = new ArrayList<IClasspathEntry>();
        if (bundles != null) {
            for (Bundle bundle2 : bundles) {
                if (bundle2.getVersion().toString().startsWith(containerPath.segment(1))) {
                    bundle = bundle2;
                    break;
                }
            }

            // Get the SDK jar.
            URL sdkJar = FileLocator.find(bundle, new Path(Messages.sdkJar), null);
            URL resSdkJar = null;
            try {
                if (sdkJar != null) {
                    resSdkJar = FileLocator.resolve(sdkJar);
                    // create classpath attribute for java doc, if present
                }
                if (resSdkJar == null) {
                    /*
                     * if sdk jar is not present then create an place holder for sdk jar so that it would be shown as
                     * missing file
                     */
                    URL bundleLoc = new URL(bundle.getLocation());
                    StringBuffer strBfr = new StringBuffer(bundleLoc.getPath());
                    strBfr.append(File.separator).append(Messages.sdkJar);
                    URL jarLoc = new URL(strBfr.toString());
                    IPath jarPath = new Path(FileLocator.resolve(jarLoc).getPath());
                    File jarFile = jarPath.toFile();
                    listEntries.add(JavaCore.newLibraryEntry(new Path(jarFile.getAbsolutePath()), null, null, null,
                            null, true));
                } else {
                    File directory = new File(resSdkJar.getPath());
                    // create the library entry for sdk jar
                    listEntries.add(JavaCore.newLibraryEntry(new Path(directory.getAbsolutePath()), null, null, null,
                            null, true));
                    FilenameFilter sdkJarsFilter = new SDKJarsFilter();
                    File[] jars = new File(directory.getParent()).listFiles(sdkJarsFilter);
                    for (int i = 0; i < jars.length; i++) {
                        if (jars[i].getName().contains(Messages.appInsightMng)
                                || jars[i].getName().contains(Messages.adAuth)
                                || jars[i].getName().contains(Messages.srvExp)) {
                            /*
                             * Do not add them as they are not part of Azure SDK. They are just used for coding purpose.
                             */
                        } else {
                            listEntries.add(JavaCore.newLibraryEntry(new Path(jars[i].getAbsolutePath()), null, null,
                                    null, null, true));
                        }
                    }
                }
            } catch (Exception e) {
                listEntries = new ArrayList<IClasspathEntry>();
                Activator.getDefault().log(Messages.excp, e);
            }
        }

        IClasspathEntry[] entries = new IClasspathEntry[listEntries.size()];
        // Return the classpath entries.
        return listEntries.toArray(entries);
    }

    @Override
    public String getDescription() {
        return Messages.containerDesc;
    }

    @Override
    public int getKind() {
        return IClasspathContainer.K_APPLICATION;
    }

    @Override
    public IPath getPath() {
        return containerPath;
    }

}
