package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Inspection class to check the version of the libraries in the pom.xml file against the recommended version.
 * The recommended version is fetched from a file hosted on GitHub.
 * The recommended version is compared against the minor version of the library. Minor version is the first two parts of the version number.
 * If the minor version is different from the recommended version, a warning is flagged and the recommended version is suggested.
 */
public class UpgradeLibraryVersionCheck extends AbstractLibraryVersionCheck {

    /**
     * Build the specific visitor for the inspection.
     *
     * @param holder     The holder for the problems found
     * @param isOnTheFly boolean to check if the inspection is on the fly - not used in this implementation but is part of the method signature
     * @return The visitor for the inspection
     */

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new UpgradeLibraryVersionVisitor(holder);
    }

    /**
     * Method to check the version of the library against the recommended version.
     *
     * @param fullName       The full name of the library
     * @param currentVersion The current version of the library
     * @param holder         The holder for the problems found
     * @param versionTag     The version tag in the pom.xml file for the library version
     */
    @Override
    protected void checkAndFlagVersion(String fullName, String currentVersion, ProblemsHolder holder, PsiElement versionTag) {

        // Check if the recommended version is available for the library
        if (!(UpgradeLibraryVersionVisitor.getLibraryRecommendedVersionMap().containsKey(fullName))) {
            return;
        }
        String recommendedVersion = UpgradeLibraryVersionVisitor.getLibraryRecommendedVersionMap().get(fullName);

        // Compare minor versions only
        String[] currentVersionParts = currentVersion.split("\\.");

        // Check if the version is in the correct format
        if (!(currentVersionParts.length > 1)) {
            return;
        }

        // Parse to get the minor version
        String currentMinor = currentVersionParts[0] + "." + currentVersionParts[1];

        // Flag the version if the minor version is different from the recommended version
        if (!currentMinor.equals(recommendedVersion)) {
            holder.registerProblem(versionTag, getFormattedMessage(fullName, recommendedVersion, UpgradeLibraryVersionVisitor.RULE_CONFIG));
        }
    }

    /**
     * Visitor class for the inspection.
     * Checks the version of the libraries in the pom.xml file against the recommended version.
     * The recommended version is fetched from a file hosted on GitHub.
     * The recommended version is compared against the minor version of the library. Minor version is the first two parts of the version number.
     * If the minor version is different from the recommended version, a warning is flagged and the recommended version is suggested.
     */
    class UpgradeLibraryVersionVisitor extends PsiElementVisitor {

        private final ProblemsHolder holder;

        /**
         * Constructs a new instance of the visitor.
         *
         * @param holder The holder for the problems found
         */
        UpgradeLibraryVersionVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        // Map to store the recommended version for each library
        private static WeakReference<Map<String, String>> LIBRARY_RECOMMENDED_VERSION_MAP_REF;

        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "UpgradeLibraryVersionCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG == RuleConfig.EMPTY_RULE;
        }

        /**
         * Visitor to check the pom.xml file for the library version.
         *
         * @param file The pom.xml file
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
                try {
                    UpgradeLibraryVersionCheck.this.checkPomXml((XmlFile) file, holder);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         * Method to get the recommended version for each library from the file hosted on GitHub.
         * The file is fetched only once and the content is stored in a WeakReference.
         * A WeakReference is used to allow the garbage collector to collect the content if necessary.
         * If the content is not available in the WeakReference, the file is fetched again.
         *
         * @return The map of the recommended version for each library
         */
        private static Map<String, String> getLibraryRecommendedVersionMap() {

            // Load the file content from the URL if it is not already loaded
            Map<String, String> fileContent = LIBRARY_RECOMMENDED_VERSION_MAP_REF == null ? null : LIBRARY_RECOMMENDED_VERSION_MAP_REF.get();

            if (fileContent == null) {
                synchronized (IncompatibleDependencyCheck.IncompatibleDependencyVisitor.class) {
                    fileContent = LIBRARY_RECOMMENDED_VERSION_MAP_REF == null ? null : LIBRARY_RECOMMENDED_VERSION_MAP_REF.get();
                    if (fileContent == null) {

                        String metadataUrl = RULE_CONFIG.getListedItemsToCheck().get(0);
                        String latestVersion = DependencyVersionFileFetcher.getLatestVersion(metadataUrl);

                        if (latestVersion != null) {
                            String pomUrl = String.format("https://repo1.maven.org/maven2/com/azure/azure-sdk-bom/%s/azure-sdk-bom-%s.pom", latestVersion, latestVersion);
                            fileContent = DependencyVersionFileFetcher.parsePomFile(pomUrl);
                            LIBRARY_RECOMMENDED_VERSION_MAP_REF = new WeakReference<>(fileContent);
                        }
                    }
                }
            }
            return fileContent;
        }
    }
}
