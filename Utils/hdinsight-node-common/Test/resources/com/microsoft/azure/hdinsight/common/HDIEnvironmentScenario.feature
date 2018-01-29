Feature: HDIEnvironment test
  Scenario: Environment url test
    Given send environment string 'global'
    Then the portal url 'https://portal.azure.com/', HDInsight url 'https://%s.azurehdinsight.net/', blob full name '%s.blob.core.windows.net'
    Given send environment string 'china'
    Then the portal url 'https://portal.azure.cn/', HDInsight url 'https://%s.azurehdinsight.cn/', blob full name '%s.blob.core.chinacloudapi.cn'
    Given send environment string 'germany'
    Then the portal url 'https://portal.microsoftazure.de/', HDInsight url 'https://%s.azurehdinsight.de/', blob full name '%s.blob.core.cloudapi.de'
    Given send environment string 'us_government'
    Then the portal url 'https://manage.windowsazure.us/', HDInsight url 'https://%s.azurehdinsight.us/', blob full name '%s.blob.core.usgovcloudapi.net'