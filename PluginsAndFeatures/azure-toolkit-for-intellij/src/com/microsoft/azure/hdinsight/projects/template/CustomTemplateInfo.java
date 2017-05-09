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

import com.microsoft.azuretools.azurecommons.helpers.StringHelper;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.List;

@XmlRootElement(name = "Template")
public class CustomTemplateInfo {

    @XmlAttribute(name = "TemplateName")
    private String name;

    @XmlElement(name = "IconPath")
    private String iconPath;

    @XmlElement(name = "Description")
    private String description;

    @XmlElementWrapper(name = "SourceFilePaths")
    @XmlElement(name = "Path")
    private List<String> sourceFiles;

    @XmlElementWrapper(name = "DependencyLibraryPaths")
    @XmlElement(name = "Path")
    private List<String> dependencyFiles;

    @XmlElement(name = "IsNeedSparkSDK")
    private boolean isNeedSparkSDK;

    @XmlElement(name = "IsNeedScalaSDK")
    private boolean isNeedScalaSDK;

    @XmlElement(name = "IsSparkProject")
    private boolean isSparkProject;

    public CustomTemplateInfo() {
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconPath() {
        return getTemplateRootPath() + iconPath;
    }

    public List<String> getSourceFiles() {
        return sourceFiles;
    }

    public List<String> getDependencyFiles() {
        return dependencyFiles;
    }

    public boolean isNeedSparkSDK() {
        return isNeedSparkSDK;
    }

    public boolean isNeedScalaSDK() {
        return isNeedScalaSDK;
    }

    public boolean isSparkProject() {
        return isSparkProject;
    }

    private String getTemplateRootPath() {
        return StringHelper.concat(TemplatesUtil.getTemplateRootFolderPath(), File.separator, name);
    }
}
