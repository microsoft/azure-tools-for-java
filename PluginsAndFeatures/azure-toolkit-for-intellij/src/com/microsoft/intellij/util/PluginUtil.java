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

package com.microsoft.intellij.util;

import com.intellij.ide.DataManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogEarthquakeShaker;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.microsoft.intellij.IToolWindowProcessor;
import com.microsoft.intellij.ToolWindowKey;
import com.microsoft.intellij.common.CommonConst;

import javax.swing.Icon;

import java.io.File;
import java.util.HashMap;


public class PluginUtil {
    public static final String BASE_PATH = "${basedir}" + File.separator + "..";
    private static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.util.PluginUtil");
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String PLATFORM_PREFIX_KEY = "idea.platform.prefix";

    private static final String IDEA_PREFIX = "idea";
    private static final String IDEA_CE_PREFIX = "Idea";
    private static final String COMMUNITY_PREFIX = IDEA_CE_PREFIX;

    //todo: check with multiple Idea projects open in separate windows
    private static HashMap<ToolWindowKey, IToolWindowProcessor> toolWindowManagerCollection = new HashMap<>();

    public static void registerToolWindowManager(ToolWindowKey toolWindowFactoryKey, IToolWindowProcessor toolWindowProcessor) {
        synchronized (PluginUtil.class) {
            toolWindowManagerCollection.put(toolWindowFactoryKey, toolWindowProcessor);
        }
    }

    public static IToolWindowProcessor getToolWindowManager(ToolWindowKey toolWindowKey) {
        return toolWindowManagerCollection.get(toolWindowKey);
    }

    public static boolean isContainsToolWindowKey(ToolWindowKey key) {
        return toolWindowManagerCollection.containsKey(key);
    }

    public static boolean isModuleRoot(VirtualFile moduleFolder, Module module) {
        return moduleFolder != null && ProjectRootsUtil.isModuleContentRoot(moduleFolder, module.getProject());
    }

    public enum ProjExportType { WAR, EAR, JAR }

    /**
     * This method returns current project.
     *
     * @return Project
     */
    public static Project getSelectedProject() {
        final DataContext dataContext = DataManager.getInstance().getDataContextFromFocus().getResult();
        return DataKeys.PROJECT.getData(dataContext);
    }

    public static Module getSelectedModule() {
        final DataContext dataContext = DataManager.getInstance().getDataContextFromFocus().getResult();
        return DataKeys.MODULE.getData(dataContext);
    }

    public static String getModulePath(Module module) {
        return new File(module.getModuleFilePath()).getParent();
    }

    /**
     * This method will display the error message box when any error occurs.It takes two parameters
     *
     * @param title   the text or title of the window.
     * @param message the message which is to be displayed
     */
    public static void displayErrorDialog(String title, String message) {
        Messages.showErrorDialog(message, title);
    }

    public static void displayErrorDialogAndLog(String title, String message, Exception e) {
        LOG.error(message, e);
        displayErrorDialog(title, message);
    }

    public static void displayErrorDialogInAWTAndLog(final String title, final String message, Throwable e) {
        LOG.error(message, e);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                PluginUtil.displayErrorDialog(title, message);
            }
        });
    }

    public static void displayInfoDialog(String title, String message) {
        Messages.showInfoMessage(message, title);
    }

    public static void displayWarningDialog(String title, String message) {
        Messages.showWarningDialog(message, title);
    }

    public static void displayWarningDialogInAWT(final String title, final String message) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                displayWarningDialog(title, message);
            }
        });
    }

    /**
     * This method find the absolute path from
     * relative path.
     *
     * @param path : relative path
     * @return absolute path
     */
    public static String convertPath(Project project, String path) {
        String newPath = "";
        if (path.startsWith(BASE_PATH)) {
            final String projectPath = project.getBasePath();
            final String rplStr = path.substring(path.indexOf('}') + 4, path.length());
            newPath = String.format("%s%s", projectPath, rplStr);
        } else {
            newPath = path;
        }
        return newPath;
    }

    public static Module findModule(Project project, String path) {
        for (final Module module : ModuleManager.getInstance(project).getModules()) {
            if (PluginUtil.getModulePath(module).equals(path)) {
                return module;
            }
        }
        return null;
    }

    public static String getPluginRootDirectory() {
        final IdeaPluginDescriptor pluginDescriptor = PluginManager.getPlugin(PluginId.findId(CommonConst.PLUGIN_ID));
        return pluginDescriptor.getPath().getAbsolutePath();
    }

    public static Icon getIcon(String iconPath) {
        return IconLoader.getIcon(iconPath);
    }

    public static void dialogShaker(ValidationInfo info, DialogWrapper dialogWrapper) {
        if (info.component != null && info.component.isVisible()) {
            IdeFocusManager.getInstance((Project) null).requestFocus(info.component, true);
        }

        DialogEarthquakeShaker.shake(dialogWrapper.getPeer().getWindow());
    }

    public static void showInfoNotificationProject(Project project, String title, String message) {
        new Notification(NOTIFICATION_GROUP_ID, title,
                         message, NotificationType.INFORMATION).notify(project);
    }

    public static void showWarningNotificationProject(Project project, String title, String message) {
        new Notification(NOTIFICATION_GROUP_ID, title, message, NotificationType.WARNING).notify(project);
    }

    public static void showErrorNotificationProject(Project project, String title, String message) {
        new Notification(NOTIFICATION_GROUP_ID, title,
                         message, NotificationType.ERROR).notify(project);
    }

    public static void showInfoNotification(String title, String message) {
        Notification notification = new Notification(NOTIFICATION_GROUP_ID, title,
                                                     message, NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
    }

    public static void showWarnNotification(String title, String message) {
        Notification notification = new Notification(NOTIFICATION_GROUP_ID, title,
                                                     message, NotificationType.WARNING);
        Notifications.Bus.notify(notification);
    }

    public static void showErrorNotification(String title, String message) {
        Notification notification = new Notification(NOTIFICATION_GROUP_ID, title,
                                                     message, NotificationType.ERROR);
        Notifications.Bus.notify(notification);
    }

    public static String getPlatformPrefix() {
        return getPlatformPrefix(IDEA_PREFIX);
    }

    public static String getPlatformPrefix(String defaultPrefix) {
        return System.getProperty(PLATFORM_PREFIX_KEY, defaultPrefix);
    }

    public static boolean isIdeaUltimate() {
        return is(IDEA_PREFIX);
    }

    public static boolean isIdeaCommunity() {
        return is(COMMUNITY_PREFIX);
    }

    private static boolean is(String idePrefix) {
        return idePrefix.equals(getPlatformPrefix());
    }
}
