/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common.survey;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.IconLoader;
import com.microsoft.azure.toolkit.lib.Azure;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CustomerSurvey implements ICustomerSurvey {

    AZURE_SDK(Constants.AZURE_SDK_SURVEY_LINK, Constants.AZURE_SDK_SURVEY, Constants.AZURE_ICON) {
        @Override
        public String getLink() {
            return String.format(super.getLink(), getInstallationId());
        }
    },
    AZURE_CLIENT_SDK(Constants.AZURE_CLIENT_SDK_SURVEY_LINK, Constants.AZURE_SDK_SURVEY, Constants.AZURE_ICON) {
        @Override
        public String getLink() {
            return String.format(super.getLink(), getInstallationId());
        }
    },
    AZURE_MGMT_SDK(Constants.AZURE_MGMT_SDK_SURVEY_LINK, Constants.AZURE_SDK_SURVEY, Constants.AZURE_ICON) {
        @Override
        public String getLink() {
            return String.format(super.getLink(), getInstallationId());
        }
    },
    AZURE_SPRING_SDK(Constants.AZURE_SPRING_SDK_SURVEY_LINK, Constants.AZURE_SPRING_SDK_SURVEY_DESCRIPTION, Constants.AZURE_ICON),
    AZURE_INTELLIJ_TOOLKIT(Constants.INTELLIJ_TOOLKIT_SURVEY_LINK, Constants.INTELLIJ_TOOLKIT_SURVEY_DESCRIPTION, Constants.AZURE_ICON) {
        @Override
        public String getLink() {
            final String toolkit = Optional.ofNullable(PluginManagerCore.getPlugin(PluginId.getId("com.microsoft.tooling.msservices.intellij.azure")))
                    .map(PluginDescriptor::getVersion).orElse(StringUtils.EMPTY);
            final ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
            final String ide = String.format("%s %s", applicationInfo.getFullVersion(), applicationInfo.getBuild());
            final String os = System.getProperty("os.name");
            final String jdk = String.format("%s %s", System.getProperty("java.vendor"), System.getProperty("java.version"));
            return String.format(super.getLink(), toolkit, ide, os, jdk, getInstallationId());
        }
    };

    private final String link;
    private final String description;
    private final Icon icon;

    private static String getInstallationId() {
        return Azure.az().config().getMachineId();
    }

    @Override
    public String getType() {
        return this.name();
    }

    private static class Constants {
        private static final Icon AZURE_ICON = IconLoader.getIcon("/icons/Common/AzureCustomerSurvey.svg", CustomerSurvey.class);
        // Survey for azure client&mgmt SDK users
        private static final String AZURE_SDK_SURVEY = "Enjoy Azure SDKs?";
        private static final String AZURE_SDK_SURVEY_LINK = "https://www.surveymonkey.com/r/2D9YTRQ?src=intellij&ver=%s";
        // Survey for azure client SDK users
        private static final String AZURE_CLIENT_SDK_SURVEY_LINK = "https://www.surveymonkey.com/r/8P768ZY?src=intellij&ver=%s";
        // Survey for azure mgmt SDK users
        private static final String AZURE_MGMT_SDK_SURVEY_LINK = "https://www.surveymonkey.com/r/8HXDGTG?src=intellij&ver=%s";
        // Survey for azure spring SDK users
        private static final String AZURE_SPRING_SDK_SURVEY_LINK = "https://forms.office.com/r/f0RbyN1idu ";
        private static final String AZURE_SPRING_SDK_SURVEY_DESCRIPTION = "Enjoy Azure Spring Starters?";
        // Survey for azure toolkit for intellij users
        private static final String INTELLIJ_TOOLKIT_SURVEY_LINK = "https://microsoft.qualtrics.com/jfe/form/SV_b17fG5QQlMhs2up?" +
                "toolkit=%s&ide=%s&os=%s&jdk=%s&id=%s";
        private static final String INTELLIJ_TOOLKIT_SURVEY_DESCRIPTION = "Enjoy Azure Toolkits?";
    }
}
