/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri.report;

import com.microsoft.azuretools.authmanage.FileStorage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by vlashch on 10/20/16.
 */
public class FileListener implements IListener<String> {
    private final static Logger LOGGER = Logger.getLogger(FileListener.class.getName());
    private final FileStorage fsReport;
    public FileListener(String filename, String path) throws IOException {
        fsReport = new FileStorage(filename, path);
    }

    @Override
    public void listen(String message) {
        try {
            fsReport.appendln(message);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.log(Level.SEVERE, "listen@FileListener", e);
        }
    }
}
