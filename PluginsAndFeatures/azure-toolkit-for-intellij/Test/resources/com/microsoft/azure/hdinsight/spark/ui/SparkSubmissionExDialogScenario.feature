Feature: Spark Submission Dialog Integration testing

  Scenario: Test the submission parameters with default settings
    Given set cluster detail to the following items
      | <Name>        | <ConnectionUrl> | <HttpUserName> | <HttpPassword> |
      | clusterMock1  |                 | userMock1      | passwordMock1  |
      | clusterMock2  |                 | userMock2      | passwordMock2  |
    And open spark submission dialog
    And selected artifact ComboBox with tooltip text 'The Artifact you want to use.' to value 'mockedArtifact.jar'
    And set text field panel with tool tip text 'Application's java/spark main class' to value 'mocked.main'
    Then the button 'Submit' is enabled
    And click the 'Submit' button
    Then submitted parameter should be
      | <key>        | <value>      |
      | clusterName  | clusterMock1 |
      | mainClass    | mocked.main  |
      | jobConfig    | { "driverCores"="", "driverMemory"="", "executorCores"="", "executorMemory"="", "numExecutors"="" } |