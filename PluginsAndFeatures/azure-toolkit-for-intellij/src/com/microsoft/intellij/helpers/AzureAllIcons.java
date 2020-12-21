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

package com.microsoft.intellij.helpers;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class AzureAllIcons {

    private static final String ICON_BASE_DIR = "/icons/";

    public static final class Common {

        public static final Icon CREATE = AllIcons.Welcome.CreateNewProject;

        public static final Icon DELETE = AllIcons.Actions.GC;

        public static final Icon START = loadIcon("common/Start.svg");

        public static final Icon STOP = loadIcon("common/Stop.svg");

        public static final Icon RESTART = AllIcons.Actions.Restart;

        public static final Icon OPEN_IN_PORTAL = loadIcon("common/OpenInPortal.svg");

        public static final Icon SHOW_PROPERTIES = AllIcons.Actions.Properties;
    }

    public static final class MySQL {

        public static final Icon MODULE = loadIcon("mysql/MySQL.svg");

        public static final Icon RUNNING = loadIcon("mysql/MySQLRunning.svg");

        public static final Icon STOPPED = loadIcon("mysql/MySQLStopped.svg");

        public static final Icon UPDATING = loadIcon("mysql/MySQLUpdating.svg");

        public static final Icon CONNECT_TO_SERVER = loadIcon("mysql/ConnectToServer.svg");

        public static final Icon BIND_INTO = loadIcon("mysql/BindInto.svg");

    }

    private static Icon loadIcon(String iconPath) {
        return IconLoader.getIcon(ICON_BASE_DIR + iconPath);
    }

}
