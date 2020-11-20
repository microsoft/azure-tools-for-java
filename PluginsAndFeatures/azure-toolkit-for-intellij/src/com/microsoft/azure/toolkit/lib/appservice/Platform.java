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

package com.microsoft.azure.toolkit.lib.appservice;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public interface Platform {

    OperatingSystem getOs();

    String getStackOrWebContainer();

    String getStackVersionOrJavaVersion();

    static boolean isSupportedArtifactType(@NotNull final String artifactExt, Platform platform) {
        if (Objects.nonNull(platform)) {
            final String container = platform.getStackOrWebContainer().toLowerCase();
            if (container.startsWith("java")) {
                return "jar".equals(artifactExt);
            } else if (container.startsWith("tomcat")) {
                return "war".equals(artifactExt);
            } else if (container.startsWith("jboss")) {
                return "war".equals(artifactExt) || "ear".equals(artifactExt);
            }
        }
        return true;
    }

    /**
     * refer com.microsoft.azure.management.appservice.RuntimeStack
     */
    @Getter
    enum Linux implements Platform {
        JAVA8("JAVA", "8-jre8"),
        JAVA11("JAVA", "11-java11"),
        JAVA8_JBOSS72("JBOSSEAP", "7.2-java8"),
        JAVA8_TOMCAT9("TOMCAT", "9.0-jre8"),
        JAVA8_TOMCAT85("TOMCAT", "8.5-jre8"),
        JAVA11_TOMCAT9("TOMCAT", "9.0-java11"),
        JAVA11_TOMCAT85("TOMCAT", "8.5-java11");

        private final String stack;
        private final String version;

        Linux(final String stack, final String version) {
            this.stack = stack;
            this.version = version;
        }

        @Override
        public OperatingSystem getOs() {
            return OperatingSystem.LINUX;
        }

        @Override
        public String getStackOrWebContainer() {
            return this.stack;
        }

        @Override
        public String getStackVersionOrJavaVersion() {
            return this.version;
        }

        @Override
        public String toString() {
            //TODO: improve implementation
            final String javaVersion = this.version.endsWith("11") ? "11" : "8";
            if ("JAVA".equals(this.getStack())) {
                return String.format("Linux-Java %s (Embedded Web Server)", javaVersion);
            }
            final String containerVersion = this.version.split("-")[0];
            return String.format("Linux-Java %s-%s %s", javaVersion, this.stack.toUpperCase(), containerVersion);
        }
    }

    /**
     * refer com.microsoft.azure.management.appservice.JavaVersion
     * refer com.microsoft.azuretools.utils.WebAppUtils.WebContainerMod
     */
    @Getter
    enum Windows implements Platform {
        JAVA8("java 8", "1.8"),
        JAVA11("java 11", "11"),
        JAVA8_TOMCAT9("tomcat 9.0", "1.8"),
        JAVA8_TOMCAT85("tomcat 8.5", "1.8"),
        JAVA11_TOMCAT9("tomcat 9.0", "11"),
        JAVA11_TOMCAT85("tomcat 8.5", "11");

        private final String webContainer;
        private final String javaVersion;

        Windows(final String webContainer, final String javaVersion) {
            this.webContainer = webContainer;
            this.javaVersion = javaVersion;
        }

        @Override
        public OperatingSystem getOs() {
            return OperatingSystem.WINDOWS;
        }

        @Override
        public String getStackOrWebContainer() {
            return this.webContainer;
        }

        @Override
        public String getStackVersionOrJavaVersion() {
            return this.javaVersion;
        }

        @Override
        public String toString() {
            //TODO: improve implementation
            final String javaVersionString = "11".equals(this.javaVersion) ? "11" : "8";
            if (this.webContainer.startsWith("java")) {
                return String.format("Windows-Java %s (Embedded Web Server)", javaVersionString);
            }
            return String.format("Windows-Java %s-%s", javaVersionString, this.webContainer.toUpperCase());
        }
    }

    @Getter
    enum AzureFunction implements Platform {
        Windows_Java8(OperatingSystem.WINDOWS, JavaVersion.JAVA_8_NEWEST),
        Windows_Java11(OperatingSystem.WINDOWS, JavaVersion.JAVA_11),
        Linux_Java8(OperatingSystem.LINUX, JavaVersion.JAVA_8_NEWEST),
        Linux_Java11(OperatingSystem.LINUX, JavaVersion.JAVA_11);

        private OperatingSystem os;
        private JavaVersion javaVersion;

        AzureFunction(final OperatingSystem os, final JavaVersion javaVersion) {
            this.os = os;
            this.javaVersion = javaVersion;
        }

        @Override
        public OperatingSystem getOs() {
            return this.os;
        }

        @Override
        public String getStackOrWebContainer() {
            return "Java";
        }

        @Override
        public String getStackVersionOrJavaVersion() {
            return javaVersion == JavaVersion.JAVA_8_NEWEST ? "8" : "11";
        }

        @Override
        public String toString() {
            return String.format("%s-Java %s", StringUtils.capitalize(os.name()), javaVersion);
        }
    }
}

