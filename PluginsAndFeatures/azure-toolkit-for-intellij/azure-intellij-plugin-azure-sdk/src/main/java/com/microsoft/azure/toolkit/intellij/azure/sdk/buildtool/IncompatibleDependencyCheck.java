package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Inspection class to check the version of the libraries in the pom.xml file against compatible versions.
 * The compatible versions are fetched from a file hosted on GitHub.
 * The compatible versions are compared against the minor version of the library. Minor version is the first two parts of the version number.
 * If the minor version is different from the compatible version, a warning is flagged and the compatible version is suggested.
 */
public class IncompatibleDependencyCheck extends AbstractLibraryVersionCheck {

    // Set to store the encountered version groups
    static Set<String> encounteredVersionGroups = new HashSet<>();

    /**
     * Abstract method to build the specific visitor for the inspection.
     *
     * @param holder     The holder for the problems found
     * @param isOnTheFly boolean to check if the inspection is on the fly - not used in this implementation but is part of the method signature
     * @return The visitor for the inspection
     */
    @Override
    protected PsiElementVisitor createVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new IncompatibleDependencyVisitor(holder, isOnTheFly);
    }

    /**
     * Method to check the version of the dependency found in the project code against the compatible versions.
     * If the version is not compatible, a warning is flagged and the compatible version is suggested.
     *
     * @param fullName       The full name of the library eg "com.azure:azure-core"
     * @param currentVersion The current version of the library
     * @param holder         The holder for the problems found
     * @param versionElement The version element in the pom.xml file to check
     */
    @Override
    protected void checkAndFlagVersion(String fullName, String currentVersion, ProblemsHolder holder, PsiElement versionElement) {

        // get version group of the dependency found in the project code
        String versionGroup = IncompatibleDependencyVisitor.getGroupVersion(fullName, currentVersion);

        if (versionGroup == null) {
            return;
        }

        // add an encountered version group to the encountered version groups
        if (encounteredVersionGroups.isEmpty()) {
            encounteredVersionGroups.add(versionGroup);
        }

        // check if the encountered version group is not already in the encountered version groups
        for (String encounteredVersionGroup : encounteredVersionGroups) {

            // check if the encountered version group is not the same as the current version group
            // and the encountered version group starts with the version group's substring
            // eg if versionGroup = "jackson_2.10" and encounteredVersionGroup = "jackson_2.10, no problem is flagged
            // if versionGroup = "jackson_2.10" and encounteredVersionGroup = "jackson_2.11", a problem is flagged

            // The substring check is used to determine if versionGroup and encounteredVersionGroup are in the same library
            if (!encounteredVersionGroup.equals(versionGroup) && encounteredVersionGroup.startsWith(versionGroup.substring(0, versionGroup.lastIndexOf("_")))) {
                String recommendedVersion = encounteredVersionGroup.substring(encounteredVersionGroup.lastIndexOf("_") + 1);

                // Flag the version if the minor version is different from the recommended version
                String message = getFormattedMessage(fullName, recommendedVersion, IncompatibleDependencyVisitor.RULE_CONFIG);
                holder.registerProblem(versionElement, message);
            }
        }
    }

    /**
     * Visitor class for the inspection.
     * Checks the version of the libraries in the pom.xml file against compatible versions.
     * The compatible versions are fetched from a file hosted on GitHub.
     * The compatible versions are compared against the minor version of the library. Minor version is the first two parts of the version number.
     * If the minor version is different from the compatible version, a warning is flagged and the compatible version is suggested.
     */
    class IncompatibleDependencyVisitor extends PsiElementVisitor {

        // Holder for the problems found
        private final ProblemsHolder holder;

        // Map to store the compatible versions for each library
        private static WeakReference<Map<String, Set<String>>> FILE_CONTENT_REF;
        private static final Logger LOGGER = Logger.getLogger(IncompatibleDependencyCheck.class.getName());

        /**
         * Constructs a new instance of the IncompatibleDependencyVisitor.
         *
         * @param holder     The holder for the problems found
         * @param isOnTheFly boolean to check if the inspection is on the fly
         */
        IncompatibleDependencyVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;


        static {
            final String ruleName = "IncompatibleDependencyCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG == RuleConfig.EMPTY_RULE;
        }

        /**
         * Method to check the pom.xml file for the library version.
         *
         * @param file The pom.xml file to check
         */
        @Override
        public void visitFile(@NotNull PsiFile file) {
            super.visitFile(file);

            if (SKIP_WHOLE_RULE) {
                return;
            }
            if (!file.getName().equals("pom.xml")) {
                return;
            }
            if (file instanceof XmlFile && file.getName().equals("pom.xml")) {

                // Check the pom.xml file for the library version
                try {
                    IncompatibleDependencyCheck.this.checkPomXml((XmlFile) file, holder);
                } catch (IOException e) {
                    LOGGER.severe("Error checking pom.xml file: " + e);
                }
            }
        }

        /**
         * Method to get the version group for the library.
         * The version group is used to get the compatible versions for the library.
         * The version group is determined by the major and minor version of the library.
         * Eg if the major version is 2 and the minor version is 10, the version group is "jackson_2.10".
         *
         * @param fullName       The full name of the library eg "com.azure:azure-core"
         * @param currentVersion The current version of the library
         * @return The version group for the library
         */
        private static String getGroupVersion(String fullName, String currentVersion) {

            // Split currentVersion to extract major and minor version
            String[] versionParts = currentVersion.split("\\.");
            String majorVersion = versionParts[0];
            String minorVersion = versionParts.length > 1 ? versionParts[1] : "";
            String versionSuffix = "_" + majorVersion + "." + minorVersion;

            // Search the file content for the version group
            String versionGroup = null;

            for (Map.Entry<String, Set<String>> entry : getFileContent().entrySet()) {

                // Check if the set of artifactIds contains the fullName and the corresponding key ends with the versionSuffix
                // This will be the version group of the dependency
                if (entry.getValue().contains(fullName) && entry.getKey().endsWith(versionSuffix)) {
                    versionGroup = entry.getKey();
                    break;
                }
            }
            if (versionGroup != null) {
                return versionGroup;
            } else {
                return null;
            }
        }

        /**
         * Method to get the content of the file hosted on GitHub.
         * The file contains the compatible versions for the libraries.
         * A WeakReference is used to store the content of the file to allow for garbage collection.
         *
         * @return The content of the file as a map
         */
        private static Map<String, Set<String>> getFileContent() {

            // Load the file content from the URL if it is not already loaded

            Map<String, Set<String>> fileContent = FILE_CONTENT_REF == null ? null : FILE_CONTENT_REF.get();

            if (fileContent == null) {
                synchronized (IncompatibleDependencyVisitor.class) {
                    fileContent = FILE_CONTENT_REF == null ? null : FILE_CONTENT_REF.get();
                    if (fileContent == null) {
                        String fileUrl = "https://raw.githubusercontent.com/Azure/azure-sdk-for-java/main/eng/versioning/supported_external_dependency_versions.json";
                        fileContent = GitHubFileFetcher.loadJsonDataFromUrl(fileUrl);
                        FILE_CONTENT_REF = new WeakReference<>(fileContent);
                    }
                }
            }
            return fileContent;
        }
    }
}
