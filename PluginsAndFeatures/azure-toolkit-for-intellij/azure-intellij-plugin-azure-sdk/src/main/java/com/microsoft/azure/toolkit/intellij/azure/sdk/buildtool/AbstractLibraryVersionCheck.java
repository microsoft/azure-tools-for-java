package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.StdLanguages;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.IOException;

/**
 * Abstract class for the library version check inspection.
 * The UpgradeLibraryVersionCheck and IncompatibleDependencyCheck classes extend this class.
 * <p>
 * The UpgradeLibraryVersionCheck class checks the version of the libraries in the pom.xml file against the recommended version.
 * The IncompatibleDependencyCheck class checks the version of the libraries in the pom.xml file against compatible versions.
 */
public abstract class AbstractLibraryVersionCheck extends LocalInspectionTool {

    /**
     * Abstract method to build the specific visitor for the inspection.
     *
     * @param holder     The holder for the problems found
     * @param isOnTheFly boolean to check if the inspection is on the fly - not used in this implementation but is part of the method signature
     * @return The visitor for the inspection
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return createVisitor(holder, isOnTheFly);
    }

    /**
     * Creates the specific visitor for the inspection.
     *
     * @param holder     The holder for the problems found
     * @param isOnTheFly boolean to check if the inspection is on the fly - not used in this implementation but is part of the method signature
     * @return The specific visitor for the inspection
     */
    protected abstract PsiElementVisitor createVisitor(ProblemsHolder holder, boolean isOnTheFly);

    /**
     * Abstract class for the library version visitor.
     * The UpgradeLibraryVersionVisitor and IncompatibleDependencyVisitor classes extend this class.
     * <p>
     * The UpgradeLibraryVersionVisitor class checks the version of the libraries in the pom.xml file against the recommended version.
     * The IncompatibleDependencyVisitor class checks the version of the libraries in the pom.xml file against compatible versions.
     */
    static abstract class LibraryVersionVisitorBase extends PsiElementVisitor {

        // The holder for the problems found
        protected final ProblemsHolder holder;

        /**
         * Constructs a new instance of the visitor.
         *
         * @param holder     The holder for the problems found
         * @param isOnTheFly boolean to check if the inspection is on the fly
         */
        protected LibraryVersionVisitorBase(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * Method to check the pom.xml file for the libraries and their versions.
         *
         * @param file   The pom.xml file to check for the libraries and their versions
         * @param holder The holder for the problems found in the file
         * @throws IOException If an error occurs while reading the file
         */
        protected void checkPomXml(XmlFile file, ProblemsHolder holder) throws IOException {

            // Get the MavenProjectsManager for the file
            MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(file.getProject());
            if (!mavenProjectsManager.isMavenizedProject()) {
                return;
            }

            // Get the root tag of the file
            FileViewProvider viewProvider = file.getViewProvider();
            XmlFile xmlFile = (XmlFile) viewProvider.getPsi(StdLanguages.XML);
            XmlTag rootTag = xmlFile.getRootTag();

            // Check the dependencies in the file and get the groupId, artifactId, and version
            if (rootTag != null && "project".equals(rootTag.getName())) {
                XmlTag[] dependenciesTags = rootTag.findSubTags("dependencies");
                for (XmlTag dependenciesTag : dependenciesTags) {
                    XmlTag[] dependencyTags = dependenciesTag.findSubTags("dependency");
                    for (XmlTag dependencyTag : dependencyTags) {
                        XmlTag groupIdTag = dependencyTag.findFirstSubTag("groupId");
                        XmlTag artifactIdTag = dependencyTag.findFirstSubTag("artifactId");
                        XmlTag versionTag = dependencyTag.findFirstSubTag("version");

                        if (groupIdTag != null && artifactIdTag != null && versionTag != null) {
                            String groupId = groupIdTag.getValue().getText();
                            String artifactId = artifactIdTag.getValue().getText();
                            String currentVersion = versionTag.getValue().getText();

                            // Get the full name of the library
                            String fullName = groupId + ":" + artifactId;

                            // Determine if the version should be flagged
                            checkAndFlagVersion(fullName, currentVersion, holder, versionTag);
                        }
                    }
                }
            }
        }

        /**
         * Method to get the formatted message for the anti-pattern.
         *
         * @param fullName           The full name of the library eg "com.azure:azure-core"
         * @param recommendedVersion The recommended version of the library eg "1.0"
         * @param RULE_CONFIG        The rule configuration object
         * @return The formatted message for the anti-pattern with the full name and recommended version
         */
        protected static String getFormattedMessage(String fullName, String recommendedVersion, RuleConfig RULE_CONFIG) {
            return RULE_CONFIG.getAntiPatternMessage().replace("{{fullName}}", fullName).replace("{{recommendedVersion}}", recommendedVersion);
        }

        /**
         * Abstract method to check the version of the library and flag it if necessary.
         *
         * @param fullName       The full name of the library eg "com.azure:azure-core"
         * @param currentVersion The current version of the library eg "1.0"
         * @param holder         The holder for the problems found
         * @param versionElement The element for the version of the library
         */
        protected abstract void checkAndFlagVersion(String fullName, String currentVersion, ProblemsHolder holder, PsiElement versionElement) throws IOException;
    }
}
