/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.applicationinsights.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.microsoft.applicationinsights.ui.activator.Activator;

public class AILibraryUtil {

    private static final int BUFF_SIZE = 1024;

    /**
     * Currently selected project in workspace.
     *
     * @return IProject
     */
    public static IProject getSelectedProject() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        ISelectionService service = window.getSelectionService();
        ISelection selection = service.getSelection();
        Object element = null;
        IResource resource;
        IProject selProject = null;
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSel = (IStructuredSelection) selection;
            element = structuredSel.getFirstElement();
        }
        if (element instanceof IProject) {
            resource = (IResource) element;
            selProject = resource.getProject();
        } else if (element instanceof IJavaProject) {
            IJavaProject proj = ((IJavaElement) element).getJavaProject();
            selProject = proj.getProject();
        }
        return selProject;
    }

    /**
     * This method will display the error message box when any error occurs.It takes two parameters
     *
     * @param shell
     *            parent shell
     * @param title
     *            the text or title of the window.
     * @param message
     *            the message which is to be displayed
     */
    public static void displayErrorDialog(Shell shell, String title, String message) {
        MessageDialog.openError(shell, title, message);
    }

    public static void displayErrorDialogAndLog(Shell shell, String title, String message, Exception e) {
        Activator.getDefault().log(message, e);
        displayErrorDialog(shell, title, message);
    }

    /**
     * copy specified file to eclipse plugins folder.
     */
    public static void copyResourceFile(String resourceFile, String destFile) {
        URL url = Activator.getDefault().getBundle().getEntry(resourceFile);
        URL fileURL;
        try {
            fileURL = FileLocator.toFileURL(url);
            URL resolve = FileLocator.resolve(fileURL);
            File file = new File(resolve.getFile());
            FileInputStream fis = new FileInputStream(file);
            File outputFile = new File(destFile);
            FileOutputStream fos = new FileOutputStream(outputFile);
            writeFile(fis, fos);
        } catch (IOException e) {
            Activator.getDefault().log(e.getMessage(), e);
        }

    }

    /**
     * Method writes contents of file.
     */
    public static void writeFile(InputStream inStream, OutputStream outStream) throws IOException {

        try {
            byte[] buf = new byte[BUFF_SIZE];
            int len = inStream.read(buf);
            while (len > 0) {
                outStream.write(buf, 0, len);
                len = inStream.read(buf);
            }
        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (outStream != null) {
                outStream.close();
            }
        }
    }

    /**
     * createFileIfNotExists.
     */
    public static String createFileIfNotExists(String fileName, String relDirLocation, String resFileLoc)
            throws IOException {
        String path = null;
        try {
            String cmpntFileLoc = getSelectedProject().getFolder(relDirLocation).getLocation().toOSString();
            File fileObject = new File(cmpntFileLoc);
            if (!fileObject.exists()) {
                // to create immediate parent directory of web.xml or ApplicationInsights.xml
                fileObject.mkdir();
            }
            String cmpntFile = String.format("%s%s%s", cmpntFileLoc, File.separator, fileName);
            if (!new File(cmpntFile).exists()) {
                URL url = Activator.getDefault().getBundle().getEntry(resFileLoc);
                URL fileURL = FileLocator.toFileURL(url);
                URL resolve = FileLocator.resolve(fileURL);
                File file = new File(resolve.getFile());
                FileInputStream fis = new FileInputStream(file);
                File outputFile = new File(cmpntFile);
                OutputStream fos = new FileOutputStream(outputFile);
                writeFile(fis, fos);
                path = cmpntFile;
            } else {
                path = cmpntFile;
            }
        } catch (IOException e) {
            throw e;
        }

        return new File(path).getPath();
    }

    /**
     * Method prepares image to display it in dialog.
     */
    public static Image getImage() {
        Image image = null;
        try {
            URL imgUrl = Activator.getDefault().getBundle()
                    .getEntry(com.microsoft.applicationinsights.ui.config.Messages.dlgImgPath);
            URL imgFileURL = FileLocator.toFileURL(imgUrl);
            URL path = FileLocator.resolve(imgFileURL);
            String imgpath = path.getFile();
            image = new Image(null, new FileInputStream(imgpath));
        } catch (Exception e) {
            Activator.getDefault().log(e.getMessage(), e);
        }
        return image;
    }
}
