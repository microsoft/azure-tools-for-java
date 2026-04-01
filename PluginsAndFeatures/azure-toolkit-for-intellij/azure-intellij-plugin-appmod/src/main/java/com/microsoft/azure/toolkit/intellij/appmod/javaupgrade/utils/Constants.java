package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils;

public class Constants {
    public static final String UPGRADE_JAVA_AND_FRAMEWORK_PROMPT = "Upgrade java runtime and java framework dependencies of this project to the latest LTS version using java upgrade tools by invoking #generate_upgrade_plan";
    public static final String UPGRADE_JAVA_VERSION_PROMPT = "Upgrade Java runtime from version %s to the latest LTS version using java upgrade tools by invoking #generate_upgrade_plan";
    public static final String UPGRADE_JAVA_FRAMEWORK_PROMPT = "Upgrade %s from version %s to the latest LTS version using java upgrade tools by invoking #generate_upgrade_pla";
    public static final String SCAN_AND_RESOLVE_CVES_PROMPT = "run CVE scan for this project using java upgrade tools by invoking #validate_cves_for_java";
    public static final String UPGRADE_JDK_WITH_COPILOT_DISPLAY_NAME = "Upgrade JDK with Copilot";
    public static final String UPGRADE_SPRING_BOOT_WITH_COPILOT_DISPLAY_NAME = "Upgrade Spring Boot with Copilot";
    public static final String UPGRADE_SPRING_FRAMEWORK_WITH_COPILOT_DISPLAY_NAME = "Upgrade Spring Framework with Copilot";
    public static final String UPGRADE_SPRING_SECURITY_WITH_COPILOT_DISPLAY_NAME = "Upgrade Spring Security with Copilot";
    public static final String SCAN_AND_RESOLVE_CVES_WITH_COPILOT_DISPLAY_NAME = "Scan and Resolve CVEs with Copilot";
    public static final String FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_PROMPT = "Fix the vulnerable dependency %s by using #validate_cves_for_java";
    public static final String FIX_VULNERABLE_DEPENDENCY_WITH_COPILOT_DISPLAY_NAME = "Fix the vulnerable dependency with Copilot";
    public static final String ISSUE_DISPLAY_NAME = "Your project uses %s %s. Consider upgrading to %s to the latest LTS version using java upgrade tools by invoking #generate_upgrade_plan";
}
