Feature: HDInsightCommonBundle tests
  Scenario: message() can get the right message string
    Then the following message string should be got from message.properties
      | property | message |
      | HDInsightRequestInstallIdMap  | HDInsight.Request.InstallId |