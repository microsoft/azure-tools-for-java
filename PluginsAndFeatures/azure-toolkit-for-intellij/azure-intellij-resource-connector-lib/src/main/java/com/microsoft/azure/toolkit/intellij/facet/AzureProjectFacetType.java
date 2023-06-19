/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.framework.detection.FacetBasedFrameworkDetector;
import com.intellij.framework.detection.FileContentPattern;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.util.indexing.FileContent;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collection;

public class AzureProjectFacetType extends FacetType<AzureProjectFacet, AzureProjectFacetConfiguration> {
    private static final String STRING_ID = "Azure";
    private static final String PRESENTABLE_NAME = "Azure Resource Connections";
    public static final FacetTypeId<AzureProjectFacet> ID = new FacetTypeId<>(STRING_ID);
    public static final AzureProjectFacetType INSTANCE = new AzureProjectFacetType();

    public AzureProjectFacetType() {
        super(ID, STRING_ID, PRESENTABLE_NAME);
    }

    @Override
    public AzureProjectFacetConfiguration createDefaultConfiguration() {
        return new AzureProjectFacetConfiguration();
    }

    @Override
    public AzureProjectFacet createFacet(@Nonnull Module module, String name, @Nonnull AzureProjectFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return new AzureProjectFacet(this, module, name, configuration, underlyingFacet);
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
        return StringUtils.containsIgnoreCase(moduleType.getId(), "JAVA");
    }

    @Override
    public boolean isOnlyOneFacetAllowed() {
        return true;
    }

    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE);
    }

    @Getter
    public static class AzureProjectFacetDetector extends FacetBasedFrameworkDetector<AzureProjectFacet, AzureProjectFacetConfiguration> {
        private final FacetType<AzureProjectFacet, AzureProjectFacetConfiguration> facetType = AzureProjectFacetType.INSTANCE;
        private final FileType fileType = XmlFileType.INSTANCE;

        public AzureProjectFacetDetector() {
            super(STRING_ID);
        }

        @Override
        @Nullable
        protected AzureProjectFacetConfiguration createConfiguration(Collection<? extends VirtualFile> files) {
            return files.stream().findAny().map(VirtualFile::getParent).map(AzureProjectFacetConfiguration::new).orElse(null);
        }

        @Nonnull
        @Override
        public ElementPattern<FileContent> createSuitableFilePattern() {
            return FileContentPattern.fileContent().inDirectory(".azure").withName("profiles.xml");
        }
    }
}