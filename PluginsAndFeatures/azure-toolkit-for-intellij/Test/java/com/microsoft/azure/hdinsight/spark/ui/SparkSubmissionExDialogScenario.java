package com.microsoft.azure.hdinsight.spark.ui;

import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.impl.artifacts.JarArtifactType;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.microsoft.azure.hdinsight.common.CallBack;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.JobStatusManager;
import com.microsoft.azure.hdinsight.common.SparkSubmissionToolWindowProcessor;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetailMock;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmissionParameter;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.intellij.ToolWindowKey;
import com.microsoft.intellij.common.CommonConst;
import com.microsoft.intellij.util.PluginUtil;
import cucumber.api.DataTable;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.matcher.JButtonMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.JButtonFixture;
import org.assertj.swing.timing.Condition;
import org.assertj.swing.timing.Pause;
import org.assertj.swing.timing.Timeout;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.swing.edt.GuiActionRunner.execute;
import static org.mockito.Mockito.*;



public class SparkSubmissionExDialogScenario extends LightPlatformCodeInsightFixtureTestCase {
    SparkSubmissionExDialog sparkSubmissionDialog;
    List<IClusterDetail> clusterDetailsMock;
    CallBack callback = mock(CallBack.class);
    SparkSubmissionToolWindowProcessor sparkSubmissionToolWindowProcessor = mock(SparkSubmissionToolWindowProcessor.class);
    JobStatusManager jobStatusManager = mock(JobStatusManager.class);
    Project project;
    ArtifactManager artifactManager = mock(ArtifactManager.class);
    Artifact sparkJobArtifact = mock(Artifact.class);
    Artifact[] artifacts = new Artifact[] { sparkJobArtifact };
    ClusterManagerEx clusterManagerExMock = mock(ClusterManagerEx.class);
    SparkSubmitModel submitModelMock;

    DialogFixture dialogFixture;

    SparkSubmissionParameter sparkSubmissionParameterCaptured;

    public TemporaryFolder setUpTempHomeFolder() throws Exception {
        TemporaryFolder tempFolder = new TemporaryFolder();
        tempFolder.create();
        System.setProperty("idea.home.path", tempFolder.getRoot().getPath());

        return tempFolder;
    }

    @Before
    public void setUp(Scenario scenario) throws Exception {
        super.setUp();

        project = this.myFixture.getProject();

        submitModelMock = mock(SparkSubmitModel.class,
                               withSettings().useConstructor(project).defaultAnswer(CALLS_REAL_METHODS));

        // Register Spark Submission tool window
        PluginUtil.registerToolWindowManager(
                new ToolWindowKey(project, CommonConst.SPARK_SUBMISSION_WINDOW_ID),
                sparkSubmissionToolWindowProcessor);
        ToolWindowManager.getInstance(project).registerToolWindow(
                CommonConst.SPARK_SUBMISSION_WINDOW_ID,
                true,
                ToolWindowAnchor.BOTTOM);

        when(sparkSubmissionToolWindowProcessor.getJobStatusManager()).thenReturn(jobStatusManager);

        PackagingElementFactory factory = PackagingElementFactory.getInstance();
        CompositePackagingElement root = factory.createArchive("default_artifact.jar");

        ArtifactManager.getInstance(project).addArtifact("mockedArtifact.jar",  new JarArtifactType(), root);

        when(sparkJobArtifact.getOutputPath()).thenReturn("mockedOutputPath/a/b/c");
        when(sparkJobArtifact.getName()).thenReturn("mockedArtifact.jar");
    }

    @Given("^open spark submission dialog$")
    public void openSparkSubmissionDialog() throws Throwable {
        sparkSubmissionDialog = new SparkSubmissionExDialog(submitModelMock, clusterManagerExMock, callback);

        SparkSubmissionContentPanel panel = (SparkSubmissionContentPanel)(sparkSubmissionDialog.getContentPane());
        panel.getSubmitModel().setClusterComboBoxModel(clusterDetailsMock);

        dialogFixture = new DialogFixture(sparkSubmissionDialog);
        dialogFixture.show();
    }

