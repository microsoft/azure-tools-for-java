/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.sqljdbc4.ui.classpath;

import java.io.File;
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

import com.microsoft.sqljdbc4.ui.activator.Activator;

/**
 * Classpath container implementation.
 *
 */
public class ClasspathContainer implements IClasspathContainer {

    private IPath containerPath;

    /**
     * Constructor.
     *
     * @param path.
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
        //Search the available SDKs
        Bundle[] bundles = Platform.getBundles(Messages.sdkID,
                null);
        List<IClasspathEntry> listEntries = new ArrayList<IClasspathEntry>();
        if (bundles != null) {
            for (Bundle bundle2 : bundles) {
                if (bundle2.getVersion().toString().startsWith(
                        containerPath.segment(1))) {
                    bundle = bundle2;
                    break;
                }
            }

            //Get the SDK jar.
            URL sdkJar = FileLocator.find(bundle,
                    new Path(Messages.sdkJar), null);
            URL resSdkJar = null;
            try {
                if (sdkJar != null) {
                    resSdkJar = FileLocator.resolve(sdkJar);
                    //create classpath attribute for java doc, if present
                }
                if (resSdkJar == null) {
                    //if sdk jar is not present then create an place holder
                    //for sdk jar so that it would be shown as missing file
                    URL bundleLoc = new URL(bundle.getLocation());
                    StringBuffer strBfr = new StringBuffer(bundleLoc.getPath());
                    strBfr.append(File.separator).append(Messages.sdkJar);
                    URL jarLoc = new URL(strBfr.toString());
                    IPath jarPath = new Path(
                            FileLocator.resolve(jarLoc).getPath());
                    File jarFile = jarPath.toFile();
                    listEntries.add(JavaCore.newLibraryEntry(new Path(
                            jarFile.getAbsolutePath()),
                            null, null, null, null, true));
                } else {
                    File directory = new File(resSdkJar.getPath());
                    //create the library entry for sdk jar
                    listEntries.add(JavaCore.newLibraryEntry(new Path(
                            directory.getAbsolutePath()), null, null, null,
                            null, true));
                }
            } catch (Exception e) {
                listEntries = new ArrayList<IClasspathEntry>();
                Activator.getDefault().log(Messages.excp, e);
            }
        }
        IClasspathEntry[] entries = new IClasspathEntry[listEntries.size()];
        //Return the classpath entries.
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


