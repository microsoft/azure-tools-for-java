/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.projects.template;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import com.microsoft.azure.hdinsight.projects.samples.ProjectSampleUtil;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TemplatesUtil {
    private static final String TEMPLATE_ROOT_FOLDER_NAME = "templates";
    private static final String XML_FILE_NAME = "template.xml";

    private static JAXBContext jaxbContext = null;
    private static Unmarshaller unmarshaller = null;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(CustomTemplateInfo.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<CustomHDInsightTemplateItem> getCustomTemplate() {
        List<CustomHDInsightTemplateItem> items = new ArrayList<>();
        File[] templatesFiles = getAllTemplateFolders();
        if (templatesFiles == null) {
            return items;
        }

        for (int i = 0; i < templatesFiles.length; ++i) {
            CustomTemplateInfo info = getTemplateInfo(templatesFiles[i]);
            if (info != null) {
                items.add(new CustomHDInsightTemplateItem(info));
            }
        }

        return items;
    }

    public static File[] getAllTemplateFolders() {
        final File file = getTemplateRootFolder();
        return file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    File[] files = pathname.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.equals(XML_FILE_NAME);
                        }
                    });

                    if (files.length == 1) {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    public static String getTemplateRootFolderPath() {
        return getTemplateRootFolder().getAbsolutePath();
    }

    public static void createTemplateSampleFiles(Module module, CustomTemplateInfo info) {
        List<String> sourceFiles = info.getSourceFiles();
        String sourceRootPath = ProjectSampleUtil.getRootOrSourceFolder(module, true);

        if (sourceRootPath != null) {
            for (int i = 0; i < sourceFiles.size(); ++i) {
                try {
                    FileUtil.copy(new File(TemplatesUtil.getTemplateRootFolderPath() + sourceFiles.get(i)), new File(sourceRootPath + sourceFiles.get(i)));
                } catch (IOException e) {

                }
            }
        }
    }

    private static File getTemplateRootFolder() {
        File templateRoot = new File(StringHelper.concat(PluginUtil.getPluginRootDirectory(), File.separator, TEMPLATE_ROOT_FOLDER_NAME));
        return templateRoot;
    }

    private static CustomTemplateInfo getTemplateInfo(File file) {
        File templateInfoFile = new File(StringHelper.concat(file.getAbsolutePath(), File.separator, XML_FILE_NAME));

        CustomTemplateInfo info = null;
        try {
            info = (CustomTemplateInfo) unmarshaller.unmarshal(templateInfoFile);
        } catch (JAXBException e) {
            DefaultLoader.getUIHelper().showException("Failed to get template info from template.xml", e, "Custom Template", true, true);
        }
        return info;
    }
}