    @Given("^set cluster detail to the following items$")
    public void setClusterDetailsMock(DataTable clusterDetailsConfig) throws Throwable {
        clusterDetailsMock = IClusterDetailMock.listFactoryFromTable(clusterDetailsConfig);

        when(clusterManagerExMock.getClusterDetails(any())).thenReturn(clusterDetailsMock);
    }

    @Given("^selected artifact ComboBox with tooltip text '(.+)' to value '(.+)'$")
    public void selectComboBoxByToolTip(String toolTipText, String value) throws Throwable {
        ComboBox selectedArtifact = dialogFixture.comboBox(new GenericTypeMatcher<JComboBox>(JComboBox.class) {
            @Override
            protected boolean isMatching(JComboBox jComboBox) {
                return jComboBox.getToolTipText().equals(toolTipText);
            }
        }).targetCastedTo(ComboBox.class);

        for(int i = 0; i < selectedArtifact.getItemCount(); i++ ) {
            Object item = selectedArtifact.getItemAt(i);
            if (item.equals(value)) {
                selectedArtifact.setSelectedIndex(i);
                break;
            }
        }
    }

    @And("^set text field panel with tool tip text '(.+)' to value '(.+)'$")
    public void setTextFieldPanelByToolTip(String toolTipText, String value) throws Throwable {
        TextFieldWithBrowseButton mainClassTextField = dialogFixture.panel(new GenericTypeMatcher<TextFieldWithBrowseButton>(TextFieldWithBrowseButton.class) {
            @Override
            protected boolean isMatching(TextFieldWithBrowseButton textFilePanel) {
                return textFilePanel.getToolTipText().equals(toolTipText);
            }
        }).targetCastedTo(TextFieldWithBrowseButton.class);

        mainClassTextField.setText(value);
    }

    @Then("^the button '(.+)' is (.+)$")
    public void checkIsButtonEnabled(String buttonText, String status) throws Throwable {
        // Spark submission dialog's button should match 'text'
        JButtonFixture button = dialogFixture.button(JButtonMatcher.withText(buttonText));
        assertTrue(status.equals("enabled") == button.isEnabled());
    }

    @Then("^click the '(.+)' button$")
    public void clickButton(String buttonText) throws Throwable {
        // Spark submission dialog's button should match 'text'
        JButtonFixture button = dialogFixture.button(JButtonMatcher.withText(buttonText));

        doNothing().when(submitModelMock).action(any());


        GuiActionRunner.execute(() -> {
            button.target().doClick();

        });

        ArgumentCaptor<SparkSubmissionParameter> parameterArgumentCaptor = ArgumentCaptor.forClass(SparkSubmissionParameter.class);
        verify(submitModelMock).action(parameterArgumentCaptor.capture());

        sparkSubmissionParameterCaptured = parameterArgumentCaptor.getValue();
    }


    @And("^wait for the button '(.+)' is enabled$")
    public void waitForButtonEnabled(String buttonText) throws Throwable {
        // Spark submission dialog's button should match 'text'
        JButtonFixture button = dialogFixture.button(JButtonMatcher.withText(buttonText));

        Pause.pause(new Condition(buttonText + " button to be enabled") {

            public boolean test() {
                return execute(button::isEnabled);
            }

        }, Timeout.timeout(60000));
    }

    @Then("^submitted parameter should be$")
    public void checkSubmittedParameter(Map<String, String> parameterExpect) throws Throwable {
        assertEquals(parameterExpect.get("clusterName"), sparkSubmissionParameterCaptured.getClusterName());
        assertEquals(parameterExpect.get("mainClass"), sparkSubmissionParameterCaptured.getMainClassName());
        assertThat(sparkSubmissionParameterCaptured.getJobConfig())
            .containsAllEntriesOf((new Gson().fromJson(parameterExpect.get("jobConfig"), Map.class)));
    }
}