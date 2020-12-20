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

package com.microsoft.tooling.msservices.serviceexplorer;

public class AzureAllIcons {

    private static final String ICON_BASE_DIR = "/icons/";

    public static final class Common {

        public static final AzureIcon CREATE = loadAzureIcon(ICON_BASE_DIR + "mysql/Create.svg", ICON_BASE_DIR + "mysql/Create_dark.svg");

        public static final AzureIcon DELETE = loadAzureIcon(ICON_BASE_DIR + "mysql/Delete.svg", ICON_BASE_DIR + "mysql/Delete_dark.svg");

        public static final AzureIcon START = loadAzureIcon(ICON_BASE_DIR + "mysql/Start.svg", ICON_BASE_DIR + "mysql/Start_dark.svg");

        public static final AzureIcon STOP = loadAzureIcon(ICON_BASE_DIR + "mysql/Stop.svg", ICON_BASE_DIR + "mysql/Stop_dark.svg");

        public static final AzureIcon RESTART = loadAzureIcon(ICON_BASE_DIR + "mysql/Restart.svg", ICON_BASE_DIR + "mysql/Restart_dark.svg");

        public static final AzureIcon OPEN_IN_PORTAL = loadAzureIcon(ICON_BASE_DIR + "mysql/OpenInPortal.svg", ICON_BASE_DIR + "mysql/OpenInPortal_dark.svg");

        public static final AzureIcon SHOW_PROPERTIES = loadAzureIcon(ICON_BASE_DIR + "mysql/ShowProperties.svg",
                ICON_BASE_DIR + "mysql/ShowProperties_dark.svg");
    }

    public static final class MySQL {

        private static final String MYSQL_BASE_DIR = ICON_BASE_DIR + "mysql/";

        public static final AzureIcon MODULE = loadAzureIcon(ICON_BASE_DIR + "mysql/MySQL.svg");

        public static final AzureIcon RUNNING = loadAzureIcon(ICON_BASE_DIR + "mysql/MySQLRunning.svg");

        public static final AzureIcon STOPPED = loadAzureIcon(ICON_BASE_DIR + "mysql/MySQLStopped.svg");

        public static final AzureIcon UPDATING = loadAzureIcon(ICON_BASE_DIR + "mysql/MySQLUpdating.svg");

        public static final AzureIcon CONNECT_TO_SERVER = loadAzureIcon(ICON_BASE_DIR + "mysql/ConnectToServer.svg",
                ICON_BASE_DIR + "mysql/ConnectToServer_dark.svg");

        public static final AzureIcon BIND_INTO = loadAzureIcon(ICON_BASE_DIR + "mysql/BindInto.svg", ICON_BASE_DIR + "mysql/BindInto_dark.svg");

    }

    private static AzureIcon loadAzureIcon(String iconPath) {
        return AzureIcon.builder().icon(iconPath).darkIcon(iconPath).build();
    }

    private static AzureIcon loadAzureIcon(String iconPath, String darkIconPath) {
        return AzureIcon.builder().icon(iconPath).darkIcon(darkIconPath).build();
    }

}
