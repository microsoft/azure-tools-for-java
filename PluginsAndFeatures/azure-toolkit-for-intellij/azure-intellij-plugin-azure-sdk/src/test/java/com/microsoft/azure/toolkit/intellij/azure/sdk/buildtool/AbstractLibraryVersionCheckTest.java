package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.StdLanguages;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.UpgradeLibraryVersionCheck.UpgradeLibraryVersionVisitor;
import com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool.IncompatibleDependencyCheck.IncompatibleDependencyVisitor;

import java.util.Arrays;
import java.util.HashSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Abstract class for the library version check inspection.
 * The UpgradeLibraryVersionCheck and IncompatibleDependencyCheck classes extend this class.
 * <p>
 * The UpgradeLibraryVersionCheck class checks the version of the libraries in the pom.xml file against the recommended version.
 * The IncompatibleDependencyCheck class checks the version of the libraries in the pom.xml file against compatible versions.
 */
class AbstractLibraryVersionCheckTest {

    @Mock
    private ProblemsHolder mockHolder;

    @Mock
    private UpgradeLibraryVersionVisitor mockUpgradeLibraryVersionVisitor;

    @Mock
    private IncompatibleDependencyVisitor mockIncompatibleDependencyVisitor;

    @Mock
    private XmlFile mockFile;

    @BeforeEach
    public void setup() {
        mockHolder = mock(ProblemsHolder.class);
        mockFile = mock(XmlFile.class);

        // Set the version groups that are "discovered" by the visitor to test the IncompatibleDependencyVisitor
        IncompatibleDependencyCheck.encounteredVersionGroups = new HashSet<>(Arrays.asList("jackson_2.10", "gson_2.10"));

        UpgradeLibraryVersionCheck mockCheck = new UpgradeLibraryVersionCheck();
        mockUpgradeLibraryVersionVisitor = mockCheck.new UpgradeLibraryVersionVisitor(mockHolder, true);

        IncompatibleDependencyCheck mockIncompatibleDependencyCheck = new IncompatibleDependencyCheck();
        mockIncompatibleDependencyVisitor = mockIncompatibleDependencyCheck.new IncompatibleDependencyVisitor(mockHolder, true);
    }

    /**
     * Test the UpgradeLibraryVersionVisitor with a library that is out of date.
     * The test checks that the visitor registers a problem when the version of the library is out of date.
     */
    @Test
    public void testUpgradeCheckOutOfDate() {
        String groupIDValue = "com.azure";
        String artifactIDValue = "azure-messaging-servicebus";
        String versionValue = "7.0.0";
        int numOfInvocations = 1;
        String recommendedVersion = "7.17";

        verifyRegisterProblem(mockUpgradeLibraryVersionVisitor, groupIDValue, artifactIDValue, versionValue, numOfInvocations, recommendedVersion);
    }

    /**
     * Test the UpgradeLibraryVersionVisitor with a library that is up to date.
     * The test checks that the visitor does not register a problem when the version of the library is up to date.
     */
    @Test
    public void testUpgradeCheckUpToDate() {
        String groupIDValue = "com.azure";
        String artifactIDValue = "azure-messaging-servicebus";
        String versionValue = "7.17.1";
        int numOfInvocations = 0;
        String recommendedVersion = "7.17";

        verifyRegisterProblem(mockUpgradeLibraryVersionVisitor, groupIDValue, artifactIDValue, versionValue, numOfInvocations, recommendedVersion);
    }

    /**
     * Test the UpgradeLibraryVersionVisitor with a library that has a missing version.
     * The test checks that the visitor does not register a problem when the version of the library is missing.
     * A missing version is considered out of scope for this inspection.
     */
    @Test
    public void testUpgradeCheckMissingVersion() {
        String groupIDValue = "com.example";
        String artifactIDValue = "example-lib";
        String versionValue = "";  // Empty version
        int numOfInvocations = 0;  // No problems will be registered by this checker because its out of scope
        String recommendedVersion = null;

        verifyRegisterProblem(mockUpgradeLibraryVersionVisitor, groupIDValue, artifactIDValue, versionValue, numOfInvocations, recommendedVersion);
    }

    /**
     * Test the IncompatibleDependencyVisitor with a library that has a different minor version.
     * The test checks that the visitor registers a problem when the version of the library is different from the recommended version.
     */
    @Test
    public void testIncompatibleDifferentMinorGroup() {
        String groupIDValue = "com.google.code.gson";
        String artifactIDValue = "gson";
        String versionValue = "2.9.0";
        int numOfInvocations = 1;
        String recommendedVersion = "2.10";

        verifyRegisterProblem(mockIncompatibleDependencyVisitor, groupIDValue, artifactIDValue, versionValue, numOfInvocations, recommendedVersion);
    }

