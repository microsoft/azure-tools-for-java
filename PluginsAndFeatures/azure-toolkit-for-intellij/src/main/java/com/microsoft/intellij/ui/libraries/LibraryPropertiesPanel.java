/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.libraries;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.ui.AzureAbstractPanel;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.intellij.util.PluginUtil;
import com.wacommon.utils.WACommonException;

import javax.swing.*;
import java.io.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

class LibraryPropertiesPanel implements AzureAbstractPanel {
    private static final int BUFF_SIZE = 1024;

    private JPanel rootPanel;
    private JCheckBox depCheck;
    private JLabel libraryVersion;
    private JLabel location;
    private AzureLibrary azureLibrary;
    private Module module;

    private boolean isEdit;

    public LibraryPropertiesPanel(Module module, AzureLibrary azureLibrary, boolean isEdit, boolean isExported) {
        this.module = module;
        this.azureLibrary = azureLibrary;
        this.isEdit = isEdit;
        init();
        depCheck.setSelected(isExported);
    }

    public void init() {
        if (!isEdit()) {
            // Add library scenario
            depCheck.setSelected(true);
        }
    }

    public JComponent prepare() {
        libraryVersion.setText(azureLibrary.getName());
        String locationText = "";
        try {
            // if there is no specific location, files are on plugin classpath
            locationText = azureLibrary.getLocation() == null ?
                    PluginHelper.getAzureLibLocation() : String.format("%s%s%s", AzurePlugin.pluginFolder, File.separator, azureLibrary.getLocation());
        } catch (WACommonException ex) {
            PluginUtil.displayErrorDialogAndLog(message("error"), ex.getMessage(), ex);
        }
        location.setText(locationText);
        rootPanel.revalidate();
        return rootPanel;
    }

    public boolean onFinish() {
        return true;
    }

    @Override
    public JComponent getPanel() {
        return prepare();
    }

    @Override
    public String getDisplayName() {
        return message("edtLbrTtl");
    }

    @Override
    public boolean doOKAction() {
        return true;
    }

    @Override
    public String getSelectedValue() {
        return null;
    }

    public boolean isExported() {
        return depCheck.isSelected();
    }

    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public String getHelpTopic() {
        return null;
    }

    public static void copy(File source, final File destination) throws IOException {
        InputStream instream = null;
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdirs();
            }
            String[] kid = source.list();
            for (int i = 0; i < kid.length; i++) {
                copy(new File(source, kid[i]),
                        new File(destination, kid[i]));
            }
        } else {
            //InputStream instream = null;
            OutputStream out = null;
            try {
                if (destination != null && destination.isFile() && !destination.getParentFile().exists())
                    destination.getParentFile().mkdirs();

                instream = new FileInputStream(source);
                out = new FileOutputStream(destination);
                byte[] buf = new byte[BUFF_SIZE];
                int len = instream.read(buf);

                while (len > 0) {
                    out.write(buf, 0, len);
                    len = instream.read(buf);
                }
            } finally {
                if (instream != null) {
                    instream.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    /**
     * @return current window is edit or not
     */
    private boolean isEdit() {
        return isEdit;
    }

    public String getHelpId() {
        return "acs_config_dialog";
    }
}
