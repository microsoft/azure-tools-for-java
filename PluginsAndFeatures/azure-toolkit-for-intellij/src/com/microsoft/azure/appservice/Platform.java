package com.microsoft.azure.appservice;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.management.appservice.OperatingSystem;
import lombok.Getter;

import java.util.List;

public interface Platform {

    OperatingSystem getOs();

    String getStackOrWebContainer();

    String getStackVersionOrJavaVersion();

    List<Platform> platforms = ImmutableList.copyOf(new Platform[]{
            Linux.JAVA8_JBOSS72,
            Linux.JAVA8_TOMCAT9,
            Linux.JAVA8_TOMCAT85,
            Linux.JAVA11_TOMCAT9,
            Linux.JAVA11_TOMCAT85,
            Windows.JAVA8_JBOSS72,
            Windows.JAVA8_TOMCAT9,
            Windows.JAVA8_TOMCAT85,
            Windows.JAVA11_TOMCAT9,
            Windows.JAVA11_TOMCAT85
    });

    /**
     * refer com.microsoft.azure.management.appservice.RuntimeStack
     */
    @Getter
    enum Linux implements Platform {
        JAVA8_JBOSS72("JBOSS", "7.2-jre8"),
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
            final String containerVersion = this.version.split("-")[0];
            final String javaVersion = this.version.endsWith("11") ? "11" : "8";
            return String.format("Linux-Java %s-%s %s", javaVersion, this.stack.toUpperCase(), containerVersion);
        }
    }

    /**
     * refer com.microsoft.azure.management.appservice.JavaVersion
     * refer com.microsoft.azuretools.utils.WebAppUtils.WebContainerMod
     */
    @Getter
    enum Windows implements Platform {
        JAVA8_JBOSS72("jboss 7.2", "1.8"),
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
            final String javaVersion = "11".equals(this.javaVersion) ? "11" : "8";
            return String.format("Windows-Java %s-%s", javaVersion, this.webContainer.toUpperCase());
        }
    }
}