    /**
     * Test the IncompatibleDependencyVisitor with a library that has the same minor version.
     * The test checks that the visitor does not register a problem when the version of the library is the same as the recommended version.
     */
    @Test
    public void testIncompatibleCheckSameMinorGroup() {
        String groupIDValue = "com.fasterxml.jackson.core";
        String artifactIDValue = "jackson-databind";
        String versionValue = "2.10.0";
        int numOfInvocations = 0;
        String recommendedVersion = "2.10";

        verifyRegisterProblem(mockIncompatibleDependencyVisitor, groupIDValue, artifactIDValue, versionValue, numOfInvocations, recommendedVersion);
    }

    /**
     * Test the IncompatibleDependencyVisitor with a library that is not in the database.
     * The test checks that the visitor does not register a problem when the library is not in the database.
     * This is because the visitor only checks for libraries that are in the database.
     */
    @Test
    public void testIncompatibleCheckDifferentMajorVersion() {
        String groupIDValue = "com.fasterxml.jackson.core";
        String artifactIDValue = "jackson-databind";
        String versionValue = "3.0.0";  // Different major version
        int numOfInvocations = 0;
        String recommendedVersion = null;

        verifyRegisterProblem(mockIncompatibleDependencyVisitor, groupIDValue, artifactIDValue, versionValue, numOfInvocations, recommendedVersion);
    }

    /**
     * Helper method to verify the registration of a problem by the visitor.
     *
     * @param visitor            The visitor to check the pom.xml file
     * @param groupIDValue       The group ID of the library
     * @param artifactIDValue    The artifact ID of the library
     * @param versionIDValue     The version of the library
     * @param numOfInvocations   The number of times the visitor should register a problem
     * @param recommendedVersion The recommended version of the library
     */
    private void verifyRegisterProblem(PsiElementVisitor visitor, String groupIDValue, String artifactIDValue, String versionIDValue, int numOfInvocations, String recommendedVersion) {

        Project project = mock(Project.class);
        MavenProjectsManager mavenProjectsManager = mock(MavenProjectsManager.class);
        FileViewProvider viewProvider = mock(FileViewProvider.class);
        XmlTag rootTag = mock(XmlTag.class);

        XmlTag dependenciesTag = mock(XmlTag.class);
        XmlTag[] dependenciesTags = new XmlTag[]{dependenciesTag};

        XmlTag dependencyTag = mock(XmlTag.class);
        XmlTag[] dependencyTags = new XmlTag[]{dependencyTag};

        XmlTag groupIdTag = mock(XmlTag.class);
        XmlTagValue groupIdValue = mock(XmlTagValue.class);
        XmlTag artifactIdTag = mock(XmlTag.class);
        XmlTagValue artifactIdValue = mock(XmlTagValue.class);
        XmlTag versionTag = mock(XmlTag.class);
        XmlTagValue versionValue = mock(XmlTagValue.class);

        when(mockFile.getName()).thenReturn("pom.xml");
        when(mockFile.getProject()).thenReturn(project);
        when(MavenProjectsManager.getInstance(project)).thenReturn(mavenProjectsManager);
        when(mavenProjectsManager.isMavenizedProject()).thenReturn(true);

        when(mockFile.getViewProvider()).thenReturn(viewProvider);
        when(viewProvider.getPsi(StdLanguages.XML)).thenReturn(mockFile);
        when(mockFile.getRootTag()).thenReturn(rootTag);
        when(rootTag.getName()).thenReturn("project");

        when(rootTag.findSubTags("dependencies")).thenReturn(dependenciesTags);
        when(dependenciesTag.findSubTags("dependency")).thenReturn(dependencyTags);

        when(dependencyTag.findFirstSubTag("groupId")).thenReturn(groupIdTag);
        when(dependencyTag.findFirstSubTag("artifactId")).thenReturn(artifactIdTag);
        when(dependencyTag.findFirstSubTag("version")).thenReturn(versionTag);

        when(groupIdTag.getValue()).thenReturn(groupIdValue);
        when(artifactIdTag.getValue()).thenReturn(artifactIdValue);
        when(versionTag.getValue()).thenReturn(versionValue);

        when(groupIdValue.getText()).thenReturn(groupIDValue);
        when(artifactIdValue.getText()).thenReturn(artifactIDValue);
        when(versionValue.getText()).thenReturn(versionIDValue);

        visitor.visitFile(mockFile);

        if (visitor instanceof UpgradeLibraryVersionVisitor) {
            verify(mockHolder, times(numOfInvocations)).registerProblem(versionTag, "A newer stable minor version of " + groupIDValue + ":" + artifactIDValue + " is available. We recommend you update to version " + recommendedVersion + ".x");
        } else if (visitor instanceof IncompatibleDependencyVisitor) {
            verify(mockHolder, times(numOfInvocations)).registerProblem(versionTag, "The version of " + groupIDValue + ":" + artifactIDValue + " is not compatible with other dependencies of the same library defined in the pom.xml. Please use versions of the same library release group " + recommendedVersion + ".x to ensure proper functionality.");
        }
    }
}
