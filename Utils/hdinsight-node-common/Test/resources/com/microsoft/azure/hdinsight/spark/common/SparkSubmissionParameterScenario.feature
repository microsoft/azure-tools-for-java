Feature: SparkSubmissionParameter unit test

  Scenario: Private function getSparkSubmissionParameterMap
    Given create SparkSubmissionParameter with the following job config
      | driverMemory | 2G |
    And apply spark configs
      | spark.driver.extraJavaOptions | -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006 |
    Then the parameter map should include key 'conf' with value '{spark.driver.extraJavaOptions=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006}'

  Scenario: serializeToJson would serialize the conf map well
    Given mock args to farg1,farg2
    And mock file to fFilePath
    And mock reference files to ffile1,ffile2
    And mock reference jars to fjar1,fjar2
    And mock className to fakeClassName
    And create SparkSubmissionParameter with the following job config
      | driverMemory | 2G |
    Then the serialized JSON should be '{"args":["farg1","farg2"],"file":"fFilePath","driverMemory":"2G","files":["ffile1","ffile2"],"jars":["fjar1","fjar2"],"className":"fakeClassName"}'

    Given create SparkSubmissionParameter with the following job config
      | driverMemory | 2G |
    And apply spark configs
      | spark.driver.extraJavaOptions | -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006 |
      | other                         | Other values                                                       |
    Then the serialized JSON should be '{"args":["farg1","farg2"],"file":"fFilePath","driverMemory":"2G","files":["ffile1","ffile2"],"jars":["fjar1","fjar2"],"className":"fakeClassName","conf":{"other":"Other values","spark.driver.extraJavaOptions":"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6006"}}'

    Given create SparkSubmissionParameter with the following job config
      | driverMemory | 2G |
    And apply spark configs
      | invalid | half close" |
    Then the serialized JSON should be '{"args":["farg1","farg2"],"file":"fFilePath","files":["ffile1","ffile2"],"driverMemory":"2G","jars":["fjar1","fjar2"],"className":"fakeClassName","conf":{"invalid":"half close\""}}'